package core.file;

import java.io.File;
import java.io.IOException;
/**
 * Utility class for performing file and directory cleanup operations.
 * <p>This class provides static methods to recursively delete all contents
 * (files and subdirectories) within a specified target folder.</p>
 */
public class CoreFileUtil {
    /**
     * Cleans up (deletes all contents from) a specified folder located relative
     * to the user's current working directory (System.getProperty("user.dir")).
     *
     * <p>If the target folder does not exist, the method terminates silently.
     * If the folder exists, it iterates through all files and subdirectories
     * and deletes them recursively. Note that deletion failures of individual
     * files/folders are currently handled silently ({@code file.delete()} returns
     * {@code false} but no exception is thrown).</p>
     *
     * @param folderName The name of the folder (e.g., "target/screenshots")
     * whose contents should be cleaned. This folder must be
     * a direct child or descendant of the current working directory.
     * @throws IOException The method signature includes {@code IOException}, which
     * is kept for consistency but is typically not thrown by the
     * underlying {@code File} operations used here.
     * @see #deleteRecursively(File)
     */
    public static void fileCleaner(String folderName) throws IOException {
        String path= System.getProperty("user.dir") + File.separator+folderName;
        File directory = new File(path);
        if(!directory.exists())
        {
            return;
        }
        File[] files = directory.listFiles();
        if(files != null)
        {
            for(File file : files)
            {
                deleteRecursively(file);
            }
        } // Note: The main folder itself is NOT deleted, only its contents.
    }
    /**
     * Helper method to recursively delete a file or directory and its contents.
     * <p>If the provided {@code file} is a directory, it first recursively
     * deletes all files and subdirectories within it. Once the directory is empty,
     * or if the input is just a file, it attempts to delete the file/directory itself.</p>
     *
     * @param file The file or directory object to be deleted.
     */
    private static void deleteRecursively(File file)
    {
        if(file.isDirectory())
        {
            for(File subFile :file.listFiles())
            {
                deleteRecursively(subFile);
            }
        }
        file.delete();
    }
}
