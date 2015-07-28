package sk.rajniak;

import java.io.File;
import java.io.IOException;

public class ProjectFileProvider {

    public static File getAndroidSourceFile() {
        final String valuesFolderName = "values";

        return getOrCreateAndroidFile(valuesFolderName);
    }

    public static File getOrCreateAndroidTranslationFile(String languageCode) {
        final String valuesFolderName = "values-" + languageCode;
        return getOrCreateAndroidFile(valuesFolderName);
    }

    private static void tryCreateFile(File androidStringFile) {
        try {
            final boolean createNewFileResult = androidStringFile.createNewFile();
            if (!createNewFileResult) {
                System.out.println(
                        "Could not create file: " + androidStringFile.getPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static File getOrCreateAndroidFile(String valuesFolderName) {
        final File currentPathFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
        final File toolsFolder = currentPathFile.getParentFile();
        final File projectFolder = toolsFolder.getParentFile();

        final File androidValuesFolder = getOrCreateValuesFolder(valuesFolderName, projectFolder);

        return getOrCreateAndroidStringFile(androidValuesFolder);
    }

    private static File getOrCreateValuesFolder(String valuesFolderName, File projectFolder) {
        final File androidValuesFolder = new File(projectFolder.getPath()
                + File.separator + "app" + File.separator + "src" + File.separator + "main"
                + File.separator + "res" + File.separator + valuesFolderName);

        if (!androidValuesFolder.exists()) {
            makeValuesFolder(androidValuesFolder);
        }

        return androidValuesFolder;
    }

    private static void makeValuesFolder(File androidValuesFolder) {
        final boolean makeDirectoryResult = androidValuesFolder.mkdirs();
        if (!makeDirectoryResult) {
            System.out.println(
                    "Could not create appropriate directories: " + androidValuesFolder.getPath());
        }
    }

    private static File getOrCreateAndroidStringFile(File androidValuesFolder) {
        final File androidStringFile = new File(androidValuesFolder.getPath() + File.separator + "strings.xml");
        if (!androidStringFile.exists()) {
            tryCreateFile(androidStringFile);
        }
        return androidStringFile;
    }

    public static File getIOsSourceFile() {
        return new File("Localizable.strings");
    }

    public static File getIOsTranslationsFolder() {
        return new File("iOsTranslations");
    }
}
