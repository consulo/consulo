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
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.document.Document;
import consulo.language.editor.completion.CompletionContributor;
import consulo.language.editor.completion.CompletionInitializationContext;
import consulo.language.editor.completion.OffsetMap;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public abstract class FileCopyPatcher {

  /**
   * On completion, a file copy is created and this method is invoked on corresponding document. This is usually
   * done to ensure that there is some non-whitespace text at caret position, for example, to find reference at
   * that offset and ask for its {@link PsiReference#getVariants()}. In
   * {@link CompletionContributor} it will also be easier to determine which
   * variants to suggest at current position.
   *
   * Default implementation is {@link consulo.ide.impl.idea.codeInsight.completion.DummyIdentifierPatcher} which
   * inserts {@link CompletionInitializationContext#DUMMY_IDENTIFIER}
   * to the document replacing editor selection (see {@link CompletionInitializationContext#START_OFFSET} and
   * {@link CompletionInitializationContext#SELECTION_END_OFFSET}).
   *
   * @param fileCopy
   * @param document
   * @param map {@link CompletionInitializationContext#START_OFFSET} should be valid after return
   */
  public abstract void patchFileCopy(@Nonnull final PsiFile fileCopy, @Nonnull Document document, @Nonnull OffsetMap map);

}
