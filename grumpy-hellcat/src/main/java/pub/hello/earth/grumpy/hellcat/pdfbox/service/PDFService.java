/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pub.hello.earth.grumpy.hellcat.pdfbox.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDAppearanceContentStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationRubberStamp;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;

public class PDFService {

    private float _fontSize = 13;

    private byte[] modifyPdf(PDDocument pddoc) throws IOException {
        int i = 0;

        List<PDFont> subsetFonts = new ArrayList<>();
        PDPage page = pddoc.getPage(0);
        addAnnotation("some_annot_id", pddoc, page, 100, 100, "test annot", subsetFonts);

        for (PDFont font : subsetFonts) {
            // mega important because when doing showText(), specific characters were marked as those that will be subsetted
            // and during subset() those characters are actually placed / or marked to be placed - one never knows exactly - in the resource stream
            if (font.willBeSubset()) {
                font.subset();
                font.getCOSObject().setNeedToBeUpdated(true); // corresponsing page resources should also be set to be updated - but we do it already in addAnnotation            
            }
        }

        pddoc.getDocumentCatalog().getPages().getCOSObject().setNeedToBeUpdated(true);
        pddoc.getDocumentCatalog().getCOSObject().setNeedToBeUpdated(true);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        pddoc.saveIncremental(baos);

        return baos.toByteArray();
    }

    private PDFont loadFont(PDDocument pddoc, boolean willSubset) throws IOException {
        PDFont ret = null;
        try ( InputStream fs = PDFService.class.getResourceAsStream("/static/fonts/LiberationSans-Bold.ttf")) { //LiberationSans-Bold-Greek3
            ret = PDType0Font.load(pddoc, fs, willSubset);
            // PDFBOX will warn, but we ignore this warning according to https://stackoverflow.com/questions/67137356/pdfbox-embedding-subset-font-for-annotations
        } catch (Exception ex) {
            System.out.println("Could not load font : " + ex.getMessage());
        }
        return ret;
    }

    // this was supposed to conditionally embed the font - find if the font has been already embedded (into this page)
    // it seems that using the SAME font for all annotations and then calling one subset() on that font doesn't do the job right - it works well only for the first annotation
    // so basically each annotation should have a different font, that will be subset separately.... that's the only way i found    
    private PDFont checkFontEmbed(PDPage page, final PDDocument pdfDoc, boolean willSubset, List<PDFont> subsetFontsForThisSession) throws IOException {
        PDFont ret = loadFont(pdfDoc, willSubset);
        ret.getCOSObject().setNeedToBeUpdated(true);
        page.getResources().add(ret);
        page.getResources().getCOSObject().setNeedToBeUpdated(true);
        page.getCOSObject().setNeedToBeUpdated(true);

        return ret;
    }

    // x, y are from the bottom left corner, so if we subtract from them, we go lower and more to the left
    private void addAnnotation(String name, PDDocument pddoc, PDPage page, float x, float y, String text, List<PDFont> subsetFonts) throws IOException {
        List<PDAnnotation> annotations = page.getAnnotations();

        // newlines should not be allowed in text - these are thought to be non-printable charaters, PDF doesn't recognize newlines
        text = text.replaceAll("[\\r\\n]", "");

        // prefer rubber stamp because it does not require RC (rich content) to be defined and Acrobat Reader DC doesn't try to modify the document to create its own RC representation
        PDAnnotationRubberStamp t = new PDAnnotationRubberStamp();
        boolean bDeleteContents = true; // need to set this to true so that rubber stamp appears as "applied"

        t.setAnnotationName(name); // might play important role
        t.setPrinted(true); // always visible
        t.setReadOnly(true); // does not interact with user
        t.setContents(text);
        t.setHidden(false);
        t.setInvisible(false);
        t.setLocked(true);

        t.setPage(page);
        t.setLockedContents(true);

        PDFont font = checkFontEmbed(page, pddoc, true, subsetFonts); //loadFont(pddoc, true);
        if (!subsetFonts.contains(font)) {
            subsetFonts.add(font); // should be subset once all annotations for a page have been added
        }

        PDRectangle rect = new PDRectangle(x, y, 100, 100);
        t.setRectangle(rect);

        Calendar c = Calendar.getInstance();
        t.setCreationDate(c);
        t.setModifiedDate(c);

        PDAppearanceDictionary ap = new PDAppearanceDictionary();

        boolean bDoWrap = true;
        if ("fuewrhsh_annot".equals(name)) {
            bDoWrap = false;
        }

        ap.setNormalAppearance(createAppearanceStream(pddoc, t, font, bDeleteContents, bDoWrap));
        ap.getCOSObject().setNeedToBeUpdated(true);
        t.setAppearance(ap);

        annotations.add(t);
        page.setAnnotations(annotations);

        t.getCOSObject().setNeedToBeUpdated(true);
        page.getResources().getCOSObject().setNeedToBeUpdated(true);
        page.getCOSObject().setNeedToBeUpdated(true);
        pddoc.getDocumentCatalog().getPages().getCOSObject().setNeedToBeUpdated(true);
        pddoc.getDocumentCatalog().getCOSObject().setNeedToBeUpdated(true);
    }

    public byte[] modifyPdf(InputStream is) {
        byte[] ret = null;
        PDDocument pddoc = null;
        try {
            pddoc = PDDocument.load(is); //Loader.loadPDF(is); pdfbox 3.0.0

            ret = modifyPdf(pddoc);

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        } finally {
            if (pddoc != null) {
                try {
                    pddoc.close();
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }

        return ret;
    }

    // there is also PDFUtils.createAppearanceStream but it is used specifically for signatures and the font is embedded fully without subsetting there
    private PDAppearanceStream createAppearanceStream(final PDDocument document, PDAnnotation ann, PDFont font, boolean bDeleteContents, boolean bDoWrap) throws IOException {
        PDAppearanceStream aps = new PDAppearanceStream(document);
        PDAppearanceContentStream apsContent = null;

        PDRectangle rect = ann.getRectangle();
        rect = new PDRectangle(0, 0, rect.getWidth(), rect.getHeight()); // need to be relative - this is mega important because otherwise it appears as if nothing is printed
        aps.setBBox(rect); // set bounding box to the dimensions of the annotation itself

        // in case of multiple lines, we want to start from the highest line
        float initialY = rect.getHeight() - _fontSize;

        // embed our unicode font (NB: yes, this needs to be done otherwise aps.getResources() == null which will cause NPE later during setFont)
        PDResources res = new PDResources();
        aps.setResources(res);

        try {
            // draw directly on the XObject's content stream
            apsContent = new PDAppearanceContentStream(aps);
            apsContent.beginText();
            apsContent.setFont(font, _fontSize); // PDType1Font.HELVETICA_BOLD would work if we didn't have possible greek characters
            apsContent.newLineAtOffset(0, initialY);
            apsContent.showText(ann.getContents());
            apsContent.endText();

            // now that we painted the text in the appearance stream, get rid of the contents of the annotation
            // because some viewers will try to interpret the text in their own way (and subsequently fail to retrieve the font?)
            // actually this is what Acrobat Reader DC user to identify the stamp as "Applied Stamp"
            if (bDeleteContents) {
                ann.setContents(null);
            }
        } finally {
            if (apsContent != null) {
                try {
                    apsContent.close();
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }

        aps.getResources().getCOSObject().setNeedToBeUpdated(true);
        aps.getCOSObject().setNeedToBeUpdated(true);
        return aps;
    }
}
