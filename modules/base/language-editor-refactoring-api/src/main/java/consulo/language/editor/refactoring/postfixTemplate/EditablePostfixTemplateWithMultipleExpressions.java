// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.refactoring.postfixTemplate;

import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateBuilderFactory;
import consulo.language.psi.PsiElement;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Base class for editable templates with applicable expressions conditions.
 * Template data is backed by live template.
 * It supports selecting the expression a template is applied to.
 *
 * @param <ConditionType> expression condition type
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/advanced-postfix-templates.html">Advanced Postfix Templates (IntelliJ Platform Docs)</a>
 */
public abstract class EditablePostfixTemplateWithMultipleExpressions<ConditionType extends PostfixTemplateExpressionCondition>
  extends EditablePostfixTemplate {

  
  protected final Set<? extends ConditionType> myExpressionConditions;
  protected final boolean myUseTopmostExpression;

  protected EditablePostfixTemplateWithMultipleExpressions(String templateId,
                                                           String templateName,
                                                           Template liveTemplate,
                                                           String example,
                                                           Set<? extends ConditionType> expressionConditions,
                                                           boolean useTopmostExpression,
                                                           PostfixTemplateProvider provider) {
    super(templateId, templateName, liveTemplate, example, provider);
    myExpressionConditions = expressionConditions;
    myUseTopmostExpression = useTopmostExpression;
  }

  protected EditablePostfixTemplateWithMultipleExpressions(String templateId,
                                                           String templateName,
                                                           String templateKey,
                                                           Template liveTemplate,
                                                           String example,
                                                           Set<? extends ConditionType> expressionConditions,
                                                           boolean useTopmostExpression,
                                                           PostfixTemplateProvider provider) {
    super(templateId, templateName, templateKey, liveTemplate, example, provider);
    myExpressionConditions = expressionConditions;
    myUseTopmostExpression = useTopmostExpression;
  }

  
  protected static Template createTemplate(String templateText) {
    Template template = TemplateBuilderFactory.getInstance().createRawTemplate("fakeKey", templateText, "");
    template.setToReformat(true);
    template.parseSegments();
    return template;
  }

  
  @Override
  protected PsiElement getElementToRemove(PsiElement element) {
    if (myUseTopmostExpression) {
      return getTopmostExpression(element);
    }
    return element;
  }

  
  protected abstract PsiElement getTopmostExpression(PsiElement element);

  
  public Set<? extends ConditionType> getExpressionConditions() {
    return Collections.unmodifiableSet(myExpressionConditions);
  }

  public boolean isUseTopmostExpression() {
    return myUseTopmostExpression;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    EditablePostfixTemplateWithMultipleExpressions<?> that = (EditablePostfixTemplateWithMultipleExpressions<?>)o;
    return myUseTopmostExpression == that.myUseTopmostExpression &&
      Objects.equals(myExpressionConditions, that.myExpressionConditions);
  }

  
  protected Predicate<PsiElement> getExpressionCompositeCondition() {
    return e -> {
      for (ConditionType condition : myExpressionConditions) {
        //noinspection unchecked
        if (condition.test(e)) {
          return true;
        }
      }
      return myExpressionConditions.isEmpty();
    };
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), myExpressionConditions, myUseTopmostExpression);
  }
}
