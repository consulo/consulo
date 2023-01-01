/*
 * Copyright 2013-2022 consulo.io
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
package consulo.compiler.artifact.element;

import consulo.application.AllIcons;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.project.Project;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import java.util.List;

@SuppressWarnings("ExtensionImplIsNotAnnotatedInspection")
public class ArtifactRootElementType extends PackagingElementType<ArtifactRootElement<?>> {
  public static final ArtifactRootElementType INSTANCE = new ArtifactRootElementType();

  private ArtifactRootElementType() {
    super("root", "");
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Nodes.Artifact;
  }

  @Override
  public boolean isAvailableForAdd(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact) {
    return false;
  }

  @Override
  @Nonnull
  public List<? extends ArtifactRootElement<?>> chooseAndCreate(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact, @Nonnull CompositePackagingElement<?> parent) {
    throw new UnsupportedOperationException("'create' not implemented in " + getClass().getName());
  }

  @Override
  @Nonnull
  public ArtifactRootElement<?> createEmpty(@Nonnull Project project) {
    return new ArtifactRootElementImpl();
  }
}
