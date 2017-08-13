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
package consulo.packaging.impl.artifacts;

import com.intellij.icons.AllIcons;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementOutputKind;
import com.intellij.packaging.impl.artifacts.ArtifactUtil;
import consulo.packaging.impl.elements.ZipArchivePackagingElement;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 15:59/18.06.13
 */
public class ZipArtifactType extends ArtifactType {
  protected ZipArtifactType() {
    super("zip", "Zip");
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return AllIcons.Nodes.Artifact;
  }

  @Override
  public String getDefaultPathFor(@NotNull PackagingElementOutputKind kind) {
    return "/";
  }

  @NotNull
  @Override
  public CompositePackagingElement<?> createRootElement(@NotNull String artifactName) {
    return new ZipArchivePackagingElement(ArtifactUtil.suggestArtifactFileName(artifactName) + ".zip");
  }
}
