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
package consulo.ide.impl.idea.packaging.impl.artifacts;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.annotation.component.ServiceImpl;
import consulo.application.AllIcons;
import consulo.ide.ServiceManager;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElementFactory;
import consulo.compiler.artifact.element.PackagingElementOutputKind;
import consulo.ui.image.Image;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;

/**
 * @author nik
 */
@Singleton
@Service(ComponentScope.APPLICATION)
@ServiceImpl
public class InvalidArtifactType extends ArtifactType {

  public static InvalidArtifactType getInstance() {
    return ServiceManager.getService(InvalidArtifactType.class);
  }

  public InvalidArtifactType() {
    super("invalid", "Invalid");
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.FileTypes.Unknown;
  }

  @Override
  public String getDefaultPathFor(@Nonnull PackagingElementOutputKind kind) {
    return "";
  }

  @Nonnull
  @Override
  public CompositePackagingElement<?> createRootElement(@Nonnull PackagingElementFactory packagingElementFactory, @Nonnull String artifactName) {
    return packagingElementFactory.createArtifactRootElement();
  }
}
