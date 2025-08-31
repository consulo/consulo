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
package consulo.ide.impl.idea.codeInsight;

import consulo.language.editor.annotation.HighlightSeverity;
import consulo.document.Document;
import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;

/**
 * The information about a single problem found during pre-checkin code analysis.
 *
 * @since 5.1
 * @see consulo.ide.impl.idea.openapi.vcs.CodeSmellDetector#findCodeSmells(java.util.List)
 * @see consulo.ide.impl.idea.openapi.vcs.CodeSmellDetector#showCodeSmellErrors(java.util.List)
 */
public final class CodeSmellInfo {
  @Nonnull
  private final Document myDocument;
  private final String myDescription;
  private final TextRange myTextRange;
  private final HighlightSeverity mySeverity;

  public CodeSmellInfo(@Nonnull Document document, String description, TextRange textRange, HighlightSeverity severity) {
    myDocument = document;
    myDescription = description;
    myTextRange = textRange;
    mySeverity = severity;
  }

  @Nonnull
  public Document getDocument() {
    return myDocument;
  }

  public String getDescription() {
    return myDescription;
  }

  public TextRange getTextRange(){
    return myTextRange;
  }

  public HighlightSeverity getSeverity(){
    return mySeverity;
  }

  public int getStartLine() {
    return getDocument().getLineNumber(getTextRange().getStartOffset());
  }

  public int getStartColumn() {
    return getTextRange().getStartOffset() - getDocument().getLineStartOffset(getStartLine());
  }
}
