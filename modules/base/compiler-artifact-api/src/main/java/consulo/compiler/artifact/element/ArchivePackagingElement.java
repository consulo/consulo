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
import consulo.compiler.artifact.ui.ArchiveElementPresentation;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingElementPresentation;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.Attribute;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public abstract class ArchivePackagingElement extends CompositePackagingElement<ArchivePackagingElement> {
  public static final String NAME_ATTRIBUTE = "name";

  protected String myArchiveFileName;

  public ArchivePackagingElement(@Nonnull PackagingElementType<? extends ArchivePackagingElement> type) {
    super(type);
  }

  public ArchivePackagingElement(@Nonnull PackagingElementType<? extends ArchivePackagingElement> type, @Nonnull String archiveFileName) {
    super(type);
    myArchiveFileName = archiveFileName;
  }

  @Override
  public PackagingElementPresentation createPresentation(@Nonnull ArtifactEditorContext context) {
    return new ArchiveElementPresentation(this);
  }

  @Override
  public void computeIncrementalCompilerInstructions(@Nonnull IncrementalCompilerInstructionCreator creator,
                                                     @Nonnull PackagingElementResolvingContext resolvingContext,
                                                     @Nonnull ArtifactIncrementalCompilerContext compilerContext,
                                                     @Nonnull ArtifactType artifactType) {
    computeChildrenInstructions(creator.archive(myArchiveFileName, getPackageWriter()), resolvingContext, compilerContext, artifactType);
  }

  public abstract ArchivePackageWriter<?> getPackageWriter();

  @Attribute(NAME_ATTRIBUTE)
  public String getArchiveFileName() {
    return myArchiveFileName;
  }

  @Override
  public String toString() {
    return "archive:" + myArchiveFileName;
  }

  @Override
  public ArchivePackagingElement getState() {
    return this;
  }

  public void setArchiveFileName(String archiveFileName) {
    myArchiveFileName = archiveFileName;
  }

  @Override
  public String getName() {
    return myArchiveFileName;
  }

  @Override
  public void rename(@Nonnull String newName) {
    myArchiveFileName = newName;
  }

  @Override
  public boolean isEqualTo(@Nonnull PackagingElement<?> element) {
    return element instanceof ArchivePackagingElement && ((ArchivePackagingElement)element).getArchiveFileName().equals(myArchiveFileName);
  }

  @Override
  public void loadState(ArtifactManager artifactManager, ArchivePackagingElement state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
