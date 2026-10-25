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
package consulo.review.impl.internal.reactions;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.review.icon.ReviewIconGroup;
import consulo.review.localize.ReviewLocalize;
import consulo.review.reactions.CodeReviewReactionComponentFactory;
import consulo.review.reactions.CodeReviewReactionPillPresentation;
import consulo.review.reactions.CodeReviewReactionsUIUtil;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.ToggleButton;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.ObservableValue;
import jakarta.inject.Singleton;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@Singleton
@ServiceImpl
public class CodeReviewReactionComponentFactoryImpl implements CodeReviewReactionComponentFactory {
    @RequiredUIAccess
    @Override
    public Component createReactionButton(Disposable parentDisposable,
                                          ObservableValue<CodeReviewReactionPillPresentation> presentation,
                                          Runnable toggle) {
        ToggleButton button = ToggleButton.create(LocalizeValue.empty());
        button.addStyle(ButtonStyle.PILL);

        AtomicReference<CodeReviewReactionPillPresentation> last = new AtomicReference<>();
        UIAccess uiAccess = UIAccess.current();
        Consumer<CodeReviewReactionPillPresentation> apply = value -> {
            if (Objects.equals(last.getAndSet(value), value)) {
                return;
            }
            button.setIcon(value.getIcon(CodeReviewReactionsUIUtil.ICON_SIZE));
            button.setText(LocalizeValue.of(String.valueOf(value.getReactors().size())));
            button.setValue(value.isOwnReaction(), false);
            button.setToolTipText(CodeReviewReactionsUIUtil.createTooltipText(value.getReactors(), value.getReactionName()));
        };
        apply.accept(presentation.get());
        Disposer.register(parentDisposable, presentation.addListener(value -> uiAccess.give(() -> apply.accept(value)))::run);

        button.addClickListener(event -> {
            button.setValue(Objects.requireNonNull(last.get()).isOwnReaction(), false);
            toggle.run();
        });
        return button;
    }

    @RequiredUIAccess
    @Override
    public Component createNewReactionButton(Consumer<Component> showPicker) {
        Button button = Button.create(LocalizeValue.empty());
        button.addStyle(ButtonStyle.PILL);
        button.setIcon(ReviewIconGroup.addemoji());
        button.setToolTipText(ReviewLocalize.reviewCommentsReactionAddTooltip());
        button.addClickListener(event -> showPicker.accept(button));
        return button;
    }

    @RequiredUIAccess
    @Override
    public Component createPickReactionButton(Image emojiIcon, Runnable pick) {
        Button button = Button.create(LocalizeValue.empty());
        button.addStyle(ButtonStyle.PILL);
        button.setIcon(emojiIcon);
        button.addClickListener(event -> pick.run());
        return button;
    }
}
