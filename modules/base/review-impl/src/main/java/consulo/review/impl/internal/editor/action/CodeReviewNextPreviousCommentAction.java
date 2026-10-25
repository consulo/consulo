// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.impl.internal.editor.action;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorKeys;
import consulo.dataContext.DataContext;
import consulo.diff.DiffDataKeys;
import consulo.localize.LocalizeValue;
import consulo.review.editor.CodeReviewEditorKeys;
import consulo.review.editor.CodeReviewNavigableEditorViewModel;
import consulo.review.timeline.thread.CodeReviewTrackableItemViewModel;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPromoter;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.Coroutine;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

public abstract class CodeReviewNextPreviousCommentAction extends DumbAwareAction implements AnActionWithAsyncUpdate, ActionPromoter {
    private record EditorModel(@Nullable Editor editor, @Nullable CodeReviewNavigableEditorViewModel model) {
    }

    private final BiPredicate<CodeReviewNavigableEditorViewModel, String> myCanGotoThreadComment;
    private final BiPredicate<CodeReviewNavigableEditorViewModel, Integer> myCanGotoLineComment;
    private final BiConsumer<CodeReviewNavigableEditorViewModel, String> myGotoThreadComment;
    private final BiConsumer<CodeReviewNavigableEditorViewModel, Integer> myGotoLineComment;

    protected CodeReviewNextPreviousCommentAction(LocalizeValue text,
                                                  LocalizeValue description,
                                                  Image icon,
                                                  BiPredicate<CodeReviewNavigableEditorViewModel, String> canGotoThreadComment,
                                                  BiPredicate<CodeReviewNavigableEditorViewModel, Integer> canGotoLineComment,
                                                  BiConsumer<CodeReviewNavigableEditorViewModel, String> gotoThreadComment,
                                                  BiConsumer<CodeReviewNavigableEditorViewModel, Integer> gotoLineComment) {
        super(text, description, icon);
        myCanGotoThreadComment = canGotoThreadComment;
        myCanGotoLineComment = canGotoLineComment;
        myGotoThreadComment = gotoThreadComment;
        myGotoLineComment = gotoLineComment;
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return Coroutine.first(UIAction.<Object, Boolean>apply(o -> {
            EditorModel editorModel = getEditorModel(e);
            Editor editor = editorModel.editor();
            CodeReviewNavigableEditorViewModel model = editorModel.model();
            if (editor == null || model == null || !model.canNavigate()) {
                e.getPresentation().setEnabledAndVisible(false);
                return false;
            }

            String focused = findFocusedThreadId(e);
            boolean enabled;
            if (focused != null) {
                enabled = myCanGotoThreadComment.test(model, focused);
            }
            else {
                // zero-index
                int editorLine = editor.getCaretModel().getLogicalPosition().line;
                enabled = myCanGotoLineComment.test(model, editorLine);
            }
            e.getPresentation().setEnabled(enabled);
            return enabled;
        }));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        EditorModel editorModel = getEditorModel(e);
        Editor editor = editorModel.editor();
        CodeReviewNavigableEditorViewModel model = editorModel.model();
        if (editor == null || model == null || !model.canNavigate()) {
            return;
        }

        String focused = findFocusedThreadId(e);
        if (focused != null) {
            myGotoThreadComment.accept(model, focused);
        }
        else {
            // zero-index
            int editorLine = editor.getCaretModel().getLogicalPosition().line;
            myGotoLineComment.accept(model, editorLine);
        }
    }

    private static EditorModel getEditorModel(AnActionEvent e) {
        Editor currentEditor = e.getData(DiffDataKeys.CURRENT_EDITOR);
        if (currentEditor == null) {
            currentEditor = e.getData(EditorKeys.EDITOR_EVEN_IF_INACTIVE);
        }
        if (currentEditor != null) {
            CodeReviewNavigableEditorViewModel navigableModel = currentEditor.getUserData(CodeReviewNavigableEditorViewModel.KEY);
            if (navigableModel != null) {
                return new EditorModel(currentEditor, navigableModel);
            }
        }

        Editor navigableEditor = e.getData(CodeReviewEditorKeys.NAVIGABLE_EDITOR_KEY);
        return new EditorModel(navigableEditor, navigableEditor == null ? null : navigableEditor.getUserData(CodeReviewNavigableEditorViewModel.KEY));
    }

    private static @Nullable String findFocusedThreadId(AnActionEvent e) {
        CodeReviewTrackableItemViewModel item = e.getData(CodeReviewTrackableItemViewModel.TRACKABLE_ITEM_KEY);
        return item == null ? null : item.getTrackingId();
    }

    @Override
    public @Nullable List<AnAction> suppress(List<AnAction> actions, DataContext context) {
        if (context.getData(CodeReviewTrackableItemViewModel.TRACKABLE_ITEM_KEY) != null) {
            List<AnAction> others = new ArrayList<>(actions);
            others.removeIf(action -> action == this);
            return others;
        }
        return null;
    }
}
