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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts;

import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ModifiableArtifactModel;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElementResolvingContext;
import consulo.compiler.artifact.ui.ArtifactEditor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public interface ArtifactsStructureConfigurableContext extends PackagingElementResolvingContext {
  @Nonnull
  ModifiableArtifactModel getOrCreateModifiableArtifactModel();

  CompositePackagingElement<?> getRootElement(@Nonnull Artifact artifact);

  void editLayout(@Nonnull Artifact artifact, Runnable action);

  ArtifactEditor getOrCreateEditor(Artifact artifact);

  @Nonnull
  Artifact getOriginalArtifact(@Nonnull Artifact artifact);

  @Nullable
  ModifiableModuleModel getModifiableModuleModel();

  void queueValidation(Artifact artifact);

  @Nonnull
  ArtifactProjectStructureElement getOrCreateArtifactElement(@Nonnull Artifact artifact);

  ModifiableRootModel getOrCreateModifiableRootModel(Module module);

  ArtifactEditorSettings getDefaultSettings();
}
