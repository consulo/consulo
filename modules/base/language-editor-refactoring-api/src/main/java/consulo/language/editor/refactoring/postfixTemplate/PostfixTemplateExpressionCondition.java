// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.refactoring.postfixTemplate;

import consulo.language.psi.PsiElement;
import org.jdom.Element;

import java.util.function.Predicate;

/**
 * Editable postfix template expression condition used to determine contexts that a postfix template can be applied in.
 *
 * @param <T> the supported PSI element type
 * @see EditablePostfixTemplateWithMultipleExpressions
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/advanced-postfix-templates.html">Advanced Postfix Templates (IntelliJ Platform Docs)</a>
 */
public interface PostfixTemplateExpressionCondition<T extends PsiElement> extends Predicate<T> {
  String ID_ATTR = "id";

  /**
   * @return presentable name for postfix editor dialog
   */
  String getPresentableName();

  /**
   * @return ID for serialization
   */
  String getId();

  boolean equals(Object o);

  int hashCode();

  default void serializeTo(Element element) {
    element.setAttribute(ID_ATTR, getId());
  }

  /**
   * @param t PSI element to check
   * @return {@code true} if an expression context determined by a given element is applicable for evaluated postfix template,
   * {@code false} otherwise
   */
  @Override
  boolean test(T t);
}
