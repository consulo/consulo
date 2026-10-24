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

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.InspectionWidgetActionProvider;
import consulo.review.editor.CodeReviewInEditorToolbarModel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The review toolbar of an editor, shown while {@link consulo.review.editor.ReviewInEditorUtil#showReviewToolbar} keeps a
 * {@link CodeReviewInEditorToolbarModel} on it.
 *
 * @author VISTALL
 * @since 2026-10-25
 */
@ExtensionImpl
public class CodeReviewInEditorToolbarActionProvider implements InspectionWidgetActionProvider {
    @RequiredUIAccess
    @Override
    public @Nullable AnAction createAction(Editor editor) {
        return new ReviewToolbarGroup(editor);
    }

    private static final class ReviewToolbarGroup extends ActionGroup implements DumbAware {
        private final Editor myEditor;

        private @Nullable CodeReviewInEditorToolbarModel myModel;
        private AnAction[] myChildren = EMPTY_ARRAY;

        private ReviewToolbarGroup(Editor editor) {
            myEditor = editor;
        }

        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            CodeReviewInEditorToolbarModel model = myEditor.getUserData(CodeReviewInEditorToolbarModel.KEY);
            if (model != myModel) {
                myModel = model;
                if (model == null) {
                    myChildren = EMPTY_ARRAY;
                }
                else {
                    List<AnAction> children = new ArrayList<>(model.additionalActions());
                    children.add(new CodeReviewInEditorToolbarActionGroup(model));
                    children.add(AnSeparator.create());
                    myChildren = children.toArray(AnAction[]::new);
                }
            }
            return myChildren;
        }
    }
}
