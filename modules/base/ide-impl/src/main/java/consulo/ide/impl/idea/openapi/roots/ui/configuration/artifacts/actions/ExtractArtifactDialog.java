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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions;

import consulo.application.CommonBundle;
import consulo.project.ProjectBundle;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactTypeCellRenderer;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.LayoutTreeComponent;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.PlainArtifactType;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.ui.ex.awt.event.DocumentAdapter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

/**
 * @author nik
 */
public class ExtractArtifactDialog extends DialogWrapper implements IExtractArtifactDialog {
  private JPanel myMainPanel;
  private JTextField myNameField;
  private JComboBox myTypeBox;
  private final ArtifactEditorContext myContext;

  public ExtractArtifactDialog(ArtifactEditorContext context, LayoutTreeComponent treeComponent, String initialName) {
    super(treeComponent.getLayoutTree(), true);
    myContext = context;
    setTitle(ProjectBundle.message("dialog.title.extract.artifact"));
    for (ArtifactType type : ArtifactType.EP_NAME.getExtensions()) {
      myTypeBox.addItem(type);
    }
    myTypeBox.setSelectedItem(PlainArtifactType.getInstance());
    myTypeBox.setRenderer(new ArtifactTypeCellRenderer());
    myNameField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        setOKActionEnabled(!StringUtil.isEmptyOrSpaces(getArtifactName()));
      }
    });
    myNameField.setText(initialName);
    init();
  }

  @Override
  protected void doOKAction() {
    final String artifactName = getArtifactName();
    if (myContext.getArtifactModel().findArtifact(artifactName) != null) {
      Messages.showErrorDialog(myContext.getProject(), "Artifact '" + artifactName + "' already exists!", CommonBundle.getErrorTitle());
      return;
    }
    super.doOKAction();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myMainPanel;
  }

  @Override
  public String getArtifactName() {
    return myNameField.getText();
  }

  @Override
  public ArtifactType getArtifactType() {
    return (ArtifactType)myTypeBox.getSelectedItem();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myNameField;
  }
}
