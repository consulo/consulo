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
package consulo.task.ui;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.http.HttpProxyManager;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.project.Project;
import consulo.task.BaseRepository;
import consulo.task.TaskManager;
import consulo.task.TaskRepository;
import consulo.task.internal.TaskInternalHelper;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;

/**
 * @author Dmitry Avdeev
 */
public class BaseRepositoryEditor<T extends BaseRepository> extends TaskRepositoryEditor implements PanelWithAnchor {

  protected JBLabel myUrlLabel;
  protected JTextField myURLText;
  protected JTextField myUserNameText;
  protected JBLabel myUsernameLabel;
  protected JCheckBox myShareUrlCheckBox;
  protected JPasswordField myPasswordText;
  protected JBLabel myPasswordLabel;

  protected JButton myTestButton;
  private JPanel myPanel;
  private JBCheckBox myUseProxy;
  private JButton myProxySettingsButton;
  protected JCheckBox myUseHttpAuthenticationCheckBox;

  protected JPanel myCustomPanel;
  private JBCheckBox myAddCommitMessage;
  private JBLabel myComment;
  private JPanel myEditorPanel;
  protected JBCheckBox myLoginAnonymouslyJBCheckBox;
  protected JBTabbedPane myTabbedPane;
  private JTextPane myAdvertiser;

  private boolean myApplying;
  protected Project myProject;
  protected final T myRepository;
  private final Consumer<T> myChangeListener;
  private final Document myDocument;
  private final Editor myEditor;
  private JComponent myAnchor;

  public BaseRepositoryEditor(final Project project, final T repository, Consumer<T> changeListener) {
    myProject = project;
    myRepository = repository;
    myChangeListener = changeListener;

    myTestButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        afterTestConnection(TaskManager.getManager(project).testConnection(repository));
      }
    });

    myProxySettingsButton.addActionListener(new ActionListener() {
      @Override
      @RequiredUIAccess
      public void actionPerformed(ActionEvent e) {
        myProject.getApplication().getInstance(TaskInternalHelper.class).openProxySettings(myProject, () -> enableButtons());
      }
    });

    myURLText.setText(repository.getUrl());
    myUserNameText.setText(repository.getUsername());
    myPasswordText.setText(repository.getPassword());
    myShareUrlCheckBox.setSelected(repository.isShared());
    myUseProxy.setSelected(repository.isUseProxy());

    myUseHttpAuthenticationCheckBox.setSelected(repository.isUseHttpAuthentication());
    myUseHttpAuthenticationCheckBox.setVisible(repository.isSupported(TaskRepository.BASIC_HTTP_AUTHORIZATION));

    myLoginAnonymouslyJBCheckBox.setVisible(repository.isSupported(TaskRepository.LOGIN_ANONYMOUSLY));
    myLoginAnonymouslyJBCheckBox.setSelected(repository.isLoginAnonymously());
    myLoginAnonymouslyJBCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        loginAnonymouslyChanged(!myLoginAnonymouslyJBCheckBox.isSelected());
      }
    });

    myAddCommitMessage.setSelected(repository.isShouldFormatCommitMessage());
    myDocument = EditorFactory.getInstance().createDocument(repository.getCommitMessageFormat());
    myEditor = EditorFactory.getInstance().createEditor(myDocument);
    myEditorPanel.add(myEditor.getComponent(), BorderLayout.CENTER);
    myComment.setText("Available placeholders: " + repository.getComment());
    String advertiser = repository.getRepositoryType().getAdvertiser();
    if (advertiser != null) {
      Messages.installHyperlinkSupport(myAdvertiser);
      myAdvertiser.setText(advertiser);
    }
    else {
      myAdvertiser.setVisible(false);
    }

    installListener(myAddCommitMessage);
    installListener(myDocument);

    installListener(myURLText);
    installListener(myUserNameText);
    installListener(myPasswordText);

    installListener(myShareUrlCheckBox);
    installListener(myUseProxy);
    installListener(myUseHttpAuthenticationCheckBox);
    installListener(myLoginAnonymouslyJBCheckBox);

    enableButtons();

    JComponent customPanel = createCustomPanel();
    if (customPanel != null) {
      myCustomPanel.add(customPanel, BorderLayout.CENTER);
    }

    setAnchor(myUseProxy);
    loginAnonymouslyChanged(!myLoginAnonymouslyJBCheckBox.isSelected());
  }


  protected final void updateCustomPanel() {
    myCustomPanel.removeAll();
    JComponent customPanel = createCustomPanel();
    if (customPanel != null) {
      myCustomPanel.add(customPanel, BorderLayout.CENTER);
    }
    myCustomPanel.repaint();
  }

  private void loginAnonymouslyChanged(boolean enabled) {
    myUsernameLabel.setEnabled(enabled);
    myUserNameText.setEnabled(enabled);
    myPasswordLabel.setEnabled(enabled);
    myPasswordText.setEnabled(enabled);
    myUseHttpAuthenticationCheckBox.setEnabled(enabled);
  }

  @Nullable
  protected JComponent createCustomPanel() {
    return null;
  }

  protected void afterTestConnection(final boolean connectionSuccessful) {
  }

  protected void enableButtons() {
    myUseProxy.setEnabled(HttpProxyManager.getInstance().isHttpProxyEnabled());
    if (!HttpProxyManager.getInstance().isHttpProxyEnabled()) {
      myUseProxy.setSelected(false);
    }
  }

  protected void installListener(JCheckBox checkBox) {
    checkBox.addActionListener(e -> doApply());
  }

  protected void installListener(JTextField textField) {
    textField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        ApplicationManager.getApplication().invokeLater(() -> doApply());
      }
    });
  }

  protected void installListener(JComboBox comboBox) {
    comboBox.addItemListener(e -> doApply());
  }

  protected void installListener(final Document document) {
    document.addDocumentListener(new consulo.document.event.DocumentAdapter() {
      @Override
      public void documentChanged(consulo.document.event.DocumentEvent e) {
        doApply();
      }
    });
  }

  protected void installListener(EditorTextField editor) {
    installListener(editor.getDocument());
  }

  protected void doApply() {
    if (!myApplying) {
      try {
        myApplying = true;
        apply();
      }
      finally {
        myApplying = false;
      }
    }
  }

  @Override
  public JComponent createComponent() {
    return myPanel;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myURLText;
  }

  @Override
  public void dispose() {
    EditorFactory.getInstance().releaseEditor(myEditor);
  }

  public void apply() {

    myRepository.setUrl(myURLText.getText().trim());
    myRepository.setUsername(myUserNameText.getText().trim());
    //noinspection deprecation
    myRepository.setPassword(myPasswordText.getText());
    myRepository.setShared(myShareUrlCheckBox.isSelected());
    myRepository.setUseProxy(myUseProxy.isSelected());
    myRepository.setUseHttpAuthentication(myUseHttpAuthenticationCheckBox.isSelected());
    myRepository.setLoginAnonymously(myLoginAnonymouslyJBCheckBox.isSelected());

    myRepository.setShouldFormatCommitMessage(myAddCommitMessage.isSelected());
    myRepository.setCommitMessageFormat(myDocument.getText());

    myChangeListener.accept(myRepository);
  }

  @Override
  public JComponent getAnchor() {
    return myAnchor;
  }

  @Override
  public void setAnchor(@Nullable final JComponent anchor) {
    myAnchor = anchor;
    myUrlLabel.setAnchor(anchor);
    myUsernameLabel.setAnchor(anchor);
    myPasswordLabel.setAnchor(anchor);
    myUseProxy.setAnchor(anchor);
  }
}
