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
package consulo.ide.impl.idea.packaging.impl.compiler;

import consulo.compiler.generic.BuildTarget;
import consulo.compiler.artifact.Artifact;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class ArtifactBuildTarget extends BuildTarget {
  private Artifact myArtifact;

  public ArtifactBuildTarget(Artifact artifact) {
    myArtifact = artifact;
  }

  public Artifact getArtifact() {
    return myArtifact;
  }

  @Nonnull
  @Override
  public String getId() {
    return myArtifact.getName();
  }
}
