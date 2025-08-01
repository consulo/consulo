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
package consulo.versionControlSystem.impl.internal.ui.awt;

import consulo.application.util.UserHomeFileUtil;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserFactory;
import consulo.fileChooser.FileSaverDescriptor;
import consulo.fileChooser.FileSaverDialog;
import consulo.project.Project;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.*;
import consulo.util.io.CharsetToolkit;
import consulo.util.io.FileUtil;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWrapper;
import consulo.virtualFileSystem.encoding.EncodingProjectManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.io.File;
import java.nio.charset.Charset;

/**
 * @author yole
 * @since 2006-11-14
 */
public class CreatePatchConfigurationPanel {
  private static final int TEXT_FIELD_WIDTH = 70;

  private JPanel myMainPanel;
  private TextFieldWithBrowseButton myFileNameField;
  private TextFieldWithBrowseButton myBasePathField;
  private JCheckBox myReversePatchCheckbox;
  private ComboBox<Charset> myEncoding;
  private JLabel myWarningLabel;
  private final Project myProject;
  @Nullable private File myCommonParentDir;

  public CreatePatchConfigurationPanel(@Nonnull Project project) {
    myProject = project;
    initMainPanel();

    myFileNameField.addActionListener(e -> {
      FileSaverDialog dialog =
        FileChooserFactory.getInstance().createSaveFileDialog(new FileSaverDescriptor("Save Patch to", ""), myMainPanel);
      String path = FileUtil.toSystemIndependentName(getFileName());
      int idx = path.lastIndexOf("/");
      VirtualFile baseDir = idx == -1 ? project.getBaseDir()
        : LocalFileSystem.getInstance().refreshAndFindFileByIoFile(new File(path.substring(0, idx)));
      baseDir = baseDir == null ? project.getBaseDir() : baseDir;
      String name = idx == -1 ? path : path.substring(idx + 1);
      VirtualFileWrapper fileWrapper = dialog.save(baseDir, name);
      if (fileWrapper != null) {
        myFileNameField.setText(fileWrapper.getFile().getPath());
      }
    });

    myFileNameField.setTextFieldPreferredWidth(TEXT_FIELD_WIDTH);
    myBasePathField.setTextFieldPreferredWidth(TEXT_FIELD_WIDTH);
    myBasePathField.addBrowseFolderListener(new TextBrowseFolderListener(FileChooserDescriptorFactory.createSingleFolderDescriptor()));
    myWarningLabel.setForeground(JBColor.RED);
    selectBasePath(ObjectUtil.assertNotNull(myProject.getBaseDir()));
    initEncodingCombo();
  }

  public void selectBasePath(@Nonnull VirtualFile baseDir) {
    myBasePathField.setText(baseDir.getPresentableUrl());
  }

  private void initEncodingCombo() {
    DefaultComboBoxModel<Charset> encodingsModel = new DefaultComboBoxModel<>(CharsetToolkit.getAvailableCharsets());
    myEncoding.setModel(encodingsModel);
    Charset projectCharset = EncodingProjectManager.getInstance(myProject).getDefaultCharset();
    myEncoding.setSelectedItem(projectCharset);
  }

  @Nonnull
  public Charset getEncoding() {
    return (Charset)myEncoding.getSelectedItem();
  }

  private void initMainPanel() {
    myFileNameField = new TextFieldWithBrowseButton();
    myBasePathField = new TextFieldWithBrowseButton();
    myReversePatchCheckbox = new JCheckBox(VcsLocalize.createPatchReverseCheckbox().get());
    myEncoding = new ComboBox<>();
    myWarningLabel = new JLabel();

    myMainPanel = FormBuilder.createFormBuilder()
      .addLabeledComponent(VcsLocalize.createPatchFilePath().get(), myFileNameField)
      .addLabeledComponent("&Base path:", myBasePathField)
      .addComponent(myReversePatchCheckbox)
      .addLabeledComponent(VcsLocalize.createPatchEncoding().get(), myEncoding)
      .addComponent(myWarningLabel)
      .getPanel();
  }

  public void setCommonParentPath(@Nullable File commonParentPath) {
    myCommonParentDir = commonParentPath == null || commonParentPath.isDirectory() ? commonParentPath : commonParentPath.getParentFile();
  }

  private void checkExist() {
    myWarningLabel.setText(new File(getFileName()).exists() ? "File with the same name already exists" : "");
  }

  public JComponent getPanel() {
    return myMainPanel;
  }

  public String getFileName() {
    return UserHomeFileUtil.expandUserHome(myFileNameField.getText().trim());
  }

  @Nonnull
  public String getBaseDirName() {
    return UserHomeFileUtil.expandUserHome(myBasePathField.getText().trim());
  }

  public void setFileName(File file) {
    myFileNameField.setText(file.getPath());
  }

  public boolean isReversePatch() {
    return myReversePatchCheckbox.isSelected();
  }

  public void setReversePatch(boolean reverse) {
    myReversePatchCheckbox.setSelected(reverse);
  }

  public boolean isOkToExecute() {
    return validateFields() == null;
  }

  @Nullable
  private ValidationInfo verifyBaseDirPath() {
    String baseDirName = getBaseDirName();
    if (StringUtil.isEmptyOrSpaces(baseDirName)) return new ValidationInfo("Base path can't be empty!", myBasePathField);
    File baseFile = new File(baseDirName);
    if (!baseFile.exists()) return new ValidationInfo("Base dir doesn't exist", myBasePathField);
    if (myCommonParentDir != null && !FileUtil.isAncestor(baseFile, myCommonParentDir, false)) {
      return new ValidationInfo(String.format("Base path doesn't contain all selected changes (use %s)", myCommonParentDir.getPath()),
                                myBasePathField);
    }
    return null;
  }

  @Nullable
  public ValidationInfo validateFields() {
    checkExist();
    String validateNameError = PatchNameChecker.validateName(getFileName());
    if (validateNameError != null) return new ValidationInfo(validateNameError, myFileNameField);
    return verifyBaseDirPath();
  }
}