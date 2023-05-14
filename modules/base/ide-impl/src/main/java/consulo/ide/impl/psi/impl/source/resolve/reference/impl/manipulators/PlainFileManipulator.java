/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.psi.impl.source.resolve.reference.impl.manipulators;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.plain.psi.PsiPlainTextFile;
import consulo.language.psi.AbstractElementManipulator;
import consulo.language.util.IncorrectOperationException;
import jakarta.annotation.Nonnull;

/**
 * User: ik
 * Date: 09.12.2003
 * Time: 14:10:35
 */
@ExtensionImpl
public class PlainFileManipulator extends AbstractElementManipulator<PsiPlainTextFile> {
  @Override
  public PsiPlainTextFile handleContentChange(@Nonnull PsiPlainTextFile file, @Nonnull TextRange range, String newContent)
          throws IncorrectOperationException {
    final Document document = FileDocumentManager.getInstance().getDocument(file.getVirtualFile());
    document.replaceString(range.getStartOffset(), range.getEndOffset(), newContent);
    PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);

    return file;
  }

  @Nonnull
  @Override
  public Class<PsiPlainTextFile> getElementClass() {
    return PsiPlainTextFile.class;
  }
}
