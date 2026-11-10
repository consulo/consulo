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
package consulo.language.editor.refactoring.impl.internal.inline;

import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.component.extension.ExtensionPoint;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.language.editor.refactoring.action.BaseRefactoringAction;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.inline.InlineActionHandler;
import consulo.language.editor.refactoring.inline.InlineHandler;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.util.concurrent.coroutine.step.Iteration;
import consulo.util.lang.ref.SimpleReference;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Jeka
 * @since 2001-11-21
 */
public class InlineRefactoringActionHandler implements RefactoringActionHandler {
    private static final Logger LOG = Logger.getInstance(InlineRefactoringActionHandler.class);
    private static final LocalizeValue REFACTORING_NAME = RefactoringLocalize.inlineTitle();

    @Override
    @RequiredUIAccess
    public void invoke(Project project, PsiElement[] elements, DataContext dataContext) {
        LOG.assertTrue(elements.length == 1);
        if (dataContext == null) {
            dataContext = DataManager.getInstance().getDataContext();
        }
        Editor editor = dataContext.getData(Editor.KEY);
        SimpleReference<Boolean> inlined = SimpleReference.create(Boolean.FALSE);
        project.getApplication().getExtensionPoint(InlineActionHandler.class).forEachBreakable(handler -> {
            if (handler.canInlineElement(elements[0])) {
                handler.inlineElement(project, editor, elements[0]);
                inlined.set(Boolean.TRUE);
                return ExtensionPoint.Flow.BREAK;
            }
            return ExtensionPoint.Flow.CONTINUE;
        });

        if (!inlined.get()) {
            invokeInliner(editor, elements[0]);
        }
    }

    @Override
    @RequiredUIAccess
    public void invoke(Project project, Editor editor, PsiFile file, DataContext dataContext) {
        editor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);

        PsiElement element = dataContext.getData(PsiElement.KEY);
        if (element == null) {
            element = BaseRefactoringAction.getElementAtCaret(editor, file);
        }
        if (element == null) {
            return;
        }
        PsiElement finalElement = element;
        SimpleReference<Boolean> inlined = SimpleReference.create(Boolean.FALSE);
        project.getApplication().getExtensionPoint(InlineActionHandler.class).forEachBreakable(handler -> {
            if (handler.canInlineElementInEditor(finalElement, editor)) {
                handler.inlineElement(project, editor, finalElement);
                inlined.set(Boolean.TRUE);
                return ExtensionPoint.Flow.BREAK;
            }
            return ExtensionPoint.Flow.CONTINUE;
        });

        if (inlined.get()) {
            return;
        }

        Coroutine.first(UIAction.<@Nullable Object, Boolean>await(ignored -> invokeInliner(editor, finalElement)))
            .then(UIAction.apply(handled -> {
                if (!handled) {
                    LocalizeValue message =
                        RefactoringLocalize.cannotPerformRefactoringWithReason(RefactoringLocalize.errorWrongCaretPositionMethodOrLocalName());
                    CommonRefactoringUtil.showErrorHint(project, editor, message, REFACTORING_NAME, null);
                }
                return null;
            }))
            .runAsync(CoroutineScope.of(project.coroutineContext()), null);
    }

    /**
     * Offers the element to the inline handlers of its language one after another, until one takes it.
     *
     * @return completes with whether a handler took the element
     */
    @RequiredUIAccess
    public static CompletableFuture<Boolean> invokeInliner(@Nullable Editor editor, PsiElement element) {
        AtomicBoolean handled = new AtomicBoolean();
        return Coroutine.first(Iteration.<InlineHandler, List<InlineHandler>>forEach(UIAction.<InlineHandler, @Nullable Void>await(handler -> {
                if (handled.get()) {
                    return CompletableFuture.completedFuture(null);
                }
                return GenericInlineHandlerImpl.invoke(element, editor, handler).thenAccept(taken -> {
                    if (taken) {
                        handled.set(true);
                    }
                });
            })))
            .then(UIAction.apply(ignored -> handled.get()))
            .runAsync(CoroutineScope.of(element.getProject().coroutineContext()), InlineHandler.forLanguage(element.getLanguage()))
            .toFuture();
    }
}
