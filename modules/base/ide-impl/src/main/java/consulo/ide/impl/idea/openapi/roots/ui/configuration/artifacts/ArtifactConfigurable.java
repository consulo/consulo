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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ArtifactType;
import consulo.configurable.ConfigurationException;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.LabeledComponent;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author nik
 */
public class ArtifactConfigurable extends ArtifactConfigurableBase {
  private boolean myIsInUpdateName;

  public ArtifactConfigurable(Artifact originalArtifact, ArtifactsStructureConfigurableContextImpl artifactsStructureContext, final Runnable updateTree) {
    super(originalArtifact, artifactsStructureContext, updateTree, true);
  }

  @Override
  public void setDisplayName(String name) {
    final String oldName = getArtifact().getName();
    if (name != null && !name.equals(oldName) && !myIsInUpdateName) {
      myArtifactsStructureContext.getOrCreateModifiableArtifactModel().getOrCreateModifiableArtifact(myOriginalArtifact).setName(name);
      getEditor().updateOutputPath(oldName, name);
    }
  }

  @Override
  public void updateName() {
    myIsInUpdateName = true;
    try {
      super.updateName();
    }
    finally {
      myIsInUpdateName = false;
    }
  }

  @RequiredUIAccess
  @Override
  public JComponent createOptionsPanel(@Nonnull Disposable parentDisposable) {
    return getEditor().createMainComponent();
  }

  @Override
  protected JComponent createTopRightComponent(final JTextField nameField) {
    final ComboBox artifactTypeBox = new ComboBox();
    for (ArtifactType type : ArtifactType.EP_NAME.getExtensionList()) {
      artifactTypeBox.addItem(type);
    }

    artifactTypeBox.setRenderer(new ArtifactTypeCellRenderer());

    artifactTypeBox.setSelectedItem(getArtifact().getArtifactType());
    artifactTypeBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final ArtifactType selected = (ArtifactType)artifactTypeBox.getSelectedItem();
        if (selected != null && !Comparing.equal(selected, getArtifact().getArtifactType())) {
          getEditor().setArtifactType(selected);
        }
      }
    });

    return LabeledComponent.left(artifactTypeBox, "Type");
  }

  @Override
  public boolean isModified() {
    return getEditor().isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    getEditor().apply();
  }

  @Override
  public void reset() {
  }

  @Override
  public String getHelpTopic() {
    return getEditor().getHelpTopic();
  }

  private ArtifactEditorImpl getEditor() {
    return myArtifactsStructureContext.getOrCreateEditor(myOriginalArtifact);
  }
}
