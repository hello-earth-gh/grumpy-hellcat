/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pub.hello.earth.grumpy.hellcat.openpdf;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfAnnotation;
import com.lowagie.text.pdf.PdfAppearance;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 *
 * the example is a modified version from https://github.com/LibrePDF/OpenPDF/blob/master/pdf-toolbox/src/test/java/com/lowagie/examples/fonts/UnicodeExample.java
 */
public class PDFServiceOpenPDFTest {
    
   public PDFServiceOpenPDFTest() {
   }
   
   public static void main(String[] args) {
        System.out.println("True Types (embedded)");
        
        // step 1: creation of a document-object
        Document document = new Document();
        
        try {
            
            // step 2: creation of the writer-object
            PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("c:\\temp\\unicode.pdf"));
            
            // step 3: we open the document
            document.open();
            
            // step 4: we add content to the document
            BaseFont bfComic = BaseFont.createFont("c:\\windows\\fonts\\comic.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            Font font = new Font(bfComic, 12);
            String text1 = "This is the quite popular True Type font 'Comic'.";
            String text2 = "Some greek characters: \u0393\u0394\u03b6" + " ένα κάποιο κείμενο";
            String text3 = "Some cyrillic characters: \u0418\u044f";
            document.add(new Paragraph(text1, font));
            document.add(new Paragraph(text2, font));
            document.add(new Paragraph(text3, font));
            
            // rectangle parameters are lower-left-x, lower-left-y, upper-right-x, upper-right-y
            Rectangle rect = new Rectangle(200f, 450f, 400f, 650f);
            PdfAnnotation annot = PdfAnnotation.createStamp(writer, rect, text2, "asdasd");
            annot.setAppearance(PdfAnnotation.APPEARANCE_NORMAL, createAppearanceStream(writer, text2, rect, bfComic));
            writer.addAnnotation(annot);
        }
        catch(DocumentException | IOException de) {
            System.err.println(de.getMessage());
        }

        // step 5: we close the document
        document.close();       
   }
   
    private static PdfAppearance createAppearanceStream(PdfWriter writer, String text, Rectangle rect, BaseFont font) throws DocumentException, IOException {
        
        //BaseFont font = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        int fontSize = 12;
        
        PdfAppearance appearance = writer.getDirectContent().createAppearance(rect.getWidth(), rect.getHeight());
        appearance.setFontAndSize(font, fontSize);
        appearance.setColorFill(Color.RED);
        appearance.setColorStroke(Color.BLUE);

        appearance.beginText();
        appearance.setLeading(fontSize * 1.2f);
        appearance.moveText(10, rect.getHeight() - 20);
        float width = 0;
        String lineText = "";
        int lineCount = 0;
        for (String word : text.split(" ")) {
            float wordWidth = word.length()*12; // wild guess
            width += wordWidth;
            if (width > rect.getWidth()) {
                // show current line
                showLine(appearance, lineCount, lineText); // almost arbitrary - remember initial position is in lower left corner
                
                // reset current line
                width = wordWidth;
                lineText = word;
                lineCount++;
            } else {
                lineText += (lineText.length() > 0 ? " " : "") + word;
            }
        }
        
        if (lineText.length() > 0) {
            showLine(appearance, lineCount, lineText);            
        }
        appearance.endText();
        
        return appearance;
    }

    private static void showLine(PdfAppearance appearance, int lineCount, String lineText) {
        // show current line
        System.out.println("showing line " + lineCount + ": " + lineText);
        appearance.moveText(10, - lineCount*15); // almost arbitrary - remember initial position is in lower left corner
        appearance.showText(lineText);
    }   
   
}
