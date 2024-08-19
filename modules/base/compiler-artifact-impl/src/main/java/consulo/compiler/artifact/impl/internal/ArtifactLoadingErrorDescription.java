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
package consulo.compiler.artifact.impl.internal;

import consulo.application.WriteAction;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ModifiableArtifactModel;
import consulo.module.ConfigurationErrorDescription;
import consulo.module.ConfigurationErrorType;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author nik
 */
public class ArtifactLoadingErrorDescription extends ConfigurationErrorDescription {
  private static final ConfigurationErrorType INVALID_ARTIFACT = new ConfigurationErrorType("artifact", false);

  private final ArtifactManager myArtifactManager;
  private final InvalidArtifact myArtifact;

  public ArtifactLoadingErrorDescription(ArtifactManager artifactManager, InvalidArtifact artifact) {
    super(artifact.getName(), artifact.getErrorMessage(), INVALID_ARTIFACT);
    myArtifactManager = artifactManager;
    myArtifact = artifact;
  }

  @RequiredUIAccess
  @Override
  public void ignoreInvalidElement() {
    final ModifiableArtifactModel model = myArtifactManager.createModifiableModel();
    model.removeArtifact(myArtifact);
    WriteAction.run(model::commit);
  }

  @Override
  public String getIgnoreConfirmationMessage() {
    return "Would you like to remove artifact '" + myArtifact.getName() + "?";
  }

  @Override
  public boolean isValid() {
    return myArtifactManager.getAllArtifactsIncludingInvalid().contains(myArtifact);
  }
}
