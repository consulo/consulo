package consulo.ide.util;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.io.StreamUtil;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.*;
import java.util.Enumeration;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * @author Sergey Simonchik
 */
@SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
public class ZipUtil {

  private static final Logger LOG = Logger.getInstance(ZipUtil.class);

  public interface ContentProcessor {
    /**
     * Return null to skip the file
     */
    @Nullable
    byte[] processContent(byte[] content, File file) throws IOException;
  }

  public static void unzipWithProgressSynchronously(@Nullable Project project, @Nonnull String progressTitle, @Nonnull final File zipArchive, @Nonnull final File extractToDir)
          throws ZipUnpackException {
    Outcome<Boolean> outcome = DownloadUtil.provideDataWithProgressSynchronously(project, progressTitle, "Unpacking ...", new Callable<Boolean>() {
      @Override
      public Boolean call() throws IOException {
        ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
        ZipInputStream stream = new ZipInputStream(new FileInputStream(zipArchive));
        unzip(progress, extractToDir, stream, null, null);
        return true;
      }
    }, () -> false);

    Boolean result = outcome.get();
    if (result == null) {
      Exception e = outcome.getException();
      if (e != null) {
        throw new ZipUnpackException("Unpacking failed, downloaded archive is broken", e);
      }
      throw new ZipUnpackException("Unpacking was cancelled");
    }
  }

  public static void unzip(@Nullable ProgressIndicator progress,
                           File extractToDir,
                           ZipInputStream stream,
                           @Nullable Function<String, String> pathConvertor,
                           @Nullable ContentProcessor contentProcessor) throws IOException {
    if (progress != null) {
      progress.setText("Extracting...");
    }
    try {
      ZipEntry entry;
      while ((entry = stream.getNextEntry()) != null) {
        unzipEntryToDir(progress, entry, extractToDir, stream, pathConvertor, contentProcessor);
      }
    }
    finally {
      stream.close();
    }
  }

  private static void unzipEntryToDir(@Nullable ProgressIndicator progress,
                                      @Nonnull final ZipEntry zipEntry,
                                      @Nonnull final File extractToDir,
                                      ZipInputStream stream,
                                      @Nullable Function<String, String> pathConvertor,
                                      @Nullable ContentProcessor contentProcessor) throws IOException {

    String relativeExtractPath = createRelativeExtractPath(zipEntry);
    if (pathConvertor != null) {
      relativeExtractPath = pathConvertor.apply(relativeExtractPath);
      if (relativeExtractPath == null) {
        // should be skipped
        return;
      }
    }
    File child = new File(extractToDir, relativeExtractPath);
    File dir = zipEntry.isDirectory() ? child : child.getParentFile();
    if (!dir.exists() && !dir.mkdirs()) {
      throw new IOException("Unable to create dir: '" + dir + "'!");
    }
    if (zipEntry.isDirectory()) {
      return;
    }
    if (progress != null) {
      progress.setText("Extracting " + relativeExtractPath + " ...");
    }
    FileOutputStream fileOutputStream = null;
    try {
      if (contentProcessor == null) {
        fileOutputStream = new FileOutputStream(child);
        FileUtil.copy(stream, fileOutputStream);
      }
      else {
        byte[] content = contentProcessor.processContent(StreamUtil.loadFromStream(stream), child);
        if (content != null) {
          fileOutputStream = new FileOutputStream(child);
          fileOutputStream.write(content);
        }
      }
    }
    finally {
      if (fileOutputStream != null) {
        fileOutputStream.close();
      }
    }
    LOG.info("Extract: " + relativeExtractPath);
  }

  private static String createRelativeExtractPath(ZipEntry zipEntry) {
    String name = zipEntry.getName();
    int ind = name.indexOf('/');
    if (ind >= 0) {
      name = name.substring(ind + 1);
    }
    return StringUtil.trimEnd(name, "/");
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
      ZipEntry entry = (ZipEntry)entries.nextElement();
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
    if (file.exists() && !overwrite) return;

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
}
