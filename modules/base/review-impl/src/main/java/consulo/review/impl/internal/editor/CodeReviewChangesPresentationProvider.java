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
package consulo.review.impl.internal.editor;

import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.markup.EditorGutterArea;
import consulo.codeEditor.markup.FillColorLineMarkerPresentation;
import consulo.codeEditor.markup.LineMarkerPresentation;
import consulo.codeEditor.markup.LineMarkerPresentationContext;
import consulo.codeEditor.markup.LineMarkerPresentationProvider;
import consulo.review.editor.CodeReviewChangedRange;
import consulo.review.editor.CodeReviewEditorGutterActionableChangesModel;
import consulo.review.editor.CodeReviewEditorGutterChangesModel;
import consulo.review.impl.internal.CodeReviewColorUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;

import java.util.List;
import java.util.Set;

/**
 * One range a review changed, as a bar in the strip the version control marks its own changes in.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
record CodeReviewChangesPresentationProvider(CodeReviewEditorGutterChangesModel model, CodeReviewChangedRange range)
    implements LineMarkerPresentationProvider {
    @Override
    public Set<EditorGutterArea> getUsedAreas() {
        return Set.of(EditorGutterArea.RIGHT_FREE_PAINTERS);
    }

    @Override
    public List<? extends LineMarkerPresentation> buildPresentations(LineMarkerPresentationContext context) {
        return List.of(new FillColorLineMarkerPresentation(range.line1(),
            range.line2(),
            EditorGutterArea.RIGHT_FREE_PAINTERS,
            CodeReviewColorUtil.REVIEW_CHANGED_LINES_COLOR,
            range));
    }

    @Override
    public boolean canDoAction(LineMarkerPresentation presentation, InputDetails details) {
        return model instanceof CodeReviewEditorGutterActionableChangesModel
            && Application.get().getExtensionPoint(CodeReviewGutterChangesPopupProvider.class).hasAnyExtensions();
    }

    @RequiredUIAccess
    @Override
    public void doAction(Editor editor, LineMarkerPresentation presentation, InputDetails details) {
        if (!(model instanceof CodeReviewEditorGutterActionableChangesModel actionable)) {
            return;
        }
        CodeReviewGutterChangesPopupProvider provider =
            Application.get().getExtensionPoint(CodeReviewGutterChangesPopupProvider.class).findFirstSafe(it -> true);
        if (provider != null) {
            provider.showPopup(editor, actionable, range, details);
        }
    }
}
