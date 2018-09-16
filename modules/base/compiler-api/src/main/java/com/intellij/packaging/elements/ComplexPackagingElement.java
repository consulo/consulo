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

import com.intellij.packaging.artifacts.ArtifactType;
import javax.annotation.Nonnull;

import java.util.List;

/**
 * @author nik
 */
public abstract class ComplexPackagingElement<S> extends PackagingElement<S> {
  protected ComplexPackagingElement(PackagingElementType type) {
    super(type);
  }

  @Override
  public void computeIncrementalCompilerInstructions(@Nonnull IncrementalCompilerInstructionCreator creator,
                                                     @Nonnull PackagingElementResolvingContext resolvingContext,
                                                     @Nonnull ArtifactIncrementalCompilerContext compilerContext, @Nonnull ArtifactType artifactType) {
    final List<? extends PackagingElement<?>> substitution = getSubstitution(resolvingContext, artifactType);
    if (substitution == null) return;

    for (PackagingElement<?> element : substitution) {
      element.computeIncrementalCompilerInstructions(creator, resolvingContext, compilerContext,
                                                     getArtifactTypeForSubstitutedElements(resolvingContext, artifactType));
    }
  }

  protected ArtifactType getArtifactTypeForSubstitutedElements(PackagingElementResolvingContext resolvingContext, ArtifactType artifactType) {
    return artifactType;
  }


  @javax.annotation.Nullable
  public abstract List<? extends PackagingElement<?>> getSubstitution(@Nonnull PackagingElementResolvingContext context, @Nonnull ArtifactType artifactType);

}
