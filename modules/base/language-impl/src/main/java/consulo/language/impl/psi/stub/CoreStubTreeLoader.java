/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.impl.psi.stub;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.Language;
import consulo.language.ast.IFileElementType;
import consulo.language.file.LanguageFileType;
import consulo.language.parser.LanguageParserDefinitions;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.PsiFile;
import consulo.language.psi.stub.*;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * @author yole
 */
public class CoreStubTreeLoader extends StubTreeLoader {
  @Override
  public ObjectStubTree readOrBuild(Project project, VirtualFile vFile, @Nullable PsiFile psiFile) {
    if (!canHaveStub(vFile)) {
      return null;
    }

    try {
      final FileContentImpl fc = new FileContentImpl(vFile, vFile.contentsToByteArray());
      fc.setProject(project);
      final Stub element = StubTreeBuilder.buildStubTree(fc);
      if (element instanceof PsiFileStub) {
        return new StubTree((PsiFileStub)element);
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    return null;
  }

  @RequiredReadAction
  @Override
  public ObjectStubTree readFromVFile(Project project, VirtualFile vFile) {
    return null;
  }

  @Override
  public void rebuildStubTree(VirtualFile virtualFile) {
  }

  @Override
  public boolean canHaveStub(VirtualFile file) {
    final FileType fileType = file.getFileType();
    if (fileType instanceof LanguageFileType) {
      Language l = ((LanguageFileType)fileType).getLanguage();
      ParserDefinition parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(l);
      if (parserDefinition == null) return false;
      final IFileElementType elementType = parserDefinition.getFileNodeType();
      return elementType instanceof IStubFileElementType && ((IStubFileElementType)elementType).shouldBuildStubFor(file);
    }
    else if (fileType.isBinary()) {
      final BinaryFileStubBuilder builder = BinaryFileStubBuilders.INSTANCE.forFileType(fileType);
      return builder != null && builder.acceptsFile(file);
    }
    return false;
  }

}
