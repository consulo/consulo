// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.impl.internal.list.error;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.review.util.ReviewUIUtil;
import consulo.review.list.error.ErrorStatusAlignment;
import consulo.review.list.error.ErrorStatusPanelFactory;
import consulo.review.list.error.ErrorStatusPresenter;
import consulo.review.list.error.HtmlErrorStatusPresenter;
import consulo.review.list.error.TextErrorStatusPresenter;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.HtmlLabel;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.concurrent.coroutine.ObservableValue;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

@Singleton
@ServiceImpl
public class ErrorStatusPanelFactoryImpl implements ErrorStatusPanelFactory {
    private static final String PLACE = "CodeReviewErrorStatus";

    @RequiredUIAccess
    @Override
    public <T> Component create(T error, ErrorStatusPresenter<T> errorPresenter, ErrorStatusAlignment alignment) {
        VerticalLayout panel = createPanel(alignment);
        update(panel, alignment, error, errorPresenter);
        return panel;
    }

    @RequiredUIAccess
    @Override
    public <T> Component create(Disposable parentDisposable,
                                ObservableValue<@Nullable T> errorState,
                                ErrorStatusPresenter<T> errorPresenter,
                                ErrorStatusAlignment alignment) {
        VerticalLayout panel = createPanel(alignment);
        ReviewUIUtil.bind(parentDisposable, errorState, error -> update(panel, alignment, error, errorPresenter));
        return panel;
    }

    @RequiredUIAccess
    private static VerticalLayout createPanel(ErrorStatusAlignment alignment) {
        return VerticalLayout.create(Space.SMALL, alignment == ErrorStatusAlignment.CENTER ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT);
    }

    @RequiredUIAccess
    private <T> void update(VerticalLayout panel, ErrorStatusAlignment alignment, @Nullable T error, ErrorStatusPresenter<T> errorPresenter) {
        panel.removeAll();
        if (error == null) {
            panel.setVisible(false);
            return;
        }

        AnAction errorAction = errorPresenter.getErrorAction(error);
        if (errorPresenter instanceof HtmlErrorStatusPresenter<T> html) {
            HtmlLabel body = HtmlLabel.create(LocalizeValue.of(html.getHTMLBody(error)));
            body.setForegroundColor(ComponentColors.ERROR_FOREGROUND);
            body.addHyperlinkListener(event -> {
                if (!ErrorStatusPresenter.ERROR_ACTION_HREF.equals(event.getDescription())) {
                    Platform.current().openInBrowser(event.getDescription());
                }
                else if (errorAction != null) {
                    ReviewUIUtil.performAction(errorAction, body, PLACE);
                }
            });
            panel.add(body);
        }
        else if (errorPresenter instanceof TextErrorStatusPresenter<T> text) {
            Label title = Label.create(text.getErrorTitle(error));
            title.setForegroundColor(ComponentColors.ERROR_FOREGROUND);
            panel.add(title);
            LocalizeValue description = text.getErrorDescription(error);
            if (description.isNotEmpty()) {
                Label descriptionLabel = Label.create(description);
                descriptionLabel.setForegroundColor(ComponentColors.ERROR_FOREGROUND);
                panel.add(descriptionLabel);
            }
        }
        if (errorAction != null) {
            Button button = Button.create(errorAction.getTemplatePresentation().getTextValue());
            button.addClickListener(event -> ReviewUIUtil.performAction(errorAction, button, PLACE));
            panel.add(button);
        }
        panel.setVisible(true);
    }
}
