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

import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.DirectoryElementPresentation;
import consulo.compiler.artifact.ui.PackagingElementPresentation;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class DirectoryPackagingElement extends CompositePackagingElement<DirectoryPackagingElement> {
  @NonNls public static final String NAME_ATTRIBUTE = "name";
  private String myDirectoryName;

  public DirectoryPackagingElement() {
    super(DirectoryElementType.getInstance());
  }

  public DirectoryPackagingElement(String directoryName) {
    super(DirectoryElementType.getInstance());
    myDirectoryName = directoryName;
  }

  @Override
  public PackagingElementPresentation createPresentation(@Nonnull ArtifactEditorContext context) {
    return new DirectoryElementPresentation(this);
  }

  @Override
  public void computeIncrementalCompilerInstructions(@Nonnull IncrementalCompilerInstructionCreator creator,
                                                     @Nonnull PackagingElementResolvingContext resolvingContext,
                                                     @Nonnull ArtifactIncrementalCompilerContext compilerContext, @Nonnull ArtifactType artifactType) {
    computeChildrenInstructions(creator.subFolder(myDirectoryName), resolvingContext, compilerContext, artifactType);
  }

  @Override
  public DirectoryPackagingElement getState() {
    return this;
  }

  @NonNls @Override
  public String toString() {
    return "dir:" + myDirectoryName;
  }

  @Attribute(NAME_ATTRIBUTE)
  public String getDirectoryName() {
    return myDirectoryName;
  }

  public void setDirectoryName(String directoryName) {
    myDirectoryName = directoryName;
  }

  @Override
  public void rename(@Nonnull String newName) {
    myDirectoryName = newName;
  }

  @Override
  public String getName() {
    return myDirectoryName;
  }

  @Override
  public boolean isEqualTo(@Nonnull PackagingElement<?> element) {
    return element instanceof DirectoryPackagingElement && ((DirectoryPackagingElement)element).getDirectoryName().equals(myDirectoryName);
  }

  @Override
  public void loadState(ArtifactManager artifactManager, DirectoryPackagingElement state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
