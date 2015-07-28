package sk.rajniak;

import org.apache.commons.lang3.StringEscapeUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static final String IOS_STRINGS_FILE_NAME = "Localizable.strings";
    private static Map<String, String> sAndroidBaseStringsMap;

    private static Map<String, String> sIOsBaseStringsMap;

    private static Map<String, String> sAndroidToIOsNamesMap;

    public static void main(String[] args) {
        parseAndroidBaseFile();

        parseIOsBaseFile();

        if (sAndroidBaseStringsMap == null || sIOsBaseStringsMap == null) {
            return;
        }

        createAndroidToIOsMatchMap();

        if (sAndroidBaseStringsMap == null) {
            return;
        }

        createAndroidTranslationsFromIOsSources();
    }

    private static void parseAndroidBaseFile() {
        final File destinationBaseFile = ProjectFileProvider.getAndroidSourceFile();
        try {
            sAndroidBaseStringsMap = loadAndroidStrings(destinationBaseFile);
        } catch (FileNotFoundException e) {
            System.out.println("Could not locate file#" + destinationBaseFile.getPath());
        }
    }

    private static Map<String, String> loadAndroidStrings(File sourceFile) throws FileNotFoundException {
        final Map<String, String> androidStrings = new HashMap<>();
        final Document doc = XmlUtils.loadXml(sourceFile);
        final NodeList dimensNodeList = doc.getElementsByTagName("string");
        for (int pos = 0, size = dimensNodeList.getLength(); pos < size; pos++) {
            final Node dimenNode = dimensNodeList.item(pos);
            if (dimenNode.getNodeType() == Node.ELEMENT_NODE) {
                Element stringElement = (Element) dimenNode;

                final String name = stringElement.getAttribute("name").trim().replace("\"", "");
                final String value = StringEscapeUtils.escapeXml10(stringElement.getTextContent());
                androidStrings.put(name, value);
            }
        }

        return androidStrings;
    }

    private static void parseIOsBaseFile() {
        final File sourceBaseFile = ProjectFileProvider.getIOsSourceFile();
        try {
            sIOsBaseStringsMap = loadIOsStrings(sourceBaseFile);
        } catch (FileNotFoundException e) {
            System.out.println("Could not locate file#" + sourceBaseFile.getPath());
        }
    }

    private static Map<String, String> loadIOsStrings(File sourceFile) throws FileNotFoundException {
        final Map<String, String> iOsStrings = new HashMap<>();
        final Scanner fileScanner = new Scanner(new FileInputStream(sourceFile), "UTF8");
        while (fileScanner.hasNextLine()) {
            String line = fileScanner.nextLine();
            if (line.contains("/*") || line.contains("*/")) {
                continue;
            }
            if (line.contains("=")) {
                line = line.trim();
                line = line.replace("\"", "");
                line = line.replace(";", "");
                final String[] parts = line.split("=");

                final String resultName = parts[0];
                boolean isPlural = resultName.contains("##{");
                final String resultValue = processIOsValue(parts[1], isPlural);
                iOsStrings.put(resultName, resultValue);
            }
        }
        return iOsStrings;
    }

    private static String processIOsValue(String part, boolean isPlural) {
        String value = part.trim();
        StringBuilder resultValue = new StringBuilder();
        if (value.contains("%@") || value.contains("%i")) {
            int formatCnt = 0;
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                char next = value.length() > i + 1 ? value.charAt(i + 1) : '\0';
                if (c == '%' && next == '@') {
                    formatCnt++;
                    resultValue.append('%');
                    resultValue.append(formatCnt);
                    resultValue.append("$s");
                    i++;
                } else if (c == '%' && next == 'i') {
                    formatCnt++;
                    resultValue.append('%');
                    resultValue.append(formatCnt);
                    resultValue.append("$d");
                    i++;
                } else {
                    resultValue.append(value.charAt(i));
                }
            }
        } else {
            // Seems like iOs plurals don't have to have special symbol if number goes as first
            final String pluralCheckedValue = isPlural ? ("%1$d " + value) : value;
            final String apostropheCheckedValue = pluralCheckedValue.replace("\'", "\\\'");
            resultValue.append(apostropheCheckedValue);
        }
        return StringEscapeUtils.escapeXml10(resultValue.toString());
    }

    private static void createAndroidToIOsMatchMap() {
        sAndroidToIOsNamesMap = pairNamesByMatchingValues(sAndroidBaseStringsMap, sIOsBaseStringsMap);
        System.out.println("\n\n\n");
        printMissingIOsNamePairs(sIOsBaseStringsMap, sAndroidToIOsNamesMap);
    }

    private static Map<String, String> pairNamesByMatchingValues(Map<String, String> androidBaseStringsMap, Map<String, String> iOsBaseStringsMap) {
        final Map<String, String> matchingNames = new HashMap<>();
        final Map<String, String> swappedIOSStringMap = MapUtils.swapMap(iOsBaseStringsMap);
        int failedToMatchCount = 0;
        for (Map.Entry<String, String> entry : androidBaseStringsMap.entrySet()) {
            final String iOsName = swappedIOSStringMap.get(entry.getValue());
            if (iOsName != null) {
                matchingNames.put(entry.getKey(), iOsName);
            } else {
                System.out.println(entry.getKey() + "\t" + entry.getValue());
                failedToMatchCount++;
            }
        }
        System.out.println("Failed to match count: " + failedToMatchCount);
        return matchingNames;
    }

    private static void printMissingIOsNamePairs(Map<String, String> iOsBaseStringsMap, Map androidToIOsNames) {
        for (Map.Entry<String, String> entry : iOsBaseStringsMap.entrySet()) {
            if (!androidToIOsNames.containsValue(entry.getKey())) {
                System.out.println(entry.getKey() + "\t" + entry.getValue());
            }
        }
    }

    private static void createAndroidTranslationsFromIOsSources() {
        final File iOsTranslationsFolder = ProjectFileProvider.getIOsTranslationsFolder();
        final File[] files = iOsTranslationsFolder.listFiles();
        if (files == null || files.length == 0) {
            return;
        }

        for (File iOsTranslationFolder : files) {
            createAndroidTranslation(iOsTranslationFolder);
        }
    }

    private static void createAndroidTranslation(File iOsTranslationFolder) {
        final String translationFolderName = iOsTranslationFolder.getName();
        final String languageCode = translationFolderName.substring(0, translationFolderName.indexOf("."));
        final File destinationFile = ProjectFileProvider.getOrCreateAndroidTranslationFile(languageCode);

        loadIOsStringsToAndroidFile(iOsTranslationFolder, destinationFile);
    }

    private static void loadIOsStringsToAndroidFile(File iOsTranslationFolder, File destinationFile) {
        final Map<String, String> iOSTranslatedStringsMap = tryLoadIOsTranslatedStrings(iOsTranslationFolder);
        final FileOutputStream fileOutputStream = tryGetFileOutputStream(destinationFile);

        if (iOSTranslatedStringsMap == null || fileOutputStream == null) {
            return;
        }

        tryParseIOsTranslationsToAndroidFile(iOSTranslatedStringsMap, fileOutputStream);
    }

    private static Map<String, String> tryLoadIOsTranslatedStrings(File iOsTranslationFolder) {
        try {
            return loadIOsStrings(new File(iOsTranslationFolder + File.separator + IOS_STRINGS_FILE_NAME));
        } catch (FileNotFoundException e) {
            System.out.println(
                    "Could not find localization file in iOS translations folder: " + iOsTranslationFolder.getPath());
        }
        return new HashMap<>();
    }

    private static FileOutputStream tryGetFileOutputStream(File destinationFile) {
        try {
            return new FileOutputStream(destinationFile);
        } catch (FileNotFoundException e) {
            System.out.println("Could not find target localized android string file: " + destinationFile.getPath());
        }
        return null;
    }

    private static void tryParseIOsTranslationsToAndroidFile(Map<String, String> iOSTranslatedStringsMap, FileOutputStream fileOutputStream) {
        try {
            parseIOsTranslationsToAndroidFile(iOSTranslatedStringsMap, fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void parseIOsTranslationsToAndroidFile(Map<String, String> iOSTranslatedStringsMap,
                                                          FileOutputStream fileOutputStream) throws IOException {
        final Writer writer = tryGetFileWriter(fileOutputStream);
        if (writer == null) {
            return;
        }

        writer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>");
        writer.append("\n");

        for (Map.Entry<String, String> entry : sAndroidToIOsNamesMap.entrySet()) {
            final String iOsNameKey = entry.getValue();
            final String translatedValue = iOSTranslatedStringsMap.get(iOsNameKey);
            final String androidNameKey = entry.getKey();
            final String androidResourceElement = createAndroidResourceEntry(androidNameKey, translatedValue);
            writer.append(androidResourceElement);
            writer.append("\n");
        }

        writer.append("</resources>");
        writer.close();
    }

    private static String createAndroidResourceEntry(String androidNameKey, String translatedValue) {
        return "    <string name=\"" + androidNameKey + "\">" + translatedValue + "</string>";
    }

    private static Writer tryGetFileWriter(FileOutputStream fileOutputStream) {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(fileOutputStream, "UTF8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return writer;
    }
}
