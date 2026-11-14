/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ai.editor.impl.internal;

import consulo.ai.AIChatContextManager;
import consulo.ai.AIChatInput;
import consulo.ai.AIChatInputFactory;
import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.codeEditor.Editor;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbar;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.review.icon.ReviewIconGroup;
import consulo.ui.Button;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.layout.VerticalLayout;

import java.util.List;
import java.util.function.Consumer;

/**
 * A comment on the selected code for the agent, written next to the code - sent to the chat at once, or added to the
 * next message, so comments on several places go together. Without a selection the comment is on the line of the caret.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ActionImpl(
    id = CommentForAIAction.ID,
    parents = {
        @ActionParentRef(@ActionRef(id = IdeActions.GROUP_EDITOR_POPUP))
    }
)
public class CommentForAIAction extends DumbAwareAction implements AnActionWithSyncUpdate {
    public static final String ID = "AI.CommentForAI";


    public CommentForAIAction() {
        super(AIEditorLocalize.actionCommentForAiText(), AIEditorLocalize.actionCommentForAiDescription(), ReviewIconGroup.comment());
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getData(Project.KEY) != null && e.getData(Editor.KEY) != null);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        Editor editor = e.getData(Editor.KEY);
        if (project == null || editor == null) {
            return;
        }
        AIEditorSelection target = AIEditorSelection.of(editor);

        Disposable disposable = Disposable.newDisposable();
        AIChatInput input = AIChatInputFactory.getInstance().create(project, AIEditorLocalize.placeholderCommentForAi(), List::of, disposable);

        VerticalLayout content = VerticalLayout.create(Space.SMALL);
        content.paddingBuilder().allSet(Space.MEDIUM).apply();
        Label title = Label.create(AIEditorLocalize.labelCommentOn(target.where()));
        title.addStyle(LabelStyle.FONT_SMALL);
        content.add(title);
        content.add(input.getComponent());

        JBPopup[] popup = new JBPopup[1];
        Consumer<String> send = text -> {
            String comment = text.strip();
            if (comment.isEmpty()) {
                return;
            }
            popup[0].cancel();
            AIChatContextManager.getInstance(project).send(target.attachment(), comment);
        };
        Button addButton = Button.create(AIEditorLocalize.buttonCommentAdd(), event -> {
            String comment = input.getText().strip();
            if (comment.isEmpty()) {
                return;
            }
            popup[0].cancel();
            // the comment goes in the attachment - the chat shows it as a chip, the agent reads it with the code
            AIChatContextManager.getInstance(project).attach(target.commented(comment));
        });
        HorizontalLayout buttons = HorizontalLayout.create(Space.SMALL);
        buttons.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
        buttons.add(addButton);
        // the box sends with its own button, as the chat does - adding to the next message sits next to it
        input.setSuffixComponent(buttons);

        // Enter and the send button of the box send, as in the chat
        input.addSubmitListener(send);

        popup[0] = JBPopupFactory.getInstance()
            .createComponentPopupBuilder(content, input.getComponent())
            .setRequestFocus(true)
            .setResizable(true)
            .setMovable(true)
            .createPopup();
        Disposer.register(popup[0], disposable);
        // from the selection toolbar the box hangs under its button, the toolbar staying - else next to the selection
        CodeFloatingToolbar toolbar = CodeFloatingToolbar.getToolbar(editor);
        if (toolbar == null || !toolbar.isShown() || !toolbar.showPopupUnderneath(this, popup[0])) {
            editor.showPopupInBestPositionFor(popup[0]);
        }
        input.focus();
    }
}
