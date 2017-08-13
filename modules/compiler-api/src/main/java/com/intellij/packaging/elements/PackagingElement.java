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
package com.intellij.packaging.elements;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public abstract class PackagingElement<S> implements PersistentStateComponent<S> {
  private final PackagingElementType myType;

  protected PackagingElement(PackagingElementType type) {
    myType = type;
  }

  public abstract PackagingElementPresentation createPresentation(@NotNull ArtifactEditorContext context);

  public final PackagingElementType getType() {
    return myType;
  }

  public abstract void computeIncrementalCompilerInstructions(@NotNull IncrementalCompilerInstructionCreator creator, @NotNull PackagingElementResolvingContext resolvingContext,
                                                              @NotNull ArtifactIncrementalCompilerContext compilerContext,
                                                              @NotNull ArtifactType artifactType);

  public abstract boolean isEqualTo(@NotNull PackagingElement<?> element);

  @NotNull
  public PackagingElementOutputKind getFilesKind(PackagingElementResolvingContext context) {
    return PackagingElementOutputKind.OTHER;
  }
}
