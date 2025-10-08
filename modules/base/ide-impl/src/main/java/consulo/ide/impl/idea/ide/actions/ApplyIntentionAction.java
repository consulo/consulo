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

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.daemon.impl.ShowIntentionsPass;
import consulo.ide.impl.idea.codeInsight.intention.impl.ShowIntentionActionsHandler;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.internal.intention.IntentionActionDescriptor;
import consulo.language.editor.internal.intention.IntentionsInfo;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ApplyIntentionAction extends AnAction {
    private final IntentionAction myAction;
    private final Editor myEditor;
    private final PsiFile myFile;

    public ApplyIntentionAction(IntentionActionDescriptor descriptor, LocalizeValue text, Editor editor, PsiFile file) {
        this(descriptor.getAction(), text, editor, file);
    }

    public ApplyIntentionAction(IntentionAction action, LocalizeValue text, Editor editor, PsiFile file) {
        super(text);
        myAction = action;
        myEditor = editor;
        myFile = file;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        ShowIntentionActionsHandler.chooseActionAndInvoke(myFile, myEditor, myAction, myAction.getText().get());
    }

    public LocalizeValue getName() {
        return Application.get().runReadAction((Supplier<LocalizeValue>)myAction::getText);
    }

    @Nullable
    public static ApplyIntentionAction[] getAvailableIntentions(Editor editor, PsiFile file) {
        IntentionsInfo info = new IntentionsInfo();
        Application.get().runReadAction(() -> ShowIntentionsPass.getActionsToShow(editor, file, info, -1));
        if (info.isEmpty()) {
            return null;
        }

        List<IntentionActionDescriptor> actions = new ArrayList<>();
        actions.addAll(info.errorFixesToShow);
        actions.addAll(info.inspectionFixesToShow);
        actions.addAll(info.intentionsToShow);

        ApplyIntentionAction[] result = new ApplyIntentionAction[actions.size()];
        for (int i = 0; i < result.length; i++) {
            IntentionActionDescriptor descriptor = actions.get(i);
            LocalizeValue actionText = Application.get().runReadAction((Supplier<LocalizeValue>)() -> descriptor.getAction().getText());
            result[i] = new ApplyIntentionAction(descriptor, actionText, editor, file);
        }
        return result;
    }
}
