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
package consulo.compiler.artifact.element;

import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.ui.ArtifactEditorContext;

import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author nik
 */
public abstract class CompositePackagingElementType<E extends CompositePackagingElement<?>> extends PackagingElementType<E> {
  protected CompositePackagingElementType(@Nonnull String id, @Nonnull LocalizeValue presentableName) {
    super(id, presentableName);
  }

  @Override
  public boolean isAvailableForAdd(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact) {
    return true;
  }


  @Nullable
  public abstract CompositePackagingElement<?> createComposite(CompositePackagingElement<?> parent, @Nullable String baseName, @Nonnull ArtifactEditorContext context);

  @Override
  @Nonnull
  public List<? extends PackagingElement<?>> chooseAndCreate(@Nonnull ArtifactEditorContext context, @Nonnull Artifact artifact, @Nonnull CompositePackagingElement<?> parent) {
    final PackagingElement<?> composite = createComposite(parent, null, context);
    return composite != null ? Collections.singletonList(composite) : Collections.<E>emptyList();
  }
}
