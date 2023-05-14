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
import consulo.compiler.artifact.ui.FileCopyPresentation;
import consulo.compiler.artifact.ui.PackagingElementPresentation;
import consulo.util.io.FileUtil;
import consulo.util.io.PathUtil;
import consulo.util.lang.Comparing;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;

/**
 * @author nik
 */
public class FileCopyPackagingElement extends FileOrDirectoryCopyPackagingElement<FileCopyPackagingElement> implements RenameablePackagingElement {
  @NonNls public static final String OUTPUT_FILE_NAME_ATTRIBUTE = "output-file-name";
  private String myRenamedOutputFileName;

  public FileCopyPackagingElement() {
    super(FileCopyElementType.getInstance());
  }

  public FileCopyPackagingElement(String filePath) {
    this();
    myFilePath = filePath;
  }

  public FileCopyPackagingElement(String filePath, String outputFileName) {
    this(filePath);
    myRenamedOutputFileName = outputFileName;
  }

  @Override
  public PackagingElementPresentation createPresentation(@Nonnull ArtifactEditorContext context) {
    return new FileCopyPresentation(myFilePath, getOutputFileName());
  }

  public String getOutputFileName() {
    return myRenamedOutputFileName != null ? myRenamedOutputFileName : PathUtil.getFileName(myFilePath);
  }

  @Override
  public void computeIncrementalCompilerInstructions(@Nonnull IncrementalCompilerInstructionCreator creator,
                                                     @Nonnull PackagingElementResolvingContext resolvingContext,
                                                     @Nonnull ArtifactIncrementalCompilerContext compilerContext, @Nonnull ArtifactType artifactType) {
    final VirtualFile file = findFile();
    if (file != null && file.isValid() && !file.isDirectory()) {
      creator.addFileCopyInstruction(file, getOutputFileName());
    }
  }

  @NonNls @Override
  public String toString() {
    return "file:" + myFilePath + (myRenamedOutputFileName != null ? ",rename to:" + myRenamedOutputFileName : "");
  }

  public boolean isDirectory() {
    return new File(FileUtil.toSystemDependentName(myFilePath)).isDirectory();
  }


  @Override
  public boolean isEqualTo(@Nonnull PackagingElement<?> element) {
    return element instanceof FileCopyPackagingElement && super.isEqualTo(element)
           && Comparing.equal(myRenamedOutputFileName, ((FileCopyPackagingElement)element).getRenamedOutputFileName());
  }

  @Override
  public FileCopyPackagingElement getState() {
    return this;
  }

  @Override
  public void loadState(ArtifactManager artifactManager, FileCopyPackagingElement state) {
    setFilePath(state.getFilePath());
    setRenamedOutputFileName(state.getRenamedOutputFileName());
  }

  @jakarta.annotation.Nullable
  @Attribute(OUTPUT_FILE_NAME_ATTRIBUTE)
  public String getRenamedOutputFileName() {
    return myRenamedOutputFileName;
  }

  public void setRenamedOutputFileName(String renamedOutputFileName) {
    myRenamedOutputFileName = renamedOutputFileName;
  }

  @Override
  public String getName() {
    return getOutputFileName();
  }

  @Override
  public boolean canBeRenamed() {
    return !isDirectory();
  }

  @Override
  public void rename(@Nonnull String newName) {
    myRenamedOutputFileName = newName.equals(PathUtil.getFileName(myFilePath)) ? null : newName;
  }

  @Nullable
  public VirtualFile getLibraryRoot() {
    final String url = VirtualFileUtil.getUrlForLibraryRoot(new File(FileUtil.toSystemDependentName(getFilePath())));
    return VirtualFileManager.getInstance().findFileByUrl(url);
  }
}
