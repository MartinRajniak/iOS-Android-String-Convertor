package sk.rajniak;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class XmlUtils {

    public static Document loadXml(File sourceFile) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document baseSizeDoc = dBuilder.parse(sourceFile);

            baseSizeDoc.getDocumentElement().normalize();

            return baseSizeDoc;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }

        throw new IllegalStateException("Could not load xml file: " + sourceFile.getPath());
    }
}
