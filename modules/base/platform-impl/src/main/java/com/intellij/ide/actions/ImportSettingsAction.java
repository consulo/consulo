/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

/**
 * @author cdr
 */
package com.intellij.ide.actions;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.startup.StartupActionScriptManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.io.ZipUtil;
import consulo.ui.RequiredUIAccess;
import consulo.ide.updateSettings.UpdateSettings;

import javax.annotation.Nonnull;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class ImportSettingsAction extends AnAction implements DumbAware {
  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    final Component component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT);
    ChooseComponentsToExportDialog.chooseSettingsFile(PathManager.getConfigPath(), component, IdeBundle.message("title.import.file.location"),
                                                      IdeBundle.message("prompt.choose.import.file.path")).doWhenDone(ImportSettingsAction::doImport);
  }

  private static void doImport(String path) {
    final File saveFile = new File(path);
    try {
      if (!saveFile.exists()) {
        Messages.showErrorDialog(IdeBundle.message("error.cannot.find.file", presentableFileName(saveFile)), IdeBundle.message("title.file.not.found"));
        return;
      }

      ZipEntry magicEntry;
      try (ZipFile zipFile = new ZipFile(saveFile)) {
        magicEntry = zipFile.getEntry(ImportSettingsFilenameFilter.SETTINGS_JAR_MARKER);
      }

      if (magicEntry == null) {
        Messages.showErrorDialog(IdeBundle.message("error.file.contains.no.settings.to.import", presentableFileName(saveFile), promptLocationMessage()),
                                 IdeBundle.message("title.invalid.file"));
        return;
      }

      MultiMap<File, ExportSettingsAction.ExportableItem> fileToComponents = ExportSettingsAction.getExportableComponentsMap(false);
      List<ExportSettingsAction.ExportableItem> components = getComponentsStored(saveFile, fileToComponents.values());
      fileToComponents.values().retainAll(components);
      final ChooseComponentsToExportDialog dialog =
              new ChooseComponentsToExportDialog(fileToComponents, false, IdeBundle.message("title.select.components.to.import"),
                                                 IdeBundle.message("prompt.check.components.to.import"));
      if (!dialog.showAndGet()) {
        return;
      }

      final Set<ExportSettingsAction.ExportableItem> chosenComponents = dialog.getExportableComponents();
      Set<String> relativeNamesToExtract = new HashSet<>();
      for (ExportSettingsAction.ExportableItem chosenComponent : chosenComponents) {
        final File[] exportFiles = chosenComponent.getExportFiles();
        for (File exportFile : exportFiles) {
          final File configPath = new File(PathManager.getConfigPath());
          final String rPath = FileUtil.getRelativePath(configPath, exportFile);
          assert rPath != null;
          final String relativePath = FileUtil.toSystemIndependentName(rPath);
          relativeNamesToExtract.add(relativePath);
        }
      }

      relativeNamesToExtract.add(PluginManager.INSTALLED_TXT);

      final File tempFile = new File(PathManager.getPluginTempPath() + "/" + saveFile.getName());
      FileUtil.copy(saveFile, tempFile);
      File outDir = new File(PathManager.getConfigPath());
      final ImportSettingsFilenameFilter filenameFilter = new ImportSettingsFilenameFilter(relativeNamesToExtract);
      StartupActionScriptManager.ActionCommand unzip = new StartupActionScriptManager.UnzipCommand(tempFile, outDir, filenameFilter);

      StartupActionScriptManager.addActionCommand(unzip);
      // remove temp file
      StartupActionScriptManager.ActionCommand deleteTemp = new StartupActionScriptManager.DeleteCommand(tempFile);
      StartupActionScriptManager.addActionCommand(deleteTemp);

      UpdateSettings.getInstance().setLastTimeCheck(0);

      String key = ApplicationManager.getApplication().isRestartCapable()
                   ? "message.settings.imported.successfully.restart"
                   : "message.settings.imported.successfully";
      final int ret = Messages.showOkCancelDialog(
              IdeBundle.message(key, ApplicationNamesInfo.getInstance().getProductName(), ApplicationNamesInfo.getInstance().getFullProductName()),
              IdeBundle.message("title.restart.needed"), Messages.getQuestionIcon());
      if (ret == Messages.OK) {
        ((ApplicationEx)ApplicationManager.getApplication()).restart(true);
      }
    }
    catch (ZipException e1) {
      Messages.showErrorDialog(IdeBundle.message("error.reading.settings.file", presentableFileName(saveFile), e1.getMessage(), promptLocationMessage()),
                               IdeBundle.message("title.invalid.file"));
    }
    catch (IOException e1) {
      Messages.showErrorDialog(IdeBundle.message("error.reading.settings.file.2", presentableFileName(saveFile), e1.getMessage()),
                               IdeBundle.message("title.error.reading.file"));
    }
  }

  private static String presentableFileName(final File file) {
    return "'" + FileUtil.toSystemDependentName(file.getPath()) + "'";
  }

  private static String promptLocationMessage() {
    return IdeBundle.message("message.please.ensure.correct.settings");
  }

  @Nonnull
  private static List<ExportSettingsAction.ExportableItem> getComponentsStored(@Nonnull File zipFile,
                                                                               @Nonnull Collection<? extends ExportSettingsAction.ExportableItem> registeredComponents)
          throws IOException {
    File configPath = new File(PathManager.getConfigPath());
    List<ExportSettingsAction.ExportableItem> components = new ArrayList<>();
    for (ExportSettingsAction.ExportableItem component : registeredComponents) {
      for (File exportFile : component.getExportFiles()) {
        String rPath = FileUtilRt.getRelativePath(configPath, exportFile);
        assert rPath != null;
        String relativePath = FileUtilRt.toSystemIndependentName(rPath);
        if (exportFile.isDirectory()) {
          relativePath += '/';
        }
        if (ZipUtil.isZipContainsEntry(zipFile, relativePath)) {
          components.add(component);
          break;
        }
      }
    }
    return components;
  }
}
