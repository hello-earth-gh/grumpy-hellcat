/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pub.hello.earth.grumpy.hellcat.pdfbox;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import pub.hello.earth.grumpy.hellcat.pdfbox.service.PDFService;

public class PdfBoxAnnotationsBrokenSignatureScenario {

    public PdfBoxAnnotationsBrokenSignatureScenario() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println("PdfBoxAnnotationsBrokenSignatureScenario");

        byte[] input = null;
        String name = "test_libre_office_signed_certified.pdf";
        try ( InputStream is = PdfBoxAnnotationsBrokenSignatureScenario.class.getResourceAsStream("/pdfbox/" + name)) {
            input = IOUtils.toByteArray(is);
        }

        PDFService pdfService = new PDFService();
        byte[] res = pdfService.modifyPdf(new ByteArrayInputStream(input));

        name = name.toLowerCase().replace(".pdf", "_annotated.pdf");

        try {
            FileUtils.writeByteArrayToFile(new File("c:\\temp\\" + name), res);
        } catch (Exception ex) {
            System.out.println("AA exception - check maybe file is open and cannot be overwritten!");
            throw ex;
        }        
    }

}
