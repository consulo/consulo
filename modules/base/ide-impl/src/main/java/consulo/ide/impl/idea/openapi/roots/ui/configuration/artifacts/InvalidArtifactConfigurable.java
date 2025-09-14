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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.compiler.artifact.internal.InvalidArtifact;
import consulo.configurable.ConfigurationException;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.MultiLineLabel;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;

/**
 * @author nik
 */
public class InvalidArtifactConfigurable extends ArtifactConfigurableBase {
  private String myErrorMessage;

  public InvalidArtifactConfigurable(InvalidArtifact originalArtifact,
                                     ArtifactsStructureConfigurableContextImpl artifactsStructureContext,
                                     Runnable updateTree) {
    super(originalArtifact, artifactsStructureContext, updateTree, false);
    myErrorMessage = originalArtifact.getErrorMessage();
  }

  @Override
  public void setDisplayName(String name) {
  }

  @Override
  public JComponent createOptionsPanel() {
    return new InvalidArtifactComponent(myErrorMessage).myMainPanel;
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
  }

  @Override
  public void reset() {
  }

  private static class InvalidArtifactComponent {
    private JPanel myMainPanel;
    private MultiLineLabel myDescriptionLabel;
    private JLabel myIconLabel;

    private InvalidArtifactComponent(String errorMessage) {
      myIconLabel.setIcon(TargetAWT.to(PlatformIconGroup.generalError()));
      myDescriptionLabel.setText(errorMessage);
    }
  }
}
