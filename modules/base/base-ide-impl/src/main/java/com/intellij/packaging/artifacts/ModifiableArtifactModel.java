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

import com.intellij.packaging.elements.CompositePackagingElement;
import consulo.annotation.access.RequiredWriteAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author nik
 */
public interface ModifiableArtifactModel extends ArtifactModel {

  @Nonnull
  ModifiableArtifact addArtifact(final @Nonnull String name, @Nonnull ArtifactType artifactType);

  @Nonnull
  ModifiableArtifact addArtifact(final @Nonnull String name, @Nonnull ArtifactType artifactType, CompositePackagingElement<?> rootElement);

  void removeArtifact(@Nonnull Artifact artifact);

  @Nonnull
  ModifiableArtifact getOrCreateModifiableArtifact(@Nonnull Artifact artifact);

  @Nullable
  Artifact getModifiableCopy(Artifact artifact);

  void addListener(@Nonnull ArtifactListener listener);

  void removeListener(@Nonnull ArtifactListener listener);

  boolean isModified();

  @RequiredWriteAction
  void commit();

  void dispose();
}
