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
package consulo.review.list.search;

import consulo.application.dumb.DumbAware;
import consulo.review.localize.ReviewLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.popup.JBPopup;

import java.util.function.Supplier;

/**
 * Shows the chooser of the value of a review list filter.
 *
 * @author VISTALL
 * @since 2026-10-26
 */
final class ReviewListSearchFilterChooseAction extends AnAction implements DumbAware {
    private final Supplier<JBPopup> myChooser;

    ReviewListSearchFilterChooseAction(Supplier<JBPopup> chooser) {
        super(ReviewLocalize.reviewListFilterChoose());
        myChooser = chooser;
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        myChooser.get().showInBestPositionFor(e.getDataContext());
    }
}
