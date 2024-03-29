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

import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.element.CompositePackagingElement;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementType;
import consulo.compiler.artifact.ui.ArtifactEditor;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import jakarta.annotation.Nonnull;

import javax.swing.*;

/**
 * @author nik
 */
public interface ArtifactEditorEx extends ArtifactEditor, Disposable {
  Key<ArtifactEditorEx> ARTIFACTS_EDITOR_KEY = Key.create("artifactsEditor");

  void addNewPackagingElement(@Nonnull PackagingElementType<?> type);

  void removeSelectedElements();

  void removePackagingElement(@Nonnull String pathToParent, @Nonnull PackagingElement<?> element);

  void replacePackagingElement(@Nonnull String pathToParent, @Nonnull PackagingElement<?> element, @Nonnull PackagingElement<?> replacement);

  LayoutTreeComponent getLayoutTreeComponent();

  Artifact getArtifact();

  CompositePackagingElement<?> getRootElement();

  ArtifactEditorContext getContext();

  JComponent getMainComponent();

  ComplexElementSubstitutionParameters getSubstitutionParameters();

  void queueValidation();

  void rebuildTries();
}
