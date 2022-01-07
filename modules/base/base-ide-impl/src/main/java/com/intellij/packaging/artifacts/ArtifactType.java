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
package com.intellij.packaging.artifacts;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.packaging.elements.*;
import com.intellij.packaging.ui.ArtifactProblemsHolder;
import com.intellij.packaging.ui.PackagingSourceItem;
import consulo.ui.image.Image;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public abstract class ArtifactType {
  @Nullable
  public static ArtifactType findById(@Nonnull @NonNls String id) {
    for (ArtifactType type : EP_NAME.getExtensionList()) {
      if (id.equals(type.getId())) {
        return type;
      }
    }
    return null;
  }


  public static final ExtensionPointName<ArtifactType> EP_NAME = ExtensionPointName.create("com.intellij.packaging.artifactType");
  private final String myId;
  private final String myTitle;

  protected ArtifactType(@NonNls String id, String title) {
    myId = id;
    myTitle = title;
  }

  public final String getId() {
    return myId;
  }

  public String getPresentableName() {
    return myTitle;
  }

  @Nonnull
  public abstract Image getIcon();

  @Nullable
  public String getDefaultPathFor(@Nonnull PackagingSourceItem sourceItem) {
    return getDefaultPathFor(sourceItem.getKindOfProducedElements());
  }

  @Nullable
  public abstract String getDefaultPathFor(@Nonnull PackagingElementOutputKind kind);

  public boolean isSuitableItem(@Nonnull PackagingSourceItem sourceItem) {
    return true;
  }

  public boolean isAvailableForAdd(@Nonnull ModulesProvider modulesProvider) {
    return true;
  }

  @Nonnull
  public abstract CompositePackagingElement<?> createRootElement(@Nonnull PackagingElementFactory packagingElementFactory, @Nonnull String artifactName);

  @Nonnull
  public List<? extends ArtifactTemplate> getNewArtifactTemplates(@Nonnull PackagingElementResolvingContext context) {
    return Collections.emptyList();
  }

  public void checkRootElement(@Nonnull CompositePackagingElement<?> rootElement, @Nonnull Artifact artifact, @Nonnull ArtifactProblemsHolder manager) {
  }

  @Nullable
  public List<? extends PackagingElement<?>> getSubstitution(@Nonnull Artifact artifact, @Nonnull PackagingElementResolvingContext context, @Nonnull ArtifactType parentType) {
    return null;
  }
}
