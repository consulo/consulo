package consulo.util.io.zip;

import consulo.util.io.FileUtil;
import consulo.util.io.StreamUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.*;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @author Sergey Simonchik
 */
public class ZipUtil {
    public interface FileContentProcessor {
        FileContentProcessor STANDARD = FileInputStream::new;

        InputStream getContent(File file) throws IOException;
    }

    public static boolean addFileToZip(
        @Nonnull ZipOutputStream zos,
        @Nonnull File file,
        @Nonnull String relativeName,
        @Nullable Set<String> writtenItemRelativePaths,
        @Nullable FileFilter fileFilter
    ) throws IOException {
        return addFileToZip(zos, file, relativeName, writtenItemRelativePaths, fileFilter, FileContentProcessor.STANDARD);
    }

    /*
     * Adds a new file entry to the ZIP output stream.
     */
    public static boolean addFileToZip(
        @Nonnull ZipOutputStream zos,
        @Nonnull File file,
        @Nonnull String relativeName,
        @Nullable Set<String> writtenItemRelativePaths,
        @Nullable FileFilter fileFilter,
        @Nonnull FileContentProcessor contentProcessor
    ) throws IOException {
        while (relativeName.length() != 0 && relativeName.charAt(0) == '/') {
            relativeName = relativeName.substring(1);
        }

        boolean isDir = file.isDirectory();
        if (isDir && !StringUtil.endsWithChar(relativeName, '/')) {
            relativeName += "/";
        }
        if (fileFilter != null && !FileUtil.isFilePathAcceptable(file, fileFilter)) {
            return false;
        }
        if (writtenItemRelativePaths != null && !writtenItemRelativePaths.add(relativeName)) {
            return false;
        }

        long size = isDir ? 0 : file.length();
        ZipEntry e = new ZipEntry(relativeName);
        e.setTime(file.lastModified());
        if (size == 0) {
            e.setMethod(ZipEntry.STORED);
            e.setSize(0);
            e.setCrc(0);
        }
        zos.putNextEntry(e);
        if (!isDir) {
            InputStream is = null;
            try {
                is = contentProcessor.getContent(file);
                FileUtil.copy(is, zos);
            }
            finally {
                StreamUtil.closeStream(is);
            }
        }
        zos.closeEntry();
        return true;
    }

    public static boolean addFileOrDirRecursively(
        @Nonnull ZipOutputStream zipOutputStream,
        @Nullable File jarFile,
        @Nonnull File file,
        @Nonnull String relativePath,
        @Nullable FileFilter fileFilter,
        @Nullable Set<String> writtenItemRelativePaths
    ) throws IOException {
        if (file.isDirectory()) {
            return addDirToZipRecursively(zipOutputStream, jarFile, file, relativePath, fileFilter, writtenItemRelativePaths);
        }
        addFileToZip(zipOutputStream, file, relativePath, writtenItemRelativePaths, fileFilter);
        return true;
    }

    public static boolean addDirToZipRecursively(
        @Nonnull ZipOutputStream outputStream,
        @Nullable File jarFile,
        @Nonnull File dir,
        @Nonnull String relativePath,
        @Nullable FileFilter fileFilter,
        @Nullable Set<String> writtenItemRelativePaths
    ) throws IOException {
        if (jarFile != null && FileUtil.isAncestor(dir, jarFile, false)) {
            return false;
        }
        if (relativePath.length() != 0) {
            addFileToZip(outputStream, dir, relativePath, writtenItemRelativePaths, fileFilter);
        }
        final File[] children = dir.listFiles();
        if (children != null) {
            for (File child : children) {
                final String childRelativePath = (relativePath.length() == 0 ? "" : relativePath + "/") + child.getName();
                addFileOrDirRecursively(outputStream, jarFile, child, childRelativePath, fileFilter, writtenItemRelativePaths);
            }
        }
        return true;
    }


    public static void extract(@Nonnull File file, @Nonnull File outputDir, @Nullable FilenameFilter filenameFilter) throws IOException {
        extract(file, outputDir, filenameFilter, true);
    }

    public static void extract(@Nonnull File file, @Nonnull File outputDir, @Nullable FilenameFilter filenameFilter, boolean overwrite) throws IOException {
        try (ZipFile zipFile = new ZipFile(file)) {
            extract(zipFile, outputDir, filenameFilter, overwrite);
        }
    }

    public static void extract(final @Nonnull ZipFile zipFile, @Nonnull File outputDir, @Nullable FilenameFilter filenameFilter) throws IOException {
        extract(zipFile, outputDir, filenameFilter, true);
    }

    public static void extract(final @Nonnull ZipFile zipFile, @Nonnull File outputDir, @Nullable FilenameFilter filenameFilter, boolean overwrite) throws IOException {
        final Enumeration entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            final File file = new File(outputDir, entry.getName());
            if (filenameFilter == null || filenameFilter.accept(file.getParentFile(), file.getName())) {
                extractEntry(entry, zipFile.getInputStream(entry), outputDir, overwrite);
            }
        }
    }

    public static void extractEntry(ZipEntry entry, final InputStream inputStream, File outputDir) throws IOException {
        extractEntry(entry, inputStream, outputDir, true);
    }

    public static void extractEntry(ZipEntry entry, final InputStream inputStream, File outputDir, boolean overwrite) throws IOException {
        final boolean isDirectory = entry.isDirectory();
        final String relativeName = entry.getName();
        final File file = new File(outputDir, relativeName);
        file.setLastModified(entry.getTime());
        if (file.exists() && !overwrite) {
            return;
        }

        FileUtil.createParentDirs(file);
        if (isDirectory) {
            file.mkdir();
        }
        else {
            try (BufferedInputStream is = new BufferedInputStream(inputStream); BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(file))) {
                FileUtil.copy(is, os);
            }
        }
    }

    @Nonnull
    public static File compressFile(@Nonnull File srcFile, @Nonnull File zipFile) throws IOException {
        try (InputStream is = new FileInputStream(srcFile)) {
            try (ZipOutputStream os = new ZipOutputStream(new FileOutputStream(zipFile))) {
                os.putNextEntry(new ZipEntry(srcFile.getName()));
                FileUtil.copy(is, os);
                os.closeEntry();
                return zipFile;
            }
        }
    }

    public static boolean isZipContainsEntry(File zip, String relativePath) throws IOException {
        try (ZipFile zipFile = new ZipFile(zip)) {
            Enumeration en = zipFile.entries();

            while (en.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) en.nextElement();
                if (relativePath.equals(zipEntry.getName())) {
                    return true;
                }
            }
            return false;
        }
    }

    /*
     * update an existing jar file. Adds/replace files specified in relpathToFile map
     */
    public static void update(InputStream in, OutputStream out, Map<String, File> relpathToFile) throws IOException {
        ZipInputStream zis = null;
        ZipOutputStream zos = null;
        try {
            zis = new ZipInputStream(in);
            zos = new ZipOutputStream(out);

            // put the old entries first, replace if necessary
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                String name = e.getName();

                if (!relpathToFile.containsKey(name)) { // copy the old stuff
                    // do our own compression
                    ZipEntry e2 = new ZipEntry(name);
                    e2.setMethod(e.getMethod());
                    e2.setTime(e.getTime());
                    e2.setComment(e.getComment());
                    e2.setExtra(e.getExtra());
                    if (e.getMethod() == ZipEntry.STORED) {
                        e2.setSize(e.getSize());
                        e2.setCrc(e.getCrc());
                    }
                    zos.putNextEntry(e2);
                    FileUtil.copy(zis, zos);
                }
                else { // replace with the new files
                    final File file = relpathToFile.get(name);
                    //addFile(file, name, zos);
                    relpathToFile.remove(name);
                    addFileToZip(zos, file, name, null, null);
                }
            }

            // add the remaining new files
            for (final String path : relpathToFile.keySet()) {
                File file = relpathToFile.get(path);
                addFileToZip(zos, file, path, null, null);
            }
        }
        finally {
            StreamUtil.closeStream(zis);
            StreamUtil.closeStream(zos);
        }
    }

    public static boolean isZipContainsFolder(File zip) throws IOException {
        try (ZipFile zipFile = new ZipFile(zip)) {
            Enumeration en = zipFile.entries();

            while (en.hasMoreElements()) {
                ZipEntry zipEntry = (ZipEntry) en.nextElement();

                // we do not necessarily get a separate entry for the subdirectory when the file
                // in the ZIP archive is placed in a subdirectory, so we need to check if the slash
                // is found anywhere in the path
                if (zipEntry.getName().indexOf('/') >= 0) {
                    return true;
                }
            }
            return false;
        }
    }
}
