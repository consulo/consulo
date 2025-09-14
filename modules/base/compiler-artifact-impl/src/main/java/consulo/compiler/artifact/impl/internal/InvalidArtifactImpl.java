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

import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.internal.InvalidArtifact;
import consulo.compiler.artifact.internal.InvalidArtifactType;
import consulo.compiler.artifact.impl.internal.state.ArtifactState;

/**
 * @author nik
 */
public class InvalidArtifactImpl extends ArtifactImpl implements InvalidArtifact {
  private ArtifactState myState;
  private final String myErrorMessage;

  public InvalidArtifactImpl(PackagingElementFactory packagingElementFactory, ArtifactState state, String errorMessage) {
    super(state.getName(), InvalidArtifactType.getInstance(), false, packagingElementFactory.createArtifactRootElement(), "");
    myState = state;
    myErrorMessage = errorMessage;
  }

  public String getErrorMessage() {
    return myErrorMessage;
  }

  public ArtifactState getState() {
    return myState;
  }
}
