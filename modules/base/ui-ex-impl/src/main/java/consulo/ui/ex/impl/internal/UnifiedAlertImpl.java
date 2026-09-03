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
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.ImageBox;
import consulo.ui.Label;
import consulo.ui.NotificationType;
import consulo.ui.Space;
import consulo.ui.Window;
import consulo.ui.WindowOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.impl.BaseAlert;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.util.TextWithMnemonic;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * The counterpart of {@code DesktopPlainAlertImpl} for the frontends which have no {@code DialogWrapper} - the
 * alert is a {@link Window} whose content is the icon of its type next to the message over a row of buttons.
 *
 * @author VISTALL
 * @since 2026-08-02
 */
public class UnifiedAlertImpl<V> extends BaseAlert<V> {
    private static final Space ICON_GAP = Space.X_LARGE;
    private static final Space BUTTON_GAP = Space.MEDIUM;

    @Override
    @RequiredUIAccess
    public CompletableFuture<V> showAsync(@Nullable Window owner) {
        if (myButtons.isEmpty()) {
            throw new UnsupportedOperationException("Buttons empty");
        }

        if (myExitValue == null) {
            throw new UnsupportedOperationException("Exit value is not set. Use #asExitButton() or #exitValue()");
        }

        V remembered = myRemember != null ? myRemember.getValue() : null;
        if (remembered != null) {
            return CompletableFuture.completedFuture(remembered);
        }

        Window window = Window.create(myTitle.get(), WindowOptions.builder().owner(owner).build());

        CheckBox rememberBox = myRemember == null
            ? null
            : CheckBox.create(LocalizeValue.of(myRemember.getMessageBoxText()), myRemember.isRememberByDefault());

        // a button records the value it stands for and closes, so the close is the single place which answers the
        // caller - the cross of the window lands there too, and then no button was hit. the value itself cannot
        // double as that mark, a button is free to answer null
        AtomicReference<Supplier<V>> picked = new AtomicReference<>();

        window.setContent(buildContent(window, picked, rememberBox));

        CompletableFuture<V> result = new CompletableFuture<>();

        window.addCloseListener(event -> {
            Supplier<V> valueGetter = picked.get();

            V value = valueGetter == null ? myExitValue.get() : valueGetter.get();

            if (rememberBox != null && rememberBox.getValueOrError()) {
                myRemember.setValue(value);
            }

            result.complete(value);
        });

        window.show();

        return result;
    }

    @RequiredUIAccess
    private Component buildContent(Window window, AtomicReference<Supplier<V>> picked, @Nullable CheckBox rememberBox) {
        DockLayout root = DockLayout.create();

        root.center(buildMessage());
        root.bottom(buildBottom(window, picked, rememberBox));

        return root;
    }

    @RequiredUIAccess
    private Component buildMessage() {
        HorizontalLayout message = HorizontalLayout.create(ICON_GAP);

        message.add(ImageBox.create(getIcon(myType)));
        message.add(Label.create(myText));

        return message;
    }

    @RequiredUIAccess
    private Component buildBottom(Window window, AtomicReference<Supplier<V>> picked, @Nullable CheckBox rememberBox) {
        DockLayout bottom = DockLayout.create();

        if (rememberBox != null) {
            bottom.left(rememberBox);
        }

        HorizontalLayout buttons = HorizontalLayout.create(BUTTON_GAP);

        for (ButtonImpl button : myButtons) {
            LocalizeValue text = getText(button).map((manager, value) -> TextWithMnemonic.parse(value).getText());

            Button uiButton = Button.create(text, event -> {
                picked.set(button.myValue);

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

    private static Image getIcon(NotificationType type) {
        return switch (type) {
            case INFO -> PlatformIconGroup.generalInformationdialog();
            case WARNING -> PlatformIconGroup.generalWarningdialog();
            case ERROR -> PlatformIconGroup.generalErrordialog();
            case QUESTION -> PlatformIconGroup.generalQuestiondialog();
        };
    }
}
