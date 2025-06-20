/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.externalService.impl.internal.update;

import consulo.application.internal.ApplicationInfo;
import consulo.application.internal.start.StartupActionScriptManager;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.container.boot.ContainerPathManager;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.container.plugin.PluginManager;
import consulo.externalService.impl.internal.repository.RepositoryHelper;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.externalService.update.UpdateSettings;
import consulo.http.HttpRequests;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.util.io.FilePermissionCopier;
import consulo.util.io.FileUtil;
import consulo.util.io.NioPathUtil;
import consulo.util.io.StreamUtil;
import consulo.util.io.zip.ZipUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.TimeoutUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

/**
 * @author anna
 * @since 2007-08-10
 */
public class PluginDownloader {
    private static final Logger LOG = Logger.getInstance(PluginDownloader.class);

    private static final String CHECKSUM_ALGORITHM = "SHA3-256";
    private static final int MAX_TRYS = 3;

    @Nonnull
    public static PluginDownloader createDownloader(@Nonnull PluginDescriptor descriptor, boolean viaUpdate) {
        return createDownloader(descriptor, null, viaUpdate);
    }

    @Nonnull
    public static PluginDownloader createDownloader(
        @Nonnull PluginDescriptor d,
        @Nullable String platformVersion,
        boolean viaUpdate
    ) {
        return new PluginDownloader(d, (descriptor, tryIndex) -> {
            boolean noRedirect = tryIndex > 2;
            return RepositoryHelper.buildUrlForDownload(
                UpdateSettings.getInstance().getChannel(),
                descriptor.getPluginId().toString(),
                platformVersion,
                false,
                viaUpdate,
                noRedirect
            );
        });
    }

    private final PluginId myPluginId;
    private final PluginDownloadUrlBuilder myPluginDownloadUrlBuilder;

    private File myFile;
    private File myOldFile;

    private final PluginDescriptor myDescriptor;

    private boolean myIsPlatform;

    public PluginDownloader(@Nonnull PluginDescriptor pluginDescriptor, @Nonnull PluginDownloadUrlBuilder pluginDownloadUrlBuilder) {
        myPluginId = pluginDescriptor.getPluginId();
        myDescriptor = pluginDescriptor;
        myPluginDownloadUrlBuilder = pluginDownloadUrlBuilder;
        myIsPlatform = PlatformOrPluginUpdateChecker.getPlatformPluginId() == pluginDescriptor.getPluginId();
    }

    private String buildUrl(int tryIndex) {
        return myPluginDownloadUrlBuilder.buildUrl(myDescriptor, tryIndex);
    }

    public void download(@Nonnull ProgressIndicator pi) throws PluginDownloadFailedException {
        PluginDescriptor descriptor;
        if (!Boolean.getBoolean(StartupActionScriptManager.STARTUP_WIZARD_MODE) && PluginManager.findPlugin(myPluginId) != null) {
            //store old plugins file
            descriptor = PluginManager.findPlugin(myPluginId);

            myOldFile = descriptor.getPath();
        }

        boolean checkChecksum = true;

        // if there no checksum at server, disable check
        String expectedChecksum = myDescriptor.getChecksumSHA3_256();
        if (expectedChecksum == null) {
            checkChecksum = false;
        }

        LocalizeValue errorMessage = ExternalServiceLocalize.unknownError();
        if (checkChecksum) {
            for (int i = 0; i < MAX_TRYS; i++) {
                try {
                    pi.checkCanceled();

                    Pair<File, String> info = downloadPlugin(pi, expectedChecksum, i);

                    if (StringUtil.equal(info.getSecond(), expectedChecksum, true)) {
                        myFile = info.getFirst();
                        break;
                    }
                    else {
                        errorMessage = ExternalServiceLocalize.checksumFailed();
                        LOG.warn(
                            "Checksum check failed. Plugin: " + myPluginId +
                                ", expected: " + expectedChecksum +
                                ", actual: " + info.getSecond()
                        );
                    }
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (Throwable e) {
                    myFile = null;
                    errorMessage = LocalizeValue.ofNullable(e.getLocalizedMessage());

                    TimeoutUtil.sleep(5000L);
                }
            }
        }
        else {
            try {
                myFile = downloadPlugin(pi, "<disabled>", -1).getFirst();
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                myFile = null;
                errorMessage = LocalizeValue.ofNullable(e.getLocalizedMessage());
            }
        }

        if (myFile == null) {
            throw new PluginDownloadFailedException(myPluginId, getPluginName(), errorMessage);
        }
    }

    public void install(boolean deleteTempFile) throws IOException {
        install(null, deleteTempFile);
    }

    public void install(@Nullable ProgressIndicator indicator, boolean deleteTempFile) throws IOException {
        LOG.assertTrue(myFile != null);
        if (myOldFile != null) {
            // add command to delete the 'action script' file
            StartupActionScriptManager.ActionCommand deleteOld = new StartupActionScriptManager.DeleteCommand(myOldFile);
            StartupActionScriptManager.addActionCommand(deleteOld);
        }

        if (myIsPlatform) {
            if (indicator != null) {
                indicator.setText2Value(ExternalServiceLocalize.progressExtractingPlatform());
            }

            String prefix = Platform.current().os().isMac() ? "Consulo.app/Contents/platform/" : "Consulo/platform/";

            File platformDirectory = ContainerPathManager.get().getExternalPlatformDirectory();

            try (TarArchiveInputStream ais = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(myFile)))) {
                TarArchiveEntry tempEntry;
                while ((tempEntry = (TarArchiveEntry) ais.getNextEntry()) != null) {
                    String name = tempEntry.getName();
                    // we interest only in new build
                    if (name.startsWith(prefix) && name.length() != prefix.length()) {
                        File targetFile = new File(platformDirectory, name.substring(prefix.length(), name.length()));

                        if (tempEntry.isDirectory()) {
                            FileUtil.createDirectory(targetFile);
                        }
                        else if (tempEntry.isSymbolicLink()) {
                            FileUtil.createParentDirs(targetFile);

                            Files.createSymbolicLink(targetFile.toPath(), Paths.get(tempEntry.getLinkName()));
                        }
                        else {
                            FileUtil.createParentDirs(targetFile);

                            try (OutputStream stream = new FileOutputStream(targetFile)) {
                                StreamUtil.copyStreamContent(ais, stream);
                            }

                            targetFile.setLastModified(tempEntry.getLastModifiedDate().getTime());

                            // it's a fix for TarArchiveEntry.DEFAULT_FILE_MODE
                            if (tempEntry.getMode() == 0b111_101_101) {
                                NioPathUtil.setPosixFilePermissions(
                                    targetFile.toPath(),
                                    NioPathUtil.convertModeToFilePermissions(tempEntry.getMode())
                                );
                            }
                        }
                    }
                }
            }

            // at start - delete old version, after restart. On mac - we can't delete boot build
            String buildNumber = ApplicationInfo.getInstance().getBuild().asString();
            File oldBuild = new File(platformDirectory, "build" + buildNumber);
            if (oldBuild.exists()) {
                StartupActionScriptManager.ActionCommand deleteTemp = new StartupActionScriptManager.DeleteCommand(oldBuild);
                StartupActionScriptManager.addActionCommand(deleteTemp);
            }

            FileUtil.delete(myFile);

            myFile = null;
        }
        else {
            install(myFile, getPluginName(), deleteTempFile);
        }
    }

    public static void install(File fromFile, String pluginName, boolean deleteFromFile) throws IOException {
        // add command to unzip file to the plugins path
        String unzipPath;
        if (ZipUtil.isZipContainsFolder(fromFile)) {
            unzipPath = ContainerPathManager.get().getInstallPluginsPath();
        }
        else {
            unzipPath = ContainerPathManager.get().getInstallPluginsPath() + File.separator + pluginName;
        }

        StartupActionScriptManager.ActionCommand unzip = new StartupActionScriptManager.UnzipCommand(fromFile, new File(unzipPath));

        StartupActionScriptManager.addActionCommand(unzip);

        // add command to remove temp plugin file
        if (deleteFromFile) {
            StartupActionScriptManager.ActionCommand deleteTemp = new StartupActionScriptManager.DeleteCommand(fromFile);
            StartupActionScriptManager.addActionCommand(deleteTemp);
        }
    }

    @Nonnull
    private Pair<File, String> downloadPlugin(
        @Nonnull ProgressIndicator indicator,
        String expectedChecksum,
        int tryIndex
    ) throws IOException {
        File pluginsTemp = new File(ContainerPathManager.get().getPluginTempPath());
        if (!pluginsTemp.exists() && !pluginsTemp.mkdirs()) {
            throw new IOException(ExternalServiceLocalize.errorCannotCreateTempDir(pluginsTemp).get());
        }
        File file = FileUtil.createTempFile(pluginsTemp, "plugin_", "_download", true, false);

        indicator.checkCanceled();
        if (myIsPlatform) {
            indicator.setText2Value(ExternalServiceLocalize.progressDownloadingPlatform());
        }
        else {
            indicator.setText2Value(ExternalServiceLocalize.progressDownloadingPlugin(getPluginName()));
        }

        LOG.info("Downloading plugin: " + myPluginId + ", try: " + tryIndex + ", checksum: " + expectedChecksum);

        String downloadUrl = buildUrl(tryIndex);
        return HttpRequests.request(downloadUrl).gzip(false).connect(request -> {
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
            }
            catch (NoSuchAlgorithmException e) {
                throw new IOException(e);
            }

            request.saveToFile(file, digest, indicator);

            String checksum = Hex.encodeHexString(digest.digest()).toUpperCase(Locale.ROOT);

            String fileName = getFileName();
            File newFile = new File(file.getParentFile(), fileName);
            FileUtil.rename(file, newFile, FilePermissionCopier.BY_NIO2);
            return Pair.create(newFile, checksum);
        });
    }

    @Nonnull
    public PluginId getPluginId() {
        return myPluginId;
    }

    @Nonnull
    private String getFileName() {
        String fileName = myPluginId + "_" + myDescriptor.getVersion();
        if (myIsPlatform) {
            fileName += ".tar.gz";
        }
        else {
            fileName += ".zip";
        }
        return fileName;
    }

    @Nonnull
    public String getPluginName() {
        return ObjectUtil.notNull(myDescriptor.getName(), myPluginId.toString());
    }

    @Nonnull
    public PluginDescriptor getPluginDescriptor() {
        return myDescriptor;
    }
}
