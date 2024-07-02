package dev.kaly7;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.xpath.*;

public class CAExtractor {

    public static void main(String[] args) throws Exception {
        // Check if the required arguments are provided
        if (args.length < 2) {
            System.out.println("Usage: java CAExtractor <service> <country> [--target_folder <target_folder>]");
            return;
        }

        String service = args[0];
        String country = args[1];
        // Optional target folder argument
        String targetFolder = args.length > 2 && "--target_folder".equals(args[2]) ? args[3] : ".";

        // Determine the service URI based on the provided service type
        String serviceUri = switch (service) {
            case "QWAC" -> "http://uri.etsi.org/TrstSvc/TrustedList/SvcInfoExt/ForWebSiteAuthentication";
            case "QSealC" -> "http://uri.etsi.org/TrstSvc/TrustedList/SvcInfoExt/ForeSeals";
            default -> throw new IllegalArgumentException("Invalid service type. Must be 'QWAC' or 'QSealC'.");
        };

        // Construct the URL for downloading the XML content
        String urlString = "https://eidas.ec.europa.eu/efda/tl-browser/api/v1/browser/download/" + country;
        URI uri = URI.create(urlString);

        // Create HttpClient instance
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder(uri).GET().build();
        
        // Send the request and get the response
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        // Use try-with-resources to ensure InputStream is closed properly
        try (InputStream xmlContent = new BufferedInputStream(response.body())) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(xmlContent));

            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xpath = xPathFactory.newXPath();
            // Set custom NamespaceContext for XPath evaluation
            xpath.setNamespaceContext(new SimpleNamespaceContext(Map.of("tsl", "http://uri.etsi.org/02231/v2#")));

            // Define the XPath expression to find the relevant service elements
            String xpathExpr = "//tsl:TSPService[" +
                    "tsl:ServiceInformation/tsl:ServiceTypeIdentifier='http://uri.etsi.org/TrstSvc/Svctype/CA/QC' and " +
                    "tsl:ServiceInformation/tsl:ServiceStatus='http://uri.etsi.org/TrstSvc/TrustedList/Svcstatus/granted' and " +
                    "tsl:ServiceInformation/tsl:ServiceInformationExtensions/tsl:Extension[" +
                    "tsl:AdditionalServiceInformation/tsl:URI[text()='" + serviceUri + "']]]";

            // Evaluate the XPath expression to get the service elements
            NodeList elements = (NodeList) xpath.evaluate(xpathExpr, doc, XPathConstants.NODESET);

            // If any elements are found, process them
            if (elements.getLength() > 0) {
                // Create target directory if it doesn't exist
                Files.createDirectories(Paths.get(targetFolder));
                for (int i = 0; i < elements.getLength(); i++) {
                    Node element = elements.item(i);
                    // Extract the service name
                    Node nameElem = (Node) xpath.evaluate(".//tsl:ServiceName/tsl:Name", element, XPathConstants.NODE);
                    System.out.println("Extracting: " + nameElem.getTextContent());

                    // Extract the X509Certificate element
                    Node certElem = (Node) xpath.evaluate(".//tsl:X509Certificate", element, XPathConstants.NODE);
                    if (certElem != null) {
                        // Clean and format the certificate string
                        String certStr = certElem.getTextContent().trim().replace(" ", "").replace("\n", "");
                        StringBuilder wrappedCertStr = new StringBuilder("-----BEGIN CERTIFICATE-----\n");
                        for (int j = 0; j < certStr.length(); j += 64) {
                            wrappedCertStr.append(certStr, j, Math.min(j + 64, certStr.length())).append("\n");
                        }
                        wrappedCertStr.append("-----END CERTIFICATE-----\n");
                        // Define the output filename
                        String filename = country + "_" + i + ".pem";
                        Path filepath = Paths.get(targetFolder, filename);
                        // Write the certificate to a file using BufferedWriter
                        try (BufferedWriter writer = Files.newBufferedWriter(filepath)) {
                            writer.write(wrappedCertStr.toString());
                        }
                        System.out.println("Wrote " + filepath.toString());
                    }
                }
            }
        }
    }
}