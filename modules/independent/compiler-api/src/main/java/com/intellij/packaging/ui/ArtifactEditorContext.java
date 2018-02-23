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
package com.intellij.packaging.ui;

import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.packaging.artifacts.Artifact;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.packaging.elements.CompositePackagingElement;
import com.intellij.packaging.elements.PackagingElementResolvingContext;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author nik
 */
public interface ArtifactEditorContext extends PackagingElementResolvingContext {

  void queueValidation();

  @Nonnull
  ArtifactType getArtifactType();

  @Nonnull
  ModifiableArtifactModel getOrCreateModifiableArtifactModel();

  @Nullable
  ModifiableModuleModel getModifiableModuleModel();

  @Nonnull
  ModifiableRootModel getOrCreateModifiableRootModel(@Nonnull Module module);

  CompositePackagingElement<?> getRootElement(@Nonnull Artifact artifact);

  void editLayout(@Nonnull Artifact artifact, Runnable runnable);

  ArtifactEditor getOrCreateEditor(Artifact originalArtifact);

  ArtifactEditor getThisArtifactEditor();

  void selectArtifact(@Nonnull Artifact artifact);

  void selectModule(@Nonnull Module module);

  void selectLibrary(@Nonnull Library library);


  List<Artifact> chooseArtifacts(List<? extends Artifact> artifacts, String title);

  List<Module> chooseModules(List<Module> modules, final String title);

  List<Library> chooseLibraries(String title);

  Artifact getArtifact();
}
