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

package consulo.language.editor.moveUpDown;

import consulo.document.Document;
import consulo.language.psi.PsiElement;
import consulo.logging.Logger;
import org.jetbrains.annotations.NonNls;
import jakarta.annotation.Nonnull;

public class LineRange {
  private static final Logger LOG = Logger.getInstance(LineRange.class);
  public final int startLine;
  public final int endLine;

  public PsiElement firstElement;
  public PsiElement lastElement;

  public LineRange(int startLine, int endLine) {
    this.startLine = startLine;
    this.endLine = endLine;
    LOG.assertTrue(startLine >= 0, "Negative start line");
    if (startLine > endLine) {
      LOG.error("start > end: start=" + startLine+"; end="+endLine);
    }
  }
  public LineRange(@Nonnull PsiElement startElement, @Nonnull PsiElement endElement, @Nonnull Document document) {
    this(document.getLineNumber(startElement.getTextRange().getStartOffset()),
         document.getLineNumber(endElement.getTextRange().getEndOffset()) + 1);
  }

  public LineRange(@Nonnull PsiElement startElement, @Nonnull PsiElement endElement) {
    this(startElement, endElement, startElement.getContainingFile().getViewProvider().getDocument());
  }

  public LineRange(@Nonnull PsiElement element) {
    this(element, element);
  }

  @NonNls
  public String toString() {
    return "line range: ["+startLine+"-"+endLine+"]";
  }

  public boolean containsLine(int lineNumber) {
    return startLine <= lineNumber && endLine > lineNumber;
  }

  public boolean contains(LineRange range) {
    return startLine <= range.startLine && endLine >= range.endLine;
  }
}
