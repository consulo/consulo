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
package consulo.ide.impl.idea.ide.actions;

import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.ide.impl.idea.codeInsight.daemon.impl.ShowIntentionsPass;
import consulo.language.editor.intention.IntentionAction;
import consulo.ide.impl.idea.codeInsight.intention.impl.ShowIntentionActionsHandler;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.application.util.function.Computable;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ApplyIntentionAction extends AnAction {

    private final IntentionAction myAction;
    private final Editor myEditor;
    private final PsiFile myFile;

    public ApplyIntentionAction(final HighlightInfoImpl.IntentionActionDescriptor descriptor, String text, Editor editor, PsiFile file) {
        this(descriptor.getAction(), text, editor, file);
    }

    public ApplyIntentionAction(final IntentionAction action, String text, Editor editor, PsiFile file) {
        super(text);
        myAction = action;
        myEditor = editor;
        myFile = file;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        ShowIntentionActionsHandler.chooseActionAndInvoke(myFile, myEditor, myAction, myAction.getText());
    }

    public String getName() {
        return ApplicationManager.getApplication().runReadAction(new Computable<String>() {
            @Override
            public String compute() {
                return myAction.getText();
            }
        });
    }

    @Nullable
    public static ApplyIntentionAction[] getAvailableIntentions(final Editor editor, final PsiFile file) {
        final ShowIntentionsPass.IntentionsInfo info = new ShowIntentionsPass.IntentionsInfo();
        ApplicationManager.getApplication().runReadAction(new Runnable() {
            @Override
            public void run() {
                ShowIntentionsPass.getActionsToShow(editor, file, info, -1);
            }
        });
        if (info.isEmpty()) {
            return null;
        }

        final List<HighlightInfoImpl.IntentionActionDescriptor> actions = new ArrayList<HighlightInfoImpl.IntentionActionDescriptor>();
        actions.addAll(info.errorFixesToShow);
        actions.addAll(info.inspectionFixesToShow);
        actions.addAll(info.intentionsToShow);

        final ApplyIntentionAction[] result = new ApplyIntentionAction[actions.size()];
        for (int i = 0; i < result.length; i++) {
            final HighlightInfoImpl.IntentionActionDescriptor descriptor = actions.get(i);
            final String actionText = ApplicationManager.getApplication().runReadAction(new Computable<String>() {
                @Override
                public String compute() {
                    return descriptor.getAction().getText();
                }
            });
            result[i] = new ApplyIntentionAction(descriptor, actionText, editor, file);
        }
        return result;
    }
}
