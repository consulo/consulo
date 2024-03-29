package consulo.ide.util;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.ProgressStreamUtil;
import consulo.http.HttpProxyManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileUtil;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.*;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author Sergey Simonchik
 */
public class DownloadUtil {

  public static final String CONTENT_LENGTH_TEMPLATE = "${content-length}";
  private static final Logger LOG = Logger.getInstance(DownloadUtil.class);

  /**
   * Downloads content of {@code url} to {@code outputFile} atomically.<br/>
   * {@code outputFile} isn't modified if an I/O error occurs or {@code contentChecker} is provided and returns false on the downloaded content.
   * More formally, the steps are:
   * <ol>
   * <li>Download {@code url} to {@code tempFile}. Stop in case of any I/O errors.</li>
   * <li>Stop if {@code contentChecker} is provided, and it returns false on the downloaded content.</li>
   * <li>Move {@code tempFile} to {@code outputFile}. On most OS this operation is done atomically.</li>
   * </ol>
   * <p>
   * Motivation: some web filtering products return pure HTML with HTTP 200 OK status instead of
   * the asked content.
   *
   * @param indicator      progress indicator
   * @param url            url to download
   * @param outputFile     output file
   * @param tempFile       temporary file to download to. This file is deleted on method exit.
   * @param contentChecker checks whether the downloaded content is OK or not
   * @throws IOException if an I/O error occurs
   * @returns true if no {@code contentChecker} is provided or the provided one returned true
   */
  public static boolean downloadAtomically(@Nullable ProgressIndicator indicator, @Nonnull String url, @Nonnull File outputFile, @Nonnull File tempFile, @Nullable Predicate<String> contentChecker)
          throws IOException {
    try {
      downloadContentToFile(indicator, url, tempFile);
      if (contentChecker != null) {
        String content = Files.readString(tempFile.toPath());
        if (!contentChecker.test(content)) {
          return false;
        }
      }
      FileUtil.rename(tempFile, outputFile, FilePermissionCopier.BY_NIO2);
      return true;
    }
    finally {
      FileUtil.delete(tempFile);
    }
  }

  /**
   * Downloads content of {@code url} to {@code outputFile} atomically.
   * {@code outputFile} won't be modified in case of any I/O download errors.
   *
   * @param indicator  progress indicator
   * @param url        url to download
   * @param outputFile output file
   * @param tempFile   temporary file to download to. This file is deleted on method exit.
   */
  public static void downloadAtomically(@Nullable ProgressIndicator indicator, @Nonnull String url, @Nonnull File outputFile, @Nonnull File tempFile) throws IOException {
    downloadAtomically(indicator, url, outputFile, tempFile, null);
  }


  @Nonnull
  public static <V> Outcome<V> provideDataWithProgressSynchronously(@Nullable Project project,
                                                                    @Nonnull String progressTitle,
                                                                    @Nonnull final String actionShortDescription,
                                                                    @Nonnull final Callable<V> supplier,
                                                                    @Nullable Supplier<Boolean> tryAgainProvider) {
    int attemptNumber = 1;
    while (true) {
      final Ref<V> dataRef = Ref.create(null);
      final Ref<Exception> innerExceptionRef = Ref.create(null);
      boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(new Runnable() {
        @Override
        public void run() {
          ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
          indicator.setText(actionShortDescription);
          try {
            V data = supplier.call();
            dataRef.set(data);
          }
          catch (Exception ex) {
            innerExceptionRef.set(ex);
          }
        }
      }, progressTitle, true, project);
      if (!completed) {
        return Outcome.createAsCancelled();
      }
      Exception latestInnerException = innerExceptionRef.get();
      if (latestInnerException == null) {
        return Outcome.createNormal(dataRef.get());
      }
      LOG.warn("[attempt#" + attemptNumber + "] Can not '" + actionShortDescription + "'", latestInnerException);
      boolean onceMore = false;
      if (tryAgainProvider != null) {
        onceMore = Boolean.TRUE.equals(tryAgainProvider.get());
      }
      if (!onceMore) {
        return Outcome.createAsException(latestInnerException);
      }
      attemptNumber++;
    }
  }

  public static void downloadContentToFile(@Nullable ProgressIndicator progress, @Nonnull String url, @Nonnull File outputFile) throws IOException {
    boolean parentDirExists = FileUtil.createParentDirs(outputFile);
    if (!parentDirExists) {
      throw new IOException("Parent dir of '" + outputFile.getAbsolutePath() + "' can not be created!");
    }
    try (OutputStream out = new FileOutputStream(outputFile)) {
      download(progress, url, out);
    }
  }

  private static void download(@Nullable ProgressIndicator progress, @Nonnull String location, @Nonnull OutputStream output) throws IOException {
    String originalText = progress != null ? progress.getText() : null;
    substituteContentLength(progress, originalText, -1);
    if (progress != null) {
      progress.setText2("Downloading " + location);
    }
    HttpURLConnection urlConnection = HttpProxyManager.getInstance().openHttpConnection(location);
    try {
      int timeout = (int)TimeUnit.MINUTES.toMillis(2);
      urlConnection.setConnectTimeout(timeout);
      urlConnection.setReadTimeout(timeout);
      urlConnection.connect();
      InputStream in = urlConnection.getInputStream();
      int contentLength = urlConnection.getContentLength();
      substituteContentLength(progress, originalText, contentLength);
      ProgressStreamUtil.copyStreamContent(progress, in, output, contentLength);
    }
    catch (IOException e) {
      throw new IOException("Can not download '" +
                            location +
                            "', response code: " +
                            urlConnection.getResponseCode() +
                            ", response message: " +
                            urlConnection.getResponseMessage() +
                            ", headers: " +
                            urlConnection.getHeaderFields(), e);
    }
    finally {
      try {
        urlConnection.disconnect();
      }
      catch (Exception e) {
        LOG.warn("Exception at disconnect()", e);
      }
    }
  }

  private static void substituteContentLength(@Nullable ProgressIndicator progress, @Nullable String text, int contentLengthInBytes) {
    if (progress != null && text != null) {
      int ind = text.indexOf(CONTENT_LENGTH_TEMPLATE);
      if (ind != -1) {
        String mes = formatContentLength(contentLengthInBytes);
        String newText = text.substring(0, ind) + mes + text.substring(ind + CONTENT_LENGTH_TEMPLATE.length());
        progress.setText(newText);
      }
    }
  }

  private static String formatContentLength(int contentLengthInBytes) {
    if (contentLengthInBytes < 0) {
      return "";
    }
    final int kilo = 1024;
    if (contentLengthInBytes < kilo) {
      return ", " + contentLengthInBytes + " bytes";
    }
    if (contentLengthInBytes < kilo * kilo) {
      return String.format(Locale.US, ", %.1f kB", contentLengthInBytes / (1.0 * kilo));
    }
    return String.format(Locale.US, ", %.1f MB", contentLengthInBytes / (1.0 * kilo * kilo));
  }

}
