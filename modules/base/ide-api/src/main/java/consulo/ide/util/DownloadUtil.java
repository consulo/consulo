package consulo.ide.util;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.http.HttpRequests;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileUtil;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author Sergey Simonchik
 */
public class DownloadUtil {
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
     * @return true if no {@code contentChecker} is provided or the provided one returned true
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
            boolean completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
                ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
                indicator.setText(actionShortDescription);
                try {
                    V data = supplier.call();
                    dataRef.set(data);
                }
                catch (Exception ex) {
                    innerExceptionRef.set(ex);
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

        HttpRequests.request(url).saveToFile(outputFile, progress);
    }
}
