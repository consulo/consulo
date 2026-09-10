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
package consulo.ui.ex.impl.internal;

import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.ImageBox;
import consulo.ui.Label;
import consulo.ui.MessageTextFormat;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.impl.BaseMessageBoxBuilder;
import consulo.ui.impl.MessagePresentation;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.TextWithMnemonic;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A message box assembled from the general components, for the frontends which have no message box
 * of their own and as the fallback for those whose own one cannot express a given request.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public class UnifiedMessageBoxBuilderImpl<V> extends BaseMessageBoxBuilder<V> {
    private static final Space ICON_GAP = Space.X_LARGE;
    private static final Space BUTTON_GAP = Space.MEDIUM;

    @Override
    @RequiredUIAccess
    public CompletableFuture<V> showAsync(@Nullable Window owner) {
        prepare();

        V remembered = rememberedValue();
        if (remembered != null) {
            return CompletableFuture.completedFuture(remembered);
        }

        Window window = Window.create(MessagePresentation.title(myTitle).get(), WindowOptions.builder().owner(owner).build());

        CheckBox rememberBox = myRemember == null || !myRemember.isVisible()
            ? null
            : CheckBox.create(myRemember.getMessageText(), myRemember.isRememberByDefault());

        // the button which was pressed is recorded apart from the value it stands for, so that a
        // button is free to answer null without that reading as a dismissal
        AtomicReference<ButtonImpl<V>> pressed = new AtomicReference<>();

        window.setContent(buildContent(window, pressed, rememberBox));

        CompletableFuture<V> result = new CompletableFuture<>();

        window.addCloseListener(event -> {
            ButtonImpl<V> button = pressed.get();
            V value = button != null ? button.myValue.get() : exitValueOrNull();

            boolean checked = rememberBox == null
                ? myRemember != null && myRemember.isRememberByDefault()
                : Boolean.TRUE.equals(rememberBox.getValue());

            storeRemembered(button, checked, value);

            result.complete(value);

            runHelpIfNeeded(button);
        });

        // resolved here, on the ui thread - a cancel arrives on whichever thread asked for it
        UIAccess uiAccess = UIAccess.current();
        result.whenComplete((value, error) -> {
            if (result.isCancelled()) {
                uiAccess.give(window::close);
            }
        });

        window.show();

        return result;
    }

    @RequiredUIAccess
    private Component buildContent(Window window, AtomicReference<ButtonImpl<V>> pressed, @Nullable CheckBox rememberBox) {
        DockLayout root = DockLayout.create();

        root.center(buildMessage());
        root.bottom(buildBottom(window, pressed, rememberBox));

        return root;
    }

    @RequiredUIAccess
    private Component buildMessage() {
        HorizontalLayout message = HorizontalLayout.create(ICON_GAP);

        Image icon = myIcon != null ? myIcon : MessagePresentation.icon(mySeverity);
        if (icon != null) {
            message.add(ImageBox.create(icon));
        }

        VerticalLayout body = VerticalLayout.create();
        body.add(myTextFormat == MessageTextFormat.RICH ? HtmlLabel.create(myText) : Label.create(myText));
        if (myDetail.isNotEmpty()) {
            body.add(Label.create(myDetail));
        }
        message.add(body);

        return message;
    }

    @RequiredUIAccess
    private Component buildBottom(Window window, AtomicReference<ButtonImpl<V>> pressed, @Nullable CheckBox rememberBox) {
        DockLayout bottom = DockLayout.create();

        if (rememberBox != null) {
            bottom.left(rememberBox);
        }

        HorizontalLayout buttons = HorizontalLayout.create(BUTTON_GAP);

        for (ButtonImpl<V> button : myButtons) {
            LocalizeValue text = button.label().map((manager, value) -> TextWithMnemonic.parse(value).getText());

            Button uiButton = Button.create(text, event -> {
                pressed.set(button);

                window.close();
            });

            if (button.myDefault) {
                uiButton.addStyle(ButtonStyle.PRIMARY);
            }

            buttons.add(uiButton);
        }

        bottom.right(buttons);

        return bottom;
    }
}
