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
package consulo.ide.impl.idea.openapi.fileTypes.ex;

import consulo.container.plugin.PluginExtensionPreview;
import consulo.ide.impl.idea.ide.plugins.pluginsAdvertisement.PluginsAdvertiser;
import consulo.application.ApplicationManager;
import consulo.ide.impl.idea.openapi.fileTypes.*;
import consulo.ide.impl.idea.openapi.fileTypes.impl.FileTypeRenderer;
import consulo.language.file.FileTypeManager;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.LabeledComponent;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.openapi.vfs.newvfs.impl.FakeVirtualFile;
import consulo.language.impl.internal.psi.PsiManagerEx;
import consulo.ui.ex.awt.CollectionComboBoxModel;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.ScrollingUtil;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.awt.JBRadioButton;
import consulo.ide.impl.idea.util.FunctionUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.container.plugin.PluginDescriptor;
import consulo.ide.impl.plugins.pluginsAdvertisement.PluginsAdvertiserDialog;
import consulo.ide.impl.plugins.pluginsAdvertisement.PluginsAdvertiserHolder;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import consulo.virtualFileSystem.fileType.UnknownFileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class FileTypeChooser extends DialogWrapper {
  private JList<FileType> myList;
  private ComboBox<String> myPattern;
  private JPanel myPanel;
  private JRadioButton myOpenInIdea;
  private JRadioButton myOpenAsNative;
  private JRadioButton myInstallPluginFromRepository;

  private final Set<PluginDescriptor> myFeaturePlugins;
  private final List<PluginDescriptor> myAllPlugins;

  private FileTypeChooser(@Nonnull List<String> patterns, @Nonnull String fileName) {
    super(true);

    myPanel = new JPanel(new VerticalFlowLayout());
    myPanel.add(new JBLabel(FileTypesBundle.message("filetype.chooser.prompt", fileName)));

    myPattern = new ComboBox<>();
    myPattern.setEditable(true);
    myPanel.add(LabeledComponent.create(myPattern, FileTypesBundle.message("filetype.chooser.file.pattern")));

    ButtonGroup group = new ButtonGroup();
    myOpenInIdea = new JBRadioButton("Open matching files as type:");
    group.add(myOpenInIdea);

    myList = new JBList<>();
    myList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myList.setCellRenderer(new FileTypeRenderer());

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent e) {
        if (myList.isEnabled()) {
          doOKAction();
        }
        return true;
      }
    }.installOn(myList);

    myList.getSelectionModel().addListSelectionListener(e -> updateButtonsState());

    ScrollingUtil.selectItem(myList, PlainTextFileType.INSTANCE);

    myPanel.add(myOpenInIdea);
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myList);
    myPanel.add(scrollPane);
    myOpenAsNative = new JBRadioButton("Open matching files in associated application");
    group.add(myOpenAsNative);

    myPanel.add(myOpenAsNative);
    myInstallPluginFromRepository = new JBRadioButton(FileTypesBundle.message("filetype.chooser.install.plugin"));
    group.add(myInstallPluginFromRepository);

    myPanel.add(myInstallPluginFromRepository);

    FileType[] fileTypes = FileTypeManager.getInstance().getRegisteredFileTypes();
    Arrays.sort(fileTypes, (fileType1, fileType2) -> fileType1.getDescription().get().compareToIgnoreCase(fileType2.getDescription().get()));

    final DefaultListModel<FileType> model = new DefaultListModel<>();
    for (FileType type : fileTypes) {
      if (!type.isReadOnly() && type != UnknownFileType.INSTANCE && !(type instanceof NativeFileType)) {
        model.addElement(type);
      }
    }
    myList.setModel(model);
    myPattern.setModel(new CollectionComboBoxModel<>(ContainerUtil.map(patterns, FunctionUtil.<String>id()), patterns.get(0)));

    PluginExtensionPreview fileFeatureForChecking = new PluginExtensionPreview(FileTypeFactory.class, fileName);
    myAllPlugins = PluginsAdvertiserHolder.getLoadedPluginDescriptors();
    myFeaturePlugins = PluginsAdvertiser.findImpl(myAllPlugins, fileFeatureForChecking);

    ItemListener listener = e -> {
      boolean selected = myOpenInIdea.isSelected();

      myList.setEnabled(selected);
      scrollPane.setEnabled(selected);
      scrollPane.getViewport().setEnabled(selected);
    };

    myOpenInIdea.addItemListener(listener);
    myInstallPluginFromRepository.addItemListener(listener);
    myOpenAsNative.addItemListener(listener);

    if (!myFeaturePlugins.isEmpty()) {
      myInstallPluginFromRepository.setSelected(true);
    }
    else {
      myOpenInIdea.setSelected(true);

      myInstallPluginFromRepository.setVisible(false);
    }

    setTitle(FileTypesBundle.message("filetype.chooser.title"));
    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  @RequiredUIAccess
  @Override
  public JComponent getPreferredFocusedComponent() {
    return myList.isEnabled() ? myList : null;
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
      final PluginsAdvertiserDialog advertiserDialog = new PluginsAdvertiserDialog(null, chooser.myAllPlugins, new ArrayList<>(chooser.myFeaturePlugins));
      advertiserDialog.show();
      return null;
    }

    if (type == UnknownFileType.INSTANCE) {
      return null;
    }

    ApplicationManager.getApplication().runWriteAction(() -> FileTypeManagerEx.getInstanceEx().associatePattern(type, (String)chooser.myPattern.getSelectedItem()));

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
