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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @author Sergey Simonchik
 */
public class ProgressZipUtil {

    private static final Logger LOG = Logger.getInstance(ProgressZipUtil.class);

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
                                        @Nonnull ZipEntry zipEntry,
                                        @Nonnull File extractToDir,
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
}
