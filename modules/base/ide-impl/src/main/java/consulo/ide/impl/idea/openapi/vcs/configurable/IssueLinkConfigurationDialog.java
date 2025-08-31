/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.configurable;

import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.versionControlSystem.IssueNavigationLink;
import consulo.ui.ex.awt.event.DocumentAdapter;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author yole
 */
public class IssueLinkConfigurationDialog extends DialogWrapper {
  private JPanel myPanel;
  private JTextField myIssueIDTextField;
  private JTextField myIssueLinkTextField;
  private JLabel myErrorLabel;
  private JTextField myExampleIssueIDTextField;
  private JTextField myExampleIssueLinkTextField;

  protected IssueLinkConfigurationDialog(Project project) {
    super(project, false);
    init();
    myIssueIDTextField.getDocument().addDocumentListener(new DocumentAdapter() {
      protected void textChanged(DocumentEvent e) {
        updateFeedback();
      }
    });
    myExampleIssueIDTextField.getDocument().addDocumentListener(new DocumentAdapter() {
      protected void textChanged(DocumentEvent e) {
        updateFeedback();
      }
    });
  }

  private void updateFeedback() {
    myErrorLabel.setText(" ");
    try {
      Pattern p = Pattern.compile(myIssueIDTextField.getText());
      if (myIssueIDTextField.getText().length() > 0) {
        Matcher matcher = p.matcher(myExampleIssueIDTextField.getText());
        if (matcher.matches()) {
          myExampleIssueLinkTextField.setText(matcher.replaceAll(myIssueLinkTextField.getText()));
        }
        else {
          myExampleIssueLinkTextField.setText("<no match>");
        }
      }
    }
    catch(Exception ex) {
      myErrorLabel.setText("Invalid regular expression: " + ex.getMessage());
      myExampleIssueLinkTextField.setText("");
    }
    setOKActionEnabled(myErrorLabel.getText().equals(" "));
  }

  @Nullable
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  @Nullable
  @Override
  protected String getHelpId() {
    return "reference.settings.vcs.issue.navigation.add.link";
  }

  public IssueNavigationLink getLink() {
    return new IssueNavigationLink(myIssueIDTextField.getText(), myIssueLinkTextField.getText());
  }

  public void setLink(IssueNavigationLink link) {
    myIssueIDTextField.setText(link.getIssueRegexp());
    myIssueLinkTextField.setText(link.getLinkRegexp());
  }

  public JComponent getPreferredFocusedComponent() {
    return myIssueIDTextField;
  }
}