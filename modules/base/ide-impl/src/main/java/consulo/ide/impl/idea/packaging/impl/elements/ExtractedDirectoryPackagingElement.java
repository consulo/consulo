/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.packaging.impl.elements;

import consulo.ide.impl.idea.openapi.util.Comparing;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.compiler.artifact.ArtifactManager;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import consulo.compiler.artifact.ArtifactType;
import consulo.compiler.artifact.element.ArtifactIncrementalCompilerContext;
import consulo.compiler.artifact.element.IncrementalCompilerInstructionCreator;
import consulo.compiler.artifact.element.PackagingElement;
import consulo.compiler.artifact.element.PackagingElementResolvingContext;
import consulo.ide.impl.idea.packaging.impl.ui.ExtractedDirectoryPresentation;
import consulo.compiler.artifact.ui.ArtifactEditorContext;
import consulo.compiler.artifact.ui.PackagingElementPresentation;
import consulo.util.xml.serializer.annotation.Attribute;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class ExtractedDirectoryPackagingElement extends FileOrDirectoryCopyPackagingElement<ExtractedDirectoryPackagingElement> {
  private String myPathInJar;

  public ExtractedDirectoryPackagingElement() {
    super(ExtractedDirectoryElementType.getInstance());
  }

  public ExtractedDirectoryPackagingElement(String jarPath, String pathInJar) {
    super(ExtractedDirectoryElementType.getInstance(), jarPath);
    myPathInJar = pathInJar;
    if (!StringUtil.startsWithChar(myPathInJar, '/')) {
      myPathInJar = "/" + myPathInJar;
    }
    if (!StringUtil.endsWithChar(myPathInJar, '/')) {
      myPathInJar += "/";
    }
  }

  @Override
  public PackagingElementPresentation createPresentation(@Nonnull ArtifactEditorContext context) {
    return new ExtractedDirectoryPresentation(this); 
  }

  @Override
  public String toString() {
    return "extracted:" + myFilePath + "!" + myPathInJar;
  }

  @Override
  public VirtualFile findFile() {
    final VirtualFile jarFile = super.findFile();
    if (jarFile == null) return null;

    final VirtualFile archiveRoot = ArchiveVfsUtil.getArchiveRootForLocalFile(jarFile);
    if ("/".equals(myPathInJar)) return archiveRoot;
    return archiveRoot != null ? archiveRoot.findFileByRelativePath(myPathInJar) : null;
  }

  @Override
  public void computeIncrementalCompilerInstructions(@Nonnull IncrementalCompilerInstructionCreator creator,
                                                     @Nonnull PackagingElementResolvingContext resolvingContext,
                                                     @Nonnull ArtifactIncrementalCompilerContext compilerContext,
                                                     @Nonnull ArtifactType artifactType) {
    final VirtualFile file = findFile();
    if (file != null && file.isValid() && file.isDirectory()) {
      creator.addDirectoryCopyInstructions(file);
    }
  }

  @Override
  public boolean isEqualTo(@Nonnull PackagingElement<?> element) {
    return element instanceof ExtractedDirectoryPackagingElement && super.isEqualTo(element)
           && Comparing.equal(myPathInJar, ((ExtractedDirectoryPackagingElement)element).getPathInJar());
  }

  @Override
  public ExtractedDirectoryPackagingElement getState() {
    return this;
  }

  @Override
  public void loadState(ArtifactManager artifactManager, ExtractedDirectoryPackagingElement state) {
    myFilePath = state.getFilePath();
    myPathInJar = state.getPathInJar();
  }

  @Attribute("path-in-jar")
  public String getPathInJar() {
    return myPathInJar;
  }

  public void setPathInJar(String pathInJar) {
    myPathInJar = pathInJar;
  }
}
