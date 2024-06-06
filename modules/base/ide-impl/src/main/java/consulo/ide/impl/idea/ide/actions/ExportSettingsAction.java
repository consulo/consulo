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
package consulo.ide.impl.idea.ide.actions;

import consulo.application.Application;
import consulo.application.CommonBundle;
import consulo.application.dumb.DumbAware;
import consulo.application.impl.internal.store.IApplicationStore;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.component.store.impl.internal.StateStorageManager;
import consulo.component.util.localize.AbstractBundle;
import consulo.configurable.OptionsBundle;
import consulo.container.boot.ContainerPathManager;
import consulo.container.classloader.PluginClassLoader;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.plugins.PluginManager;
import consulo.ide.impl.idea.ide.plugins.PluginManagerCore;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.ide.impl.idea.util.io.ZipUtil;
import consulo.component.internal.inject.InjectingKey;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.Messages;
import consulo.util.collection.MultiMap;
import consulo.util.collection.Sets;
import consulo.util.lang.StringUtil;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.zip.ZipOutputStream;

public class ExportSettingsAction extends AnAction implements DumbAware {
  public static final String INSTALLED_TXT = "installed.txt";

  private final Application myApplication;
  private final IApplicationStore myApplicationStore;

  @Inject
  public ExportSettingsAction(Application application, IApplicationStore applicationStore) {
    myApplication = application;
    myApplicationStore = applicationStore;
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nullable AnActionEvent e) {
    myApplication.saveSettings();

    ChooseComponentsToExportDialog dialog = new ChooseComponentsToExportDialog(
      getExportableComponentsMap(myApplication, myApplicationStore, true),
      true,
      IdeLocalize.titleSelectComponentsToExport().get(),
      IdeLocalize.promptPleaseCheckAllComponentsToExport().get()
    );
    if (!dialog.showAndGet()) {
      return;
    }

    Set<ExportableItem> markedComponents = dialog.getExportableComponents();
    if (markedComponents.isEmpty()) {
      return;
    }

    Set<File> exportFiles = Sets.newHashSet(FileUtil.FILE_HASHING_STRATEGY);
    for (ExportableItem markedComponent : markedComponents) {
      ContainerUtil.addAll(exportFiles, markedComponent.getExportFiles());
    }

    final File saveFile = dialog.getExportFile();
    try {
      if (saveFile.exists()) {
        final int ret = Messages.showOkCancelDialog(IdeBundle.message("prompt.overwrite.settings.file", FileUtil.toSystemDependentName(saveFile.getPath())),
                                                    IdeBundle.message("title.file.already.exists"), Messages.getWarningIcon());
        if (ret != Messages.OK) return;
      }
      try (ZipOutputStream output = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(saveFile)))) {
        final File configPath = new File(ContainerPathManager.get().getConfigPath());
        final HashSet<String> writtenItemRelativePaths = new HashSet<>();
        for (File file : exportFiles) {
          final String rPath = FileUtil.getRelativePath(configPath, file);
          assert rPath != null;
          final String relativePath = FileUtil.toSystemIndependentName(rPath);
          if (file.exists()) {
            ZipUtil.addFileOrDirRecursively(output, saveFile, file, relativePath, null, writtenItemRelativePaths);
          }
        }

        exportInstalledPlugins(saveFile, output, writtenItemRelativePaths);

        final File magicFile = new File(FileUtil.getTempDirectory(), ImportSettingsFilenameFilter.SETTINGS_ZIP_MARKER);
        FileUtil.createIfDoesntExist(magicFile);
        magicFile.deleteOnExit();
        ZipUtil.addFileToZip(output, magicFile, ImportSettingsFilenameFilter.SETTINGS_ZIP_MARKER, writtenItemRelativePaths, null);
      }
      ShowFilePathAction
              .showDialog(e == null ? null : e.getData(Project.KEY), IdeBundle.message("message.settings.exported.successfully"), IdeBundle.message("title.export.successful"),
                          saveFile, null);
    }
    catch (IOException e1) {
      Messages.showErrorDialog(IdeBundle.message("error.writing.settings", e1.toString()), IdeBundle.message("title.error.writing.file"));
    }
  }

  private static void exportInstalledPlugins(File saveFile, ZipOutputStream output, HashSet<String> writtenItemRelativePaths) throws IOException {
    final Set<String> oldPlugins = new LinkedHashSet<>();
    for (PluginDescriptor descriptor : consulo.container.plugin.PluginManager.getPlugins()) {
      if (!PluginIds.isPlatformPlugin(descriptor.getPluginId()) && descriptor.isEnabled()) {
        oldPlugins.add(descriptor.getPluginId().getIdString());
      }
    }
    if (!oldPlugins.isEmpty()) {
      final File tempFile = File.createTempFile("installed", "plugins");
      tempFile.deleteOnExit();
      Files.write(tempFile.toPath(), oldPlugins, StandardCharsets.UTF_8);
      ZipUtil.addDirToZipRecursively(output, saveFile, tempFile, "/" + INSTALLED_TXT, null, writtenItemRelativePaths);
    }
  }

  @Nonnull
  public static MultiMap<File, ExportableItem> getExportableComponentsMap(Application application, IApplicationStore applicationStore, final boolean onlyExisting) {
    final MultiMap<File, ExportableItem> result = MultiMap.createLinkedSet();

    final StateStorageManager storageManager = applicationStore.getStateStorageManager();
    for (InjectingKey<?> key : application.getInjectingContainer().getKeys()) {
      Class<?> targetClass = key.getTargetClass();
      State stateAnnotation = targetClass.getAnnotation(State.class);
      if (stateAnnotation != null && !StringUtil.isEmpty(stateAnnotation.name())) {
        int storageIndex;
        Storage[] storages = stateAnnotation.storages();
        if (storages.length == 1) {
          storageIndex = 0;
        }
        else if (storages.length > 1) {
          storageIndex = storages.length - 1;
        }
        else {
          continue;
        }

        Storage storage = storages[storageIndex];
        if (storage.roamingType() != RoamingType.DISABLED) {
          String fileSpec = storageManager.buildFileSpec(storage);

          if (!fileSpec.startsWith(StoragePathMacros.APP_CONFIG)) {
            continue;
          }

          File file = new File(storageManager.expandMacros(fileSpec));

          File additionalExportFile = null;
          if (!StringUtil.isEmpty(stateAnnotation.additionalExportFile())) {
            additionalExportFile = new File(storageManager.expandMacros(stateAnnotation.additionalExportFile()));
            if (onlyExisting && !additionalExportFile.exists()) {
              additionalExportFile = null;
            }
          }

          boolean fileExists = !onlyExisting || file.exists();
          if (fileExists || additionalExportFile != null) {
            File[] files;
            if (additionalExportFile == null) {
              files = new File[]{file};
            }
            else {
              files = fileExists ? new File[]{file, additionalExportFile} : new File[]{additionalExportFile};
            }
            ExportableItem item = new ExportableItem(files, getComponentPresentableName(stateAnnotation, targetClass));
            result.putValue(file, item);
            if (additionalExportFile != null) {
              result.putValue(additionalExportFile, item);
            }
          }
        }
      }
    }
    return result;
  }

  @Nonnull
  private static String getComponentPresentableName(@Nonnull State state, @Nonnull Class<?> aClass) {
    String defaultName = state.name();
    String resourceBundleName;

    PluginDescriptor pluginDescriptor = null;
    ClassLoader classLoader = aClass.getClassLoader();

    if(classLoader instanceof PluginClassLoader) {
      pluginDescriptor = PluginManager.getPlugin(((PluginClassLoader)classLoader).getPluginId());
    }

    if (pluginDescriptor != null && !PluginManagerCore.CORE_PLUGIN.equals(pluginDescriptor.getPluginId())) {
      resourceBundleName = pluginDescriptor.getResourceBundleBaseName();
    }
    else {
      resourceBundleName = OptionsBundle.PATH_TO_BUNDLE;
    }

    if (resourceBundleName == null) {
      return defaultName;
    }

    if (classLoader != null) {
      ResourceBundle bundle = AbstractBundle.getResourceBundle(resourceBundleName, classLoader);
      if (bundle != null) {
        return CommonBundle.messageOrDefault(bundle, "exportable." + defaultName + ".presentable.name", defaultName);
      }
    }
    return defaultName;
  }

  public static final class ExportableItem {
    private final File[] files;
    private final String name;

    public ExportableItem(@Nonnull File[] files, @Nonnull String name) {
      this.files = files;
      this.name = name;
    }

    @Nonnull
    public File[] getExportFiles() {
      return files;
    }

    @Nonnull
    public String getPresentableName() {
      return name;
    }
  }
}

