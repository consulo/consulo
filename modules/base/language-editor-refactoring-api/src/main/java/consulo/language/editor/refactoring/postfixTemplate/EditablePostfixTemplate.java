// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.refactoring.postfixTemplate;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.postfixTemplate.PostfixTemplate;
import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import consulo.language.editor.postfixTemplate.PostfixTemplatesUtils;
import consulo.language.editor.refactoring.IntroduceTargetChooser;
import consulo.language.editor.refactoring.unwrap.ScopeHighlighter;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateManager;
import consulo.language.editor.template.TextExpression;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Base class for editable templates.
 * Template data is backed by live template.
 * It supports selecting the expression a template is applied to.
 *
 * @see EditablePostfixTemplateWithMultipleExpressions
 * @see <a href="https://plugins.jetbrains.com/docs/intellij/advanced-postfix-templates.html">Advanced Postfix Templates (IntelliJ Platform Docs)</a>
 */
public abstract class EditablePostfixTemplate extends PostfixTemplate {
    @Nonnull
    private final Template myLiveTemplate;

    public EditablePostfixTemplate(
        @Nonnull String templateId,
        @Nonnull String templateName,
        @Nonnull Template liveTemplate,
        @Nonnull String example,
        @Nonnull PostfixTemplateProvider provider
    ) {
        this(templateId, templateName, "." + templateName, liveTemplate, example, provider);
    }

    public EditablePostfixTemplate(
        @Nonnull String templateId,
        @Nonnull String templateName,
        @Nonnull String templateKey,
        @Nonnull Template liveTemplate,
        @Nonnull String example,
        @Nonnull PostfixTemplateProvider provider
    ) {
        super(templateId, templateName, templateKey, example, provider);
        assert StringUtil.isNotEmpty(liveTemplate.getKey());
        myLiveTemplate = liveTemplate;
    }

    @Nonnull
    public Template getLiveTemplate() {
        return myLiveTemplate;
    }

    @Override
    @RequiredUIAccess
    public final void expand(@Nonnull PsiElement context, @Nonnull final Editor editor) {
        List<PsiElement> expressions = getExpressions(context, editor.getDocument(), editor.getCaretModel().getOffset());

        if (expressions.isEmpty()) {
            PostfixTemplatesUtils.showErrorHint(context.getProject(), editor);
            return;
        }

        if (expressions.size() == 1) {
            prepareAndExpandForChooseExpression(expressions.get(0), editor);
            return;
        }

        if (Application.get().isUnitTestMode()) {
            PsiElement item = ContainerUtil.getFirstItem(expressions);
            assert item != null;
            prepareAndExpandForChooseExpression(item, editor);
            return;
        }

        IntroduceTargetChooser.showChooser(
            editor, expressions,
            e -> prepareAndExpandForChooseExpression(e, editor),
            getElementRenderer(),
            CodeInsightLocalize.dialogTitleExpressions().get(),
            0,
            ScopeHighlighter.NATURAL_RANGER
        );
    }

    @Override
    public boolean equals(Object o) {
        return this == o
            || o instanceof EditablePostfixTemplate template
            && super.equals(o)
            && Objects.equals(myLiveTemplate, template.myLiveTemplate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), myLiveTemplate);
    }

    protected abstract List<PsiElement> getExpressions(@Nonnull PsiElement context, @Nonnull Document document, int offset);

    @Override
    public boolean isApplicable(@Nonnull PsiElement context, @Nonnull Document copyDocument, int newOffset) {
        return !getExpressions(context, copyDocument, newOffset).isEmpty();
    }

    protected void addTemplateVariables(@Nonnull PsiElement element, @Nonnull Template template) {
    }

    /**
     * @param element element to which the template was applied
     * @return an element to remove before inserting the template
     */
    @Nonnull
    protected PsiElement getElementToRemove(@Nonnull PsiElement element) {
        return element;
    }

    /**
     * Default implementation delegates to {@link #getElementToRemove(PsiElement)} and takes the text range of the resulting element.
     * Override it if it's desired to remove only a part of {@code PsiElement}'s range.
     *
     * @param element element to which the template was applied
     * @return a range to remove before inserting the template
     */
    @Nonnull
    @RequiredReadAction
    protected TextRange getRangeToRemove(@Nonnull PsiElement element) {
        return getElementToRemove(element).getTextRange();
    }

    @Nonnull
    protected Function<PsiElement, String> getElementRenderer() {
        return element -> element.getText();
    }

    @Nonnull
    @Override
    public PostfixTemplateProvider getProvider() {
        PostfixTemplateProvider provider = super.getProvider();
        assert provider != null;
        return provider;
    }

    @RequiredUIAccess
    private void prepareAndExpandForChooseExpression(@Nonnull PsiElement element, @Nonnull Editor editor) {
        CommandProcessor.getInstance().newCommand(() -> expandForChooseExpression(element, editor))
            .withProject(element.getProject())
            .withName(CodeInsightLocalize.commandExpandPostfixTemplate())
            .withGroupId(PostfixTemplate.POSTFIX_TEMPLATE_CUSTOM_TEMPLATE_ID)
            .executeInWriteAction();
    }

    @RequiredReadAction
    private void expandForChooseExpression(@Nonnull PsiElement element, @Nonnull Editor editor) {
        Project project = element.getProject();
        Document document = editor.getDocument();
        TextRange range = getRangeToRemove(element);
        document.deleteString(range.getStartOffset(), range.getEndOffset());
        TemplateManager manager = TemplateManager.getInstance(project);

        Template template = myLiveTemplate.copy();
        template.addVariable("EXPR", new TextExpression(element.getText()), false);
        addTemplateVariables(element, template);
        manager.startTemplate(editor, template);
    }
}
