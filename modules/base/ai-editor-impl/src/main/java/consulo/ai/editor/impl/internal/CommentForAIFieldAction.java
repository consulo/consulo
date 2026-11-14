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
import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.codeEditor.Editor;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbar;
import consulo.codeEditor.toolbar.floating.CodeFloatingToolbarActions;
import consulo.project.Project;
import consulo.review.icon.ReviewIconGroup;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.TextBoxStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.action.CustomUIComponentAction;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.util.dataholder.Key;

/**
 * A box in the selection toolbar to write the agent about the selected code - Enter sends the comment to the chat,
 * the code going with it. The box has no border, it reads as a part of the toolbar.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
@ActionImpl(id = CommentForAIFieldAction.ID, parents = @ActionParentRef(@ActionRef(id = CodeFloatingToolbarActions.GROUP)))
public class CommentForAIFieldAction extends DumbAwareAction implements CustomUIComponentAction, AnActionWithSyncUpdate {
    public static final String ID = "AI.CommentForAIField";

    /**
     * The editor of the toolbar - the box finds the selection through it when Enter is pressed.
     */
    private static final Key<Editor> EDITOR = Key.create("AI.CommentForAIField.editor");
    /**
     * The box of the toolbar of an editor - the comment action of the editor menu types in it.
     */
    static final Key<TextBox> FIELD = Key.create("AI.CommentForAIField.box");
    /**
     * The box of a presentation - the update hands it to the editor once the editor is known.
     */
    private static final Key<TextBox> BOX = Key.create("AI.CommentForAIField.presentationBox");

    private static final int COLUMNS = 32;

    public CommentForAIFieldAction() {
        super(AIEditorLocalize.actionCommentForAiText(), AIEditorLocalize.actionCommentForAiDescription(), ReviewIconGroup.comment());
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        e.getPresentation().setEnabledAndVisible(e.getData(Project.KEY) != null && editor != null);
        e.getPresentation().putClientProperty(EDITOR, editor);
        TextBox box = e.getPresentation().getClientProperty(BOX);
        if (editor != null && box != null) {
            editor.putUserData(FIELD, box);
        }
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
    }

    @RequiredUIAccess
    @Override
    public Component createCustomComponent(Presentation presentation, String place) {
        TextBox box = TextBox.create();
        presentation.putClientProperty(BOX, box);
        box.addDetachListener(event -> {
            Editor editor = presentation.getClientProperty(EDITOR);
            if (editor != null && editor.getUserData(FIELD) == box) {
                editor.putUserData(FIELD, null);
            }
        });
        box.addStyle(TextBoxStyle.INPLACE);
        box.setPlaceholder(AIEditorLocalize.placeholderCommentForAi());
        box.setVisibleLength(COLUMNS);
        box.addKeyPressedListener(event -> {
            if (!(event.getInputDetails() instanceof KeyboardInputDetails details)
                || details.getKeyCode() != KeyCode.ENTER
                || !details.getModifiers().isEmpty()) {
                return;
            }
            String comment = box.getValue();
            Editor editor = presentation.getClientProperty(EDITOR);
            Project project = editor == null ? null : editor.getProject();
            if (comment == null || comment.isBlank() || editor == null || project == null) {
                return;
            }
            AIEditorSelection target = AIEditorSelection.of(editor);
            box.setValue("");
            CodeFloatingToolbar toolbar = CodeFloatingToolbar.getToolbar(editor);
            if (toolbar != null) {
                toolbar.scheduleHide();
            }
            AIChatContextManager.getInstance(project).send(target.attachment(), comment.strip());
        });
        return box;
    }
}
