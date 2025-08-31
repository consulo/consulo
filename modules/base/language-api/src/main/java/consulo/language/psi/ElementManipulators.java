/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.psi;

import consulo.document.util.TextRange;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public class ElementManipulators {
  private static final Logger LOG = Logger.getInstance(ElementManipulators.class);

  /**
   * @see #getNotNullManipulator(PsiElement)
   */
  public static <T extends PsiElement> ElementManipulator<T> getManipulator(@Nonnull T element) {
    return ElementManipulator.getManipulator(element);
  }

  public static int getOffsetInElement(PsiElement element) {
    ElementManipulator<PsiElement> manipulator = getNotNullManipulator(element);
    return manipulator.getRangeInElement(element).getStartOffset();
  }

  public static <T extends PsiElement> ElementManipulator<T> getNotNullManipulator(T element) {
    ElementManipulator<T> manipulator = getManipulator(element);
    LOG.assertTrue(manipulator != null, element.getClass().getName());
    return manipulator;
  }

  public static TextRange getValueTextRange(PsiElement element) {
    ElementManipulator<PsiElement> manipulator = getManipulator(element);
    return manipulator == null ? TextRange.from(0, element.getTextLength()) : manipulator.getRangeInElement(element);
  }

  @Nonnull
  public static String getValueText(PsiElement element) {
    TextRange valueTextRange = getValueTextRange(element);
    if (valueTextRange.isEmpty()) return "";

    String text = element.getText();
    if (valueTextRange.getEndOffset() > text.length()) {
      LOG.error("Wrong range for " + element + " text: " + text + " range " + valueTextRange);
    }

    return valueTextRange.substring(text);
  }

  public static <T extends PsiElement> T handleContentChange(T element, String text) {
    ElementManipulator<T> manipulator = getNotNullManipulator(element);
    return manipulator.handleContentChange(element, text);
  }
}
