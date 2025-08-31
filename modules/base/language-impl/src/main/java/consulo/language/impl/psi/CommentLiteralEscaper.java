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

package consulo.language.impl.psi;

import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.language.CodeDocumentationAwareCommenter;
import consulo.language.Commenter;
import consulo.language.psi.LiteralTextEscaper;

import jakarta.annotation.Nonnull;

/**
 * @author cdr
*/
public class CommentLiteralEscaper extends LiteralTextEscaper<PsiCommentImpl> {
  public CommentLiteralEscaper(PsiCommentImpl host) {
    super(host);
  }

  @Override
  public boolean decode(@Nonnull TextRange rangeInsideHost, @Nonnull StringBuilder outChars) {
    ProperTextRange.assertProperRange(rangeInsideHost);
    outChars.append(myHost.getText(), rangeInsideHost.getStartOffset(), rangeInsideHost.getEndOffset());
    return true;
  }

  @Override
  public int getOffsetInHost(int offsetInDecoded, @Nonnull TextRange rangeInsideHost) {
    int offset = offsetInDecoded + rangeInsideHost.getStartOffset();
    if (offset < rangeInsideHost.getStartOffset()) offset = rangeInsideHost.getStartOffset();
    if (offset > rangeInsideHost.getEndOffset()) offset = rangeInsideHost.getEndOffset();
    return offset;
  }

  @Override
  public boolean isOneLine() {
    Commenter commenter = Commenter.forLanguage(myHost.getLanguage());
    if (commenter instanceof CodeDocumentationAwareCommenter) {
      return myHost.getTokenType() == ((CodeDocumentationAwareCommenter) commenter).getLineCommentTokenType();
    }
    return false;
  }
}