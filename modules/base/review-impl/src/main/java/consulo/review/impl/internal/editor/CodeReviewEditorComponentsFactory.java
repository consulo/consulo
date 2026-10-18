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

import consulo.disposer.Disposable;
import consulo.review.editor.CodeReviewNewCommentInlayModel;
import consulo.review.impl.internal.CodeReviewColorUtil;
import consulo.review.impl.internal.comment.CodeReviewCommentTextFieldFactory;
import consulo.review.impl.internal.timeline.CodeReviewThreadComponentFactory;
import consulo.review.localize.ReviewLocalize;
import consulo.review.timeline.thread.CodeReviewThreadViewModel;
import consulo.ui.Card;
import consulo.ui.CardStyle;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;

/**
 * The components of review inlays: a thread or a new comment in a rounded frame.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public final class CodeReviewEditorComponentsFactory {
    private CodeReviewEditorComponentsFactory() {
    }

    @RequiredUIAccess
    public static Component createThreadIn(Disposable parentDisposable, CodeReviewThreadViewModel thread) {
        return createEditorInlayPanel(CodeReviewThreadComponentFactory.create(parentDisposable, thread, true));
    }

    @RequiredUIAccess
    public static Component createNewCommentIn(Disposable parentDisposable, CodeReviewNewCommentInlayModel inlay) {
        Hyperlink cancel = Hyperlink.create(ReviewLocalize.reviewCommentCancel(), event -> inlay.cancel());
        Component input = CodeReviewCommentTextFieldFactory.create(parentDisposable,
            inlay.getEditor(),
            ReviewLocalize.reviewCommentPlaceholder(),
            ReviewLocalize.reviewCommentSubmit(),
            cancel);
        input.setBackgroundColor(CodeReviewColorUtil.EDITOR_BACKGROUND);
        return createEditorInlayPanel(input);
    }

    @RequiredUIAccess
    public static Component createEditorInlayPanel(Component content) {
        Card card = Card.create();
        card.addStyle(CardStyle.OUTLINED);
        card.addStyle(CardStyle.COMPACT);
        card.setBackgroundColor(CodeReviewColorUtil.EDITOR_BACKGROUND);
        card.add(content);

        VerticalLayout margin = VerticalLayout.create(Space.NONE).add(card);
        margin.paddingBuilder().leftSet(Space.MEDIUM).verticalSet(Space.SMALL).apply();
        margin.setBackgroundColor(CodeReviewColorUtil.EDITOR_BACKGROUND);
        return margin;
    }
}
