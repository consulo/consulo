/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.packaging.impl.artifacts;

import consulo.application.AllIcons;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementOutputKind;
import consulo.ide.impl.idea.packaging.impl.artifacts.ArtifactUtil;
import consulo.ide.impl.packaging.impl.elements.ZipArchivePackagingElement;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 15:59/18.06.13
 */
public class ZipArtifactType extends ArtifactType {
  public ZipArtifactType() {
    super("zip", "Zip");
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Nodes.Artifact;
  }

  @Override
  public String getDefaultPathFor(@Nonnull PackagingElementOutputKind kind) {
    return "/";
  }

  @Nonnull
  @Override
  public CompositePackagingElement<?> createRootElement(@Nonnull PackagingElementFactory packagingElementFactory, @Nonnull String artifactName) {
    return new ZipArchivePackagingElement(ArtifactUtil.suggestArtifactFileName(artifactName) + ".zip");
  }
}
