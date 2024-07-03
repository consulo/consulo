/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.execution.test.export;

import consulo.application.AllIcons;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.execution.localize.ExecutionLocalize;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.proxy.EventDispatcher;
import consulo.ui.ex.awt.TextComponentAccessor;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.ui.ex.awt.UserActivityWatcher;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.event.UserActivityListener;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

public class ExportTestResultsForm {
  private JRadioButton myXmlRb;
  private JRadioButton myBundledTemplateRb;
  private TextFieldWithBrowseButton myCustomTemplateField;
  private TextFieldWithBrowseButton myFolderField;
  private JPanel myContentPane;
  private JLabel myOutputFolderLabel;
  private JRadioButton myCustomTemplateRb;
  private JTextField myFileNameField;
  private JLabel myMessageLabel;
  private JCheckBox myOpenExportedFileCb;

  private final EventDispatcher<ChangeListener> myEventDispatcher = EventDispatcher.create(ChangeListener.class);

  public ExportTestResultsForm(ExportTestResultsConfiguration config, String defaultFileName, String defaultFolder) {
    ActionListener listener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        updateOnFormatChange();
      }
    };
    myXmlRb.addActionListener(listener);
    myBundledTemplateRb.addActionListener(listener);
    myCustomTemplateRb.addActionListener(listener);

    myOutputFolderLabel.setLabelFor(myFolderField.getChildComponent());

    myFileNameField.setText(defaultFileName);

    myCustomTemplateField.addBrowseFolderListener(
      ExecutionLocalize.exportTestResultsCustomTemplateChooserTitle().get(),
      null,
      null,
      new FileChooserDescriptor(true, false, false, false, false, false) {
        public boolean isFileSelectable(VirtualFile file) {
          return "xsl".equalsIgnoreCase(file.getExtension()) || "xslt".equalsIgnoreCase(file.getExtension());
        }
      },
      TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT,
      false
    );

    myFolderField.addBrowseFolderListener(
      ExecutionLocalize.exportTestResultsOutputFolderChooserTitle().get(),
      null,
      null,
      FileChooserDescriptorFactory.createSingleFolderDescriptor(),
      TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT,
      false
    );

    myFileNameField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        updateOpenInLabel();
      }
    });

    UserActivityWatcher watcher = new UserActivityWatcher();
    watcher.register(myContentPane);
    watcher.addUserActivityListener(new UserActivityListener() {
      @Override
      public void stateChanged() {
        myEventDispatcher.getMulticaster().stateChanged(new ChangeEvent(this));
      }
    });

    myMessageLabel.setIcon(TargetAWT.to(AllIcons.General.BalloonWarning));
    JRadioButton b;
    if (config.getExportFormat() == ExportTestResultsConfiguration.ExportFormat.Xml) {
      b = myXmlRb;
    }
    else if (config.getExportFormat() == ExportTestResultsConfiguration.ExportFormat.BundledTemplate) {
      b = myBundledTemplateRb;
    }
    else {
      b = myCustomTemplateRb;
    }
    b.setSelected(true);
    IdeFocusManager.findInstanceByComponent(myContentPane).requestFocus(b, true);
    myFolderField.setText(defaultFolder);
    myCustomTemplateField.setText(FileUtil.toSystemDependentName(StringUtil.notNullize(config.getUserTemplatePath())));
    myOpenExportedFileCb.setSelected(config.isOpenResults());
    updateOnFormatChange();
    updateOpenInLabel();
  }

  private void updateOpenInLabel() {
    myOpenExportedFileCb.setText(
      shouldOpenInBrowser(myFileNameField.getText())
        ? ExecutionLocalize.exportTestResultsOpenBrowser().get()
        : ExecutionLocalize.exportTestResultsOpenEditor().get()
    );
  }

  public static boolean shouldOpenInBrowser(String filename) {
    return StringUtil.isNotEmpty(filename) && (filename.endsWith(".html") || filename.endsWith(".htm"));
  }

  private void updateOnFormatChange() {
    if (getExportFormat() == ExportTestResultsConfiguration.ExportFormat.UserTemplate) {
      myCustomTemplateField.setEnabled(true);
      IdeFocusManager.findInstanceByComponent(myContentPane).requestFocus(myCustomTemplateField.getChildComponent(), true);
    }
    else {
      myCustomTemplateField.setEnabled(false);
    }
    String filename = myFileNameField.getText();
    if (filename != null && filename.indexOf('.') != -1) {
      myFileNameField.setText(filename.substring(0, filename.lastIndexOf('.') + 1) + getExportFormat().getDefaultExtension());
    }
  }

  public void apply(ExportTestResultsConfiguration config) {
    config.setExportFormat(getExportFormat());
    config.setUserTemplatePath(FileUtil.toSystemIndependentName(myCustomTemplateField.getText()));
    config.setOutputFolder(FileUtil.toSystemIndependentName(myFolderField.getText()));
    config.setOpenResults(myOpenExportedFileCb.isSelected());
  }

  private ExportTestResultsConfiguration.ExportFormat getExportFormat() {
    if (myXmlRb.isSelected()) return ExportTestResultsConfiguration.ExportFormat.Xml;
    if (myBundledTemplateRb.isSelected()) return ExportTestResultsConfiguration.ExportFormat.BundledTemplate;
    return ExportTestResultsConfiguration.ExportFormat.UserTemplate;
  }

  public JComponent getContentPane() {
    return myContentPane;
  }

  public void addChangeListener(ChangeListener changeListener) {
    myEventDispatcher.addListener(changeListener);
  }

  @Nullable
  public String validate() {
    if (getExportFormat() == ExportTestResultsConfiguration.ExportFormat.UserTemplate) {
      if (StringUtil.isEmpty(myCustomTemplateField.getText())) {
        return ExecutionLocalize.exportTestResultsCustomTemplatePathEmpty().get();
      }
      File file = new File(myCustomTemplateField.getText());
      if (!file.isFile()) {
        return ExecutionLocalize.exportTestResultsCustomTemplateNotFound(file.getAbsolutePath()).get();
      }
    }

    if (StringUtil.isEmpty(myFileNameField.getText())) {
      return ExecutionLocalize.exportTestResultsOutputFilenameEmpty().get();
    }
    if (StringUtil.isEmpty(myFolderField.getText())) {
      return ExecutionLocalize.exportTestResultsOutputPathEmpty().get();
    }

    return null;
  }

  public void showMessage(@Nullable String message) {
    myMessageLabel.setText(message);
    boolean visible = myMessageLabel.isVisible();
    myMessageLabel.setVisible(message != null);
  }

  public JComponent getPreferredFocusedComponent() {
    return myFileNameField;
  }

  public String getFileName() {
    return myFileNameField.getText();
  }
}
