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

package consulo.language.editor.template.macro;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.completion.lookup.event.LookupAdapter;
import consulo.language.editor.completion.lookup.event.LookupEvent;
import consulo.language.editor.template.*;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

public abstract class BaseCompleteMacro extends Macro {
    private final String myName;

    protected BaseCompleteMacro(String name) {
        myName = name;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public String getPresentableName() {
        return myName + "()";
    }

    @Override
    @Nonnull
    public String getDefaultValue() {
        return "a";
    }

    @Override
    public final Result calculateResult(@Nonnull Expression[] params, ExpressionContext context) {
        return new InvokeActionResult(() -> invokeCompletion(context));
    }

    private void invokeCompletion(ExpressionContext context) {
        Project project = context.getProject();
        Editor editor = context.getEditor();

        PsiFile psiFile = PsiUtilBase.getPsiFileInEditor(editor, project);
        Runnable runnable = () -> {
            if (project.isDisposed() || editor.isDisposed() || psiFile == null || !psiFile.isValid()) {
                return;
            }

            CommandProcessor.getInstance().newCommand().project(project).run(() -> {
                invokeCompletionHandler(project, editor);
                Lookup lookup = LookupManager.getInstance(project).getActiveLookup();

                if (lookup != null) {
                    lookup.addLookupListener(new MyLookupListener(context));
                }
                else {
                    considerNextTab(editor);
                }
            });
        };

        Application.get().invokeLater(runnable);
    }

    private static void considerNextTab(Editor editor) {
        TemplateState templateState = TemplateManager.getInstance(editor.getProject()).getTemplateState(editor);
        if (templateState != null) {
            TextRange range = templateState.getCurrentVariableRange();
            if (range != null && range.getLength() > 0) {
                int caret = editor.getCaretModel().getOffset();
                if (caret == range.getEndOffset()) {
                    templateState.nextTab();
                }
                else if (caret > range.getEndOffset()) {
                    templateState.cancelTemplate();
                }
            }
        }
    }

    protected abstract void invokeCompletionHandler(Project project, Editor editor);

    private static class MyLookupListener extends LookupAdapter {
        private final ExpressionContext myContext;

        public MyLookupListener(@Nonnull ExpressionContext context) {
            myContext = context;
        }

        @Override
        public void itemSelected(LookupEvent event) {
            LookupElement item = event.getItem();
            if (item == null) {
                return;
            }

            char c = event.getCompletionChar();
            if (!LookupEvent.isSpecialCompletionChar(c)) {
                return;
            }

            for (TemplateCompletionProcessor processor : TemplateCompletionProcessor.EP_NAME.getExtensionList()) {
                if (!processor.nextTabOnItemSelected(myContext, item)) {
                    return;
                }
            }

            Project project = myContext.getProject();
            if (project == null) {
                return;
            }

            Runnable runnable = () -> CommandProcessor.getInstance().newCommand()
                .project(project)
                .inWriteAction()
                .run(() -> {
                    Editor editor = myContext.getEditor();
                    if (editor != null) {
                        considerNextTab(editor);
                    }
                });

            Application application = Application.get();
            application.invokeLater(runnable, application.getCurrentModalityState(), project.getDisposed());
        }
    }
}
