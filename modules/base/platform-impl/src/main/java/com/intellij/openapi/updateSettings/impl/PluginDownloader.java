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
package com.intellij.openapi.updateSettings.impl;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.InstalledPluginsTableModel;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.plugins.RepositoryHelper;
import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import consulo.logging.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.util.ObjectUtil;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.ZipUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.ide.updateSettings.UpdateSettings;
import consulo.ide.updateSettings.impl.PlatformOrPluginUpdateChecker;
import consulo.util.io2.PathUtil;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author anna
 * @since 10-Aug-2007
 */
public class PluginDownloader {
  private static final Logger LOG = Logger.getInstance(PluginDownloader.class);

  @Nonnull
  public static PluginDownloader createDownloader(@Nonnull PluginDescriptor descriptor, boolean viaUpdate) {
    return createDownloader(descriptor, null, viaUpdate);
  }

  @Nonnull
  public static PluginDownloader createDownloader(@Nonnull PluginDescriptor descriptor, @Nullable String platformVersion, boolean viaUpdate) {
    String url = RepositoryHelper.buildUrlForDownload(UpdateSettings.getInstance().getChannel(), descriptor.getPluginId().toString(), platformVersion, false, viaUpdate);

    return new PluginDownloader(descriptor, url);
  }

  private final PluginId myPluginId;
  private String myPluginUrl;

  private File myFile;
  private File myOldFile;
  private String myDescription;

  private final PluginDescriptor myDescriptor;

  private boolean myIsPlatform;

  public PluginDownloader(@Nonnull PluginDescriptor pluginDescriptor, @Nonnull String pluginUrl) {
    myPluginId = pluginDescriptor.getPluginId();
    myDescriptor = pluginDescriptor;
    myPluginUrl = pluginUrl;
    myIsPlatform = PlatformOrPluginUpdateChecker.getPlatformPluginId() == pluginDescriptor.getPluginId();
  }

  public boolean prepareToInstall(ProgressIndicator pi) throws IOException {
    PluginDescriptor descriptor = null;
    if (!Boolean.getBoolean(StartupActionScriptManager.STARTUP_WIZARD_MODE) && PluginManager.isPluginInstalled(myPluginId)) {
      //store old plugins file
      descriptor = PluginManager.getPlugin(myPluginId);

      myOldFile = descriptor.getPath();
    }

    // download plugin
    String errorMessage = IdeBundle.message("unknown.error");
    try {
      myFile = downloadPlugin(pi);
    }
    catch (IOException ex) {
      myFile = null;
      errorMessage = ex.getMessage();
    }
    if (myFile == null) {
      final String text = IdeBundle.message("error.plugin.was.not.installed", getPluginName(), errorMessage);
      final String title = IdeBundle.message("title.failed.to.download");
      ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(text, title));
      return false;
    }

    return !InstalledPluginsTableModel.wasUpdated(myDescriptor.getPluginId());
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
        indicator.setText2(IdeBundle.message("progress.extracting.platform"));
      }

      String prefix = SystemInfo.isMac ? "Consulo.app/Contents/platform/" : "Consulo/platform/";

      File platformDirectory = PathManager.getExternalPlatformDirectory();

      try (TarArchiveInputStream ais = new TarArchiveInputStream(new GzipCompressorInputStream(new FileInputStream(myFile)))) {
        TarArchiveEntry tempEntry;
        while ((tempEntry = (TarArchiveEntry)ais.getNextEntry()) != null) {
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
                PathUtil.setPosixFilePermissions(targetFile.toPath(), PathUtil.convertModeToFilePermissions(tempEntry.getMode()));
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

  public static void install(final File fromFile, final String pluginName, boolean deleteFromFile) throws IOException {
    // add command to unzip file to the plugins path
    String unzipPath;
    if (ZipUtil.isZipContainsFolder(fromFile)) {
      unzipPath = PathManager.getInstallPluginsPath();
    }
    else {
      unzipPath = PathManager.getInstallPluginsPath() + File.separator + pluginName;
    }

    StartupActionScriptManager.ActionCommand unzip = new StartupActionScriptManager.UnzipCommand(fromFile, new File(unzipPath));

    StartupActionScriptManager.addActionCommand(unzip);

    // add command to remove temp plugin file
    if (deleteFromFile) {
      StartupActionScriptManager.ActionCommand deleteTemp = new StartupActionScriptManager.DeleteCommand(fromFile);
      StartupActionScriptManager.addActionCommand(deleteTemp);
    }
  }

  private File downloadPlugin(final ProgressIndicator indicator) throws IOException {
    File pluginsTemp = new File(PathManager.getPluginTempPath());
    if (!pluginsTemp.exists() && !pluginsTemp.mkdirs()) {
      throw new IOException(IdeBundle.message("error.cannot.create.temp.dir", pluginsTemp));
    }
    final File file = FileUtil.createTempFile(pluginsTemp, "plugin_", "_download", true, false);

    indicator.checkCanceled();
    if (myIsPlatform) {
      indicator.setText2(IdeBundle.message("progress.downloading.platform"));
    }
    else {
      indicator.setText2(IdeBundle.message("progress.downloading.plugin", getPluginName()));
    }

    return HttpRequests.request(myPluginUrl).gzip(false).connect(request -> {
      request.saveToFile(file, indicator);

      String fileName = getFileName();
      File newFile = new File(file.getParentFile(), fileName);
      FileUtil.rename(file, newFile);
      return newFile;
    });
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

  public void setDescription(String description) {
    myDescription = description;
  }

  public String getDescription() {
    return myDescription;
  }

  public PluginDescriptor getDescriptor() {
    return myDescriptor;
  }
}
