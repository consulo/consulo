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

package consulo.language.editor.template;

import consulo.document.Document;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;

public class TextResult implements Result {
  private final String myText;

  public TextResult(@Nonnull String text) {
    myText = text;
  }

  @Nonnull
  public String getText() {
    return myText;
  }

  @Override
  public boolean equalsToText(String text, PsiElement context) {
    return text.equals(myText);
  }

  @Override
  public String toString() {
    return myText;
  }

  @Override
  public void handleFocused(PsiFile psiFile, Document document, int segmentStart, int segmentEnd) {
  }
}