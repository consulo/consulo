/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package consulo.execution.configuration.log.ui;

import consulo.execution.localize.ExecutionLocalize;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ui.ex.UIBundle;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.TextComponentAccessor;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.ui.ex.awt.event.DocumentAdapter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

/**
 * @author anna
 * @since 2006-02-05
 */
public class EditLogPatternDialog extends DialogWrapper {

  private JPanel myWholePanel;
  private JTextField myNameField;
  private JCheckBox myShowFilesCombo;
  private TextFieldWithBrowseButton myFilePattern;

  public EditLogPatternDialog() {
    super(true);
    setTitle(ExecutionLocalize.logMonitorEditAliasesTitle());
    init();
  }

  public void init(String name, String pattern, boolean showAll){
    myNameField.setText(name);
    myFilePattern.setText(pattern);
    myShowFilesCombo.setSelected(showAll);
    setOKActionEnabled(pattern != null && pattern.length() > 0);
  }

  @Override
  protected JComponent createCenterPanel() {
    myFilePattern.addBrowseFolderListener(UIBundle.message("file.chooser.default.title"), null, null, FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor(), TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
    myFilePattern.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        setOKActionEnabled(myFilePattern.getText() != null && myFilePattern.getText().length() > 0);
      }
    });
    return myWholePanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myNameField;
  }

  public boolean isShowAllFiles() {
    return myShowFilesCombo.isSelected();
  }

  public String getName(){
    String name = myNameField.getText();
    if (name != null && name.length() > 0){
      return name;
    }
    return myFilePattern.getText();
  }

  public String getLogPattern(){
    return myFilePattern.getText();
  }


  @Override
  protected String getHelpId() {
    return "reference.run.configuration.edit.logfile.aliases";
  }
}
