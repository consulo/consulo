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

import com.intellij.packaging.artifacts.ArtifactType;
import com.intellij.packaging.elements.*;
import com.intellij.packaging.impl.ui.ArchiveElementPresentation;
import com.intellij.packaging.ui.ArtifactEditorContext;
import com.intellij.packaging.ui.PackagingElementPresentation;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author nik
 */
public abstract class ArchivePackagingElement extends CompositePackagingElement<ArchivePackagingElement> {

  @NonNls
  public static final String NAME_ATTRIBUTE = "name";

  protected String myArchiveFileName;

  public ArchivePackagingElement(@NotNull PackagingElementType<? extends ArchivePackagingElement> type) {
    super(type);
  }

  public ArchivePackagingElement(@NotNull PackagingElementType<? extends ArchivePackagingElement> type, @NotNull String archiveFileName) {
    super(type);
    myArchiveFileName = archiveFileName;
  }

  @Override
  public PackagingElementPresentation createPresentation(@NotNull ArtifactEditorContext context) {
    return new ArchiveElementPresentation(this);
  }

  @Override
  public void computeIncrementalCompilerInstructions(@NotNull IncrementalCompilerInstructionCreator creator,
                                                     @NotNull PackagingElementResolvingContext resolvingContext,
                                                     @NotNull ArtifactIncrementalCompilerContext compilerContext,
                                                     @NotNull ArtifactType artifactType) {
    computeChildrenInstructions(creator.archive(myArchiveFileName, getPackageWriter()), resolvingContext, compilerContext, artifactType);
  }

  public abstract ArchivePackageWriter<?> getPackageWriter();

  @Attribute(NAME_ATTRIBUTE)
  public String getArchiveFileName() {
    return myArchiveFileName;
  }

  @NonNls
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
  public void rename(@NotNull String newName) {
    myArchiveFileName = newName;
  }

  @Override
  public boolean isEqualTo(@NotNull PackagingElement<?> element) {
    return element instanceof ArchivePackagingElement && ((ArchivePackagingElement)element).getArchiveFileName().equals(myArchiveFileName);
  }

  @Override
  public void loadState(ArchivePackagingElement state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
