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
package com.intellij.packaging.impl.elements;

import consulo.ui.ex.tree.PresentationData;
import consulo.compiler.CompilerBundle;
import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.element.ArtifactIncrementalCompilerContext;
import consulo.compiler.artifact.element.ArtifactRootElement;
import consulo.compiler.artifact.element.IncrementalCompilerInstructionCreator;
import consulo.compiler.artifact.element.PackagingElementResolvingContext;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingElementPresentation;
import consulo.ui.ex.SimpleTextAttributes;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class ArtifactRootElementImpl extends ArtifactRootElement<Object> {
  public ArtifactRootElementImpl() {
    super(PackagingElementFactoryImpl.ARTIFACT_ROOT_ELEMENT_TYPE);
  }

  @Override
  public PackagingElementPresentation createPresentation(@Nonnull final ArtifactEditorContext context) {
    return new PackagingElementPresentation() {
      @Override
      public String getPresentableName() {
        return CompilerBundle.message("packaging.element.text.output.root");
      }

      @Override
      public void render(@Nonnull PresentationData presentationData, SimpleTextAttributes mainAttributes,
                         SimpleTextAttributes commentAttributes) {
        presentationData.setIcon(context.getArtifactType().getIcon());
        presentationData.addText(getPresentableName(), mainAttributes);
      }

      @Override
      public int getWeight() {
        return 0;
      }
    };
  }

  @Override
  public Object getState() {
    return null;
  }

  @Override
  public void loadState(ArtifactManager artifactManager, Object state) {
  }

  @Override
  public boolean canBeRenamed() {
    return false;
  }

  @Override
  public void rename(@Nonnull String newName) {
  }

  @Override
  public void computeIncrementalCompilerInstructions(@Nonnull IncrementalCompilerInstructionCreator creator,
                                                     @Nonnull PackagingElementResolvingContext resolvingContext,
                                                     @Nonnull ArtifactIncrementalCompilerContext compilerContext, @Nonnull ArtifactType artifactType) {
    computeChildrenInstructions(creator, resolvingContext, compilerContext, artifactType);
  }

  @Override
  public String getName() {
    return "";
  }

  @Override
  public String toString() {
    return "<root>";
  }
}
