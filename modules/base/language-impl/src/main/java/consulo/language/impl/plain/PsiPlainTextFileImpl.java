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

package consulo.language.impl.plain;

import consulo.language.file.FileViewProvider;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.plain.PlainTextFileType;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.plain.psi.PsiPlainTextFile;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiReference;
import consulo.language.psi.ReferenceProvidersRegistry;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;

public class PsiPlainTextFileImpl extends PsiFileImpl implements PsiPlainTextFile {
  private final FileType myFileType;

  public PsiPlainTextFileImpl(FileViewProvider viewProvider) {
    super(PlainTextParserDefinition.PLAIN_FILE_ELEMENT_TYPE, PlainTextParserDefinition.PLAIN_FILE_ELEMENT_TYPE, viewProvider);
    myFileType = viewProvider.getBaseLanguage() != PlainTextLanguage.INSTANCE ? PlainTextFileType.INSTANCE : viewProvider.getFileType();
  }

  @Override
  public void accept(@Nonnull PsiElementVisitor visitor){
    visitor.visitPlainTextFile(this);
  }

  @Override
  public String toString(){
    return "PsiFile(plain text):" + getName();
  }

  @Override
  @Nonnull
  public FileType getFileType() {
    return myFileType;
  }

  @Override
  @Nonnull
  public PsiReference[] getReferences() {
    return ReferenceProvidersRegistry.getReferencesFromProviders(this);
  }
}
