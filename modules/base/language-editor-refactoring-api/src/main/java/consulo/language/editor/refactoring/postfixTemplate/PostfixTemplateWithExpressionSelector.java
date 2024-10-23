// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.refactoring.postfixTemplate;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.postfixTemplate.PostfixTemplate;
import consulo.language.editor.postfixTemplate.PostfixTemplateProvider;
import consulo.language.editor.postfixTemplate.PostfixTemplatesUtils;
import consulo.language.editor.refactoring.IntroduceTargetChooser;
import consulo.language.editor.refactoring.unwrap.ScopeHighlighter;
import consulo.language.psi.PsiElement;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import java.util.List;

public abstract class PostfixTemplateWithExpressionSelector extends PostfixTemplate {
    @Nonnull
    private final PostfixTemplateExpressionSelector mySelector;

    /**
     * @deprecated use {@link #PostfixTemplateWithExpressionSelector(String, String, String, String, PostfixTemplateExpressionSelector, PostfixTemplateProvider)}
     */
    @Deprecated(forRemoval = true)
    protected PostfixTemplateWithExpressionSelector(
        @Nonnull String name,
        @Nonnull String key,
        @Nonnull String example,
        @Nonnull PostfixTemplateExpressionSelector selector
    ) {
        this(null, name, key, example, selector, null);
    }

    /**
     * @deprecated use {@link #PostfixTemplateWithExpressionSelector(String, String, String, PostfixTemplateExpressionSelector, PostfixTemplateProvider)}
     */
    @Deprecated(forRemoval = true)
    protected PostfixTemplateWithExpressionSelector(
        @Nonnull String name,
        @Nonnull String example,
        @Nonnull PostfixTemplateExpressionSelector selector
    ) {
        this(null, name, example, selector, null);
    }

    protected PostfixTemplateWithExpressionSelector(
        @Nullable @NonNls String id,
        @Nonnull String name,
        @Nonnull String example,
        @Nonnull PostfixTemplateExpressionSelector selector,
        @Nullable PostfixTemplateProvider provider
    ) {
        super(id, name, example, provider);
        mySelector = selector;
    }

    protected PostfixTemplateWithExpressionSelector(
        @Nullable @NonNls String id,
        @Nonnull String name,
        @Nonnull String key,
        @Nonnull String example,
        @Nonnull PostfixTemplateExpressionSelector selector,
        @Nullable PostfixTemplateProvider provider
    ) {
        super(id, name, key, example, provider);
        mySelector = selector;
    }

    @Override
    public boolean isApplicable(@Nonnull PsiElement context, @Nonnull Document copyDocument, int newOffset) {
        return mySelector.hasExpression(context, copyDocument, newOffset);
    }

    @Override
    public final void expand(@Nonnull PsiElement context, @Nonnull final Editor editor) {
        List<PsiElement> expressions = mySelector.getExpressions(
            context,
            editor.getDocument(),
            editor.getCaretModel().getOffset()
        );

        if (expressions.isEmpty()) {
            PostfixTemplatesUtils.showErrorHint(context.getProject(), editor);
            return;
        }

        if (expressions.size() == 1) {
            prepareAndExpandForChooseExpression(expressions.get(0), editor);
            return;
        }

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            PsiElement item = ContainerUtil.getFirstItem(expressions);
            assert item != null;
            prepareAndExpandForChooseExpression(item, editor);
            return;
        }

        IntroduceTargetChooser.showChooser(
            editor, expressions, e -> prepareAndExpandForChooseExpression(e, editor),
            mySelector.getRenderer(),
            CodeInsightBundle.message("dialog.title.expressions"), 0, ScopeHighlighter.NATURAL_RANGER
        );
    }

    @RequiredUIAccess
    protected void prepareAndExpandForChooseExpression(@Nonnull PsiElement expression, @Nonnull Editor editor) {
        ApplicationManager.getApplication().runWriteAction(() -> CommandProcessor.getInstance().executeCommand(
            expression.getProject(),
            () -> expandForChooseExpression(
                expression,
                editor
            ),
            CodeInsightBundle.message("command.expand.postfix.template"),
            PostfixTemplate.POSTFIX_TEMPLATE_CUSTOM_TEMPLATE_ID
        ));
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    protected abstract void expandForChooseExpression(@Nonnull PsiElement expression, @Nonnull Editor editor);
}
