/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.fileTypes.ex;

import com.intellij.ide.plugins.pluginsAdvertisement.PluginsAdvertiser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.*;
import com.intellij.openapi.fileTypes.impl.FileTypeRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.UnknownExtension;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.FakeVirtualFile;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.ScrollingUtil;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.FunctionUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.ide.plugins.pluginsAdvertisement.PluginsAdvertiserDialog;
import consulo.ide.plugins.pluginsAdvertisement.PluginsAdvertiserHolder;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class FileTypeChooser extends DialogWrapper {
  private JList<FileType> myList;
  private JLabel myTitleLabel;
  private ComboBox<String> myPattern;
  private JPanel myPanel;
  private JRadioButton myOpenInIdea;
  private JRadioButton myOpenAsNative;
  private JBScrollPane myListScrollPanel;
  private JRadioButton myInstallPluginFromRepository;
  private final String myFileName;

  private final Set<PluginDescriptor> myFeaturePlugins;

  private FileTypeChooser(@Nonnull List<String> patterns, @Nonnull String fileName) {
    super(true);
    myFileName = fileName;

    FileType[] fileTypes = FileTypeManager.getInstance().getRegisteredFileTypes();
    Arrays.sort(fileTypes, (fileType1, fileType2) -> {
      if (fileType1 == null) {
        return 1;
      }
      if (fileType2 == null) {
        return -1;
      }
      return fileType1.getDescription().compareToIgnoreCase(fileType2.getDescription());
    });

    final DefaultListModel<FileType> model = new DefaultListModel<>();
    for (FileType type : fileTypes) {
      if (!type.isReadOnly() && type != UnknownFileType.INSTANCE && !(type instanceof NativeFileType)) {
        model.addElement(type);
      }
    }
    myList.setModel(model);
    myPattern.setModel(new CollectionComboBoxModel<>(ContainerUtil.map(patterns, FunctionUtil.<String>id()), patterns.get(0)));

    UnknownExtension fileFeatureForChecking = new UnknownExtension(FileTypeFactory.FILE_TYPE_FACTORY_EP.getName(), fileName);
    List<PluginDescriptor> allPlugins = PluginsAdvertiserHolder.getLoadedPluginDescriptors();
    myFeaturePlugins = PluginsAdvertiser.findByFeature(allPlugins, fileFeatureForChecking);

    if (!myFeaturePlugins.isEmpty()) {
      myInstallPluginFromRepository.setSelected(true);
      myInstallPluginFromRepository.setText(FileTypesBundle.message("filetype.chooser.install.plugin"));
    }
    else {
      myInstallPluginFromRepository.setVisible(false);
    }

    setTitle(FileTypesBundle.message("filetype.chooser.title"));
    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    myTitleLabel.setText(FileTypesBundle.message("filetype.chooser.prompt", myFileName));

    myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myList.setCellRenderer(new FileTypeRenderer());

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        doOKAction();
        return true;
      }
    }.installOn(myList);

    myList.getSelectionModel().addListSelectionListener(e -> updateButtonsState());

    ScrollingUtil.selectItem(myList, PlainTextFileType.INSTANCE);

    return myPanel;
  }

  @RequiredUIAccess
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myList;
  }

  private void updateButtonsState() {
    setOKActionEnabled(myList.getSelectedIndex() != -1 || myOpenAsNative.isSelected());
  }

  @Override
  protected String getDimensionServiceKey() {
    return "#com.intellij.fileTypes.FileTypeChooser";
  }

  @Nullable
  public FileType getSelectedType() {
    if (myInstallPluginFromRepository.isSelected()) {
      return null;
    }

    return myOpenAsNative.isSelected() ? NativeFileType.INSTANCE : myList.getSelectedValue();
  }

  /**
   * Speculates if file with newName would have known file type
   */
  @Nullable
  @RequiredUIAccess
  public static FileType getKnownFileTypeOrAssociate(@Nonnull VirtualFile parent, @Nonnull String newName, @Nullable Project project) {
    return getKnownFileTypeOrAssociate(new FakeVirtualFile(parent, newName), project);
  }

  /**
   * If fileName is already associated any known file type returns it.
   * Otherwise asks user to select file type and associates it with fileName extension if any selected.
   *
   * @return Known file type or null. Never returns {@link UnknownFileType#INSTANCE}.
   */
  @Nullable
  @RequiredUIAccess
  public static FileType getKnownFileTypeOrAssociate(@Nonnull VirtualFile file, @Nullable Project project) {
    if (project != null && !(file instanceof FakeVirtualFile)) {
      PsiManagerEx.getInstanceEx(project).getFileManager().findFile(file); // autodetect text file if needed
    }
    FileType type = file.getFileType();
    if (type == UnknownFileType.INSTANCE) {
      type = getKnownFileTypeOrAssociate(file.getName());
    }
    return type;
  }

  @Nullable
  @RequiredUIAccess
  public static FileType getKnownFileTypeOrAssociate(@Nonnull String fileName) {
    FileTypeManager fileTypeManager = FileTypeManager.getInstance();
    FileType type = fileTypeManager.getFileTypeByFileName(fileName);
    if (type == UnknownFileType.INSTANCE) {
      type = associateFileType(fileName);
    }
    return type;
  }

  @Nullable
  @RequiredUIAccess
  public static FileType associateFileType(@Nonnull final String fileName) {
    final FileTypeChooser chooser = new FileTypeChooser(suggestPatterns(fileName), fileName);
    if (!chooser.showAndGet()) {
      return null;
    }
    final FileType type = chooser.getSelectedType();
    if (type == null) {
      final PluginsAdvertiserDialog advertiserDialog = new PluginsAdvertiserDialog(null, new ArrayList<>(chooser.myFeaturePlugins));
      advertiserDialog.show();
      return null;
    }

    if (type == UnknownFileType.INSTANCE) {
      return null;
    }

    ApplicationManager.getApplication()
            .runWriteAction(() -> FileTypeManagerEx.getInstanceEx().associatePattern(type, (String)chooser.myPattern.getSelectedItem()));

    return type;
  }

  @Nonnull
  static List<String> suggestPatterns(@Nonnull String fileName) {
    List<String> patterns = ContainerUtil.newLinkedList(fileName);

    int i = -1;
    while ((i = fileName.indexOf('.', i + 1)) > 0) {
      String extension = fileName.substring(i);
      if (!StringUtil.isEmpty(extension)) {
        patterns.add(0, "*" + extension);
      }
    }

    return patterns;
  }

  @Override
  protected String getHelpId() {
    return "reference.dialogs.register.association";
  }
}
