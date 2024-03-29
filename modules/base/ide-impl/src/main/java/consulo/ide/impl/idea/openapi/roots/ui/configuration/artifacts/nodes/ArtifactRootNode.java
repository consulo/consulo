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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.nodes;

import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ArtifactEditorEx;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.ComplexElementSubstitutionParameters;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.ui.ArtifactEditorContext;

import java.util.Collections;

/**
 * @author nik
 */
public class ArtifactRootNode extends CompositePackagingElementNode {
  public ArtifactRootNode(ArtifactEditorEx artifactEditor, ArtifactEditorContext context,
                          ComplexElementSubstitutionParameters substitutionParameters, ArtifactType artifactType) {
    super(artifactEditor.getRootElement(), context, null, null, substitutionParameters, Collections.<PackagingNodeSource>emptyList(),
          artifactType);
  }

  @Override
  public boolean isAutoExpandNode() {
    return true;
  }
}
