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

import consulo.logging.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.artifacts.*;
import com.intellij.packaging.elements.*;
import com.intellij.packaging.impl.ui.ArtifactElementPresentation;
import com.intellij.packaging.impl.ui.DelegatedPackagingElementPresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import com.intellij.util.xmlb.annotations.Attribute;
import consulo.packaging.impl.artifacts.ArtifactPointerManagerImpl;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author nik
 */
public class ArtifactPackagingElement extends ComplexPackagingElement<ArtifactPackagingElement.ArtifactPackagingElementState> {
  private static final Logger LOG = Logger.getInstance(ArtifactPackagingElement.class);

  @NonNls
  public static final String ARTIFACT_NAME_ATTRIBUTE = "artifact-name";

  private final ArtifactPointerManager myArtifactPointerManager;
  private ArtifactPointer myArtifactPointer;

  public ArtifactPackagingElement(@Nonnull ArtifactPointerManager artifactPointerManager) {
    super(ArtifactElementType.getInstance());
    myArtifactPointerManager = artifactPointerManager;
  }

  public ArtifactPackagingElement(@Nonnull ArtifactPointerManager artifactPointerManager, @Nonnull ArtifactPointer artifactPointer) {
    this(artifactPointerManager);
    myArtifactPointer = artifactPointer;
  }

  @Override
  public List<? extends PackagingElement<?>> getSubstitution(@Nonnull PackagingElementResolvingContext context, @Nonnull ArtifactType artifactType) {
    final Artifact artifact = findArtifact(context);
    if (artifact != null) {
      final ArtifactType type = artifact.getArtifactType();
      List<? extends PackagingElement<?>> substitution = type.getSubstitution(artifact, context, artifactType);
      if (substitution != null) {
        return substitution;
      }

      final List<PackagingElement<?>> elements = new ArrayList<PackagingElement<?>>();
      final CompositePackagingElement<?> rootElement = artifact.getRootElement();
      if (rootElement instanceof ArtifactRootElement<?>) {
        elements.addAll(rootElement.getChildren());
      }
      else {
        elements.add(rootElement);
      }
      return elements;
    }
    return null;
  }

  @Override
  public void computeIncrementalCompilerInstructions(@Nonnull IncrementalCompilerInstructionCreator creator,
                                                     @Nonnull PackagingElementResolvingContext resolvingContext,
                                                     @Nonnull ArtifactIncrementalCompilerContext compilerContext,
                                                     @Nonnull ArtifactType artifactType) {
    Artifact artifact = findArtifact(resolvingContext);
    if (artifact == null) return;

    if (StringUtil.isEmpty(artifact.getOutputPath()) || artifact.getArtifactType().getSubstitution(artifact, resolvingContext, artifactType) != null) {
      super.computeIncrementalCompilerInstructions(creator, resolvingContext, compilerContext, artifactType);
      return;
    }

    VirtualFile outputFile = artifact.getOutputFile();
    if (outputFile == null) {
      LOG.debug("Output file for " + artifact + " not found");
      return;
    }
    if (!outputFile.isValid()) {
      LOG.debug("Output file for " + artifact + "(" + outputFile + ") is not valid");
      return;
    }

    if (outputFile.isDirectory()) {
      creator.addDirectoryCopyInstructions(outputFile);
    }
    else {
      creator.addFileCopyInstruction(outputFile, outputFile.getName());
    }
  }

  @Override
  protected ArtifactType getArtifactTypeForSubstitutedElements(PackagingElementResolvingContext resolvingContext, ArtifactType artifactType) {
    final Artifact artifact = findArtifact(resolvingContext);
    if (artifact != null) {
      return artifact.getArtifactType();
    }
    return artifactType;
  }

  @Override
  public PackagingElementPresentation createPresentation(@Nonnull ArtifactEditorContext context) {
    return new DelegatedPackagingElementPresentation(new ArtifactElementPresentation(myArtifactPointer, context));
  }

  @Override
  public ArtifactPackagingElementState getState() {
    final ArtifactPackagingElementState state = new ArtifactPackagingElementState();
    if (myArtifactPointer != null) {
      state.setArtifactName(myArtifactPointer.getName());
    }
    return state;
  }

  @Override
  public void loadState(ArtifactManager artifactManager, ArtifactPackagingElementState state) {
    final String name = state.getArtifactName();
    myArtifactPointer = name != null ? ((ArtifactPointerManagerImpl)myArtifactPointerManager).create(artifactManager, name) : null;
  }

  @Override
  public String toString() {
    return "artifact:" + getArtifactName();
  }

  @Override
  public boolean isEqualTo(@Nonnull PackagingElement<?> element) {
    return element instanceof ArtifactPackagingElement && myArtifactPointer != null && myArtifactPointer.equals(((ArtifactPackagingElement)element).myArtifactPointer);
  }

  @Nullable
  public Artifact findArtifact(@Nonnull PackagingElementResolvingContext context) {
    return myArtifactPointer != null ? myArtifactPointer.findArtifact(context.getArtifactModel()) : null;
  }

  @Nullable
  public String getArtifactName() {
    return myArtifactPointer != null ? myArtifactPointer.getName() : null;
  }

  @SuppressWarnings("unused")
  public void setArtifactPointer(@Nullable ArtifactPointer artifactPointer) {
    myArtifactPointer = artifactPointer;
  }

  public static class ArtifactPackagingElementState {
    private String myArtifactName;

    @Attribute(ARTIFACT_NAME_ATTRIBUTE)
    public String getArtifactName() {
      return myArtifactName;
    }

    public void setArtifactName(String artifactName) {
      myArtifactName = artifactName;
    }
  }
}
