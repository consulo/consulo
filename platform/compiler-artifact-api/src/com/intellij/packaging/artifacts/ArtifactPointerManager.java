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

import org.consulo.util.pointers.NamedPointerManager;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.DeprecationInfo;

/**
 * @author nik
 */
public interface ArtifactPointerManager extends NamedPointerManager<Artifact> {
  @NotNull
  @Override
  ArtifactPointer create(@NotNull String name);

  @NotNull
  @Override
  ArtifactPointer create(@NotNull Artifact value);

  @NotNull
  ArtifactPointer create(@NotNull Artifact artifact, @NotNull ArtifactModel artifactModel);

  @NotNull
  @Deprecated
  @DeprecationInfo(value = "Use #create(String)", until = "1.0")
  ArtifactPointer createPointer(@NotNull String name);

  @NotNull
  @Deprecated
  @DeprecationInfo(value = "Use #create(Artifact)", until = "1.0")
  ArtifactPointer createPointer(@NotNull Artifact artifact);

  @NotNull
  @Deprecated
  @DeprecationInfo(value = "Use #create(Artifact, ArtifactModel)", until = "1.0")
  ArtifactPointer createPointer(@NotNull Artifact artifact, @NotNull ArtifactModel artifactModel);
}
