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
package consulo.desktop.qt.ui.impl.messagebox;

import consulo.desktop.qt.ui.impl.QtMnemonic;
import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.ui.MessageButtonRole;
import consulo.ui.MessageSeverity;
import consulo.ui.MessageTextFormat;
import consulo.ui.UIAccess;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.impl.internal.UnifiedMessageBoxBuilderImpl;
import consulo.ui.impl.BaseMessageBoxBuilder;

import io.qt.core.Qt;
import io.qt.widgets.QAbstractButton;
import io.qt.widgets.QApplication;
import io.qt.widgets.QCheckBox;
import io.qt.widgets.QMessageBox;
import io.qt.widgets.QPushButton;
import io.qt.widgets.QWidget;

import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import consulo.ui.impl.MessagePresentation;

/**
 * A message box drawn by the toolkit itself, which is what gives it platform button order, platform
 * button names, real keyboard handling and its own sizing.
 * <p>
 * A request the toolkit box cannot represent is handed to the assembled one instead.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public class DesktopQtMessageBoxBuilderImpl<V> extends BaseMessageBoxBuilder<V> {
    @Override
    @RequiredUIAccess
    public CompletableFuture<V> showAsync(@Nullable Window owner) {
        prepare();

        V remembered = rememberedValue();
        if (remembered != null) {
            return CompletableFuture.completedFuture(remembered);
        }

        if (!canPresentNatively()) {
            return fallback().showAsync(owner);
        }

        QWidget parent = owner == null ? QApplication.activeWindow() : TargetQt.to(owner);

        QMessageBox box = new QMessageBox(parent);
        box.setWindowTitle(MessagePresentation.title(myTitle).get());
        box.setText(myText.get());
        box.setTextFormat(myTextFormat == MessageTextFormat.RICH ? Qt.TextFormat.RichText : Qt.TextFormat.PlainText);
        box.setIcon(toIcon(mySeverity));

        if (myDetail.isNotEmpty()) {
            box.setDetailedText(myDetail.get());
        }

        Map<QAbstractButton, ButtonImpl<V>> answers = new IdentityHashMap<>();

        for (ButtonImpl<V> button : myButtons) {
            QPushButton widget = box.addButton(toStandardButton(button.myRole));
            if (widget == null) {
                continue;
            }

            if (button.myText != null) {
                widget.setText(QtMnemonic.withMnemonic(button.myText));
            }

            answers.put(widget, button);

            if (button.myDefault) {
                box.setDefaultButton(widget);
            }
            if (button.myExit) {
                box.setEscapeButton(widget);
            }
        }

        QCheckBox rememberBox = null;
        if (myRemember != null && myRemember.isVisible()) {
            rememberBox = new QCheckBox(QtMnemonic.plain(myRemember.getMessageText()));
            rememberBox.setChecked(myRemember.isRememberByDefault());
            box.setCheckBox(rememberBox);
        }

        CompletableFuture<V> result = new CompletableFuture<>();
        QCheckBox checkBox = rememberBox;

        box.finished.connect(code -> {
            if (result.isDone()) {
                return;
            }

            QAbstractButton clicked = box.clickedButton();
            ButtonImpl<V> pressed = clicked != null ? answers.get(clicked) : null;

            V value = pressed != null ? pressed.myValue.get() : exitValueOrNull();

            boolean checked = checkBox != null
                ? checkBox.isChecked()
                : myRemember != null && myRemember.isRememberByDefault();

            storeRemembered(pressed, checked, value);

            result.complete(value);

            box.disposeLater();

            runHelpIfNeeded(pressed);
        });

        box.destroyed.connect(destroyed -> {
            if (!result.isDone()) {
                result.complete(exitValueOrNull());
            }
        });

        // resolved here, on the ui thread - a cancel arrives on whichever thread asked for it
        UIAccess uiAccess = UIAccess.current();
        result.whenComplete((value, error) -> {
            if (!result.isCancelled()) {
                return;
            }

            // the box may already have been torn down, and the native object is gone with it
            uiAccess.give(() -> {
                if (!box.isDisposed()) {
                    box.reject();
                }
            });
        });

        box.open();

        return result;
    }

    /**
     * A link handler needs an activation signal the toolkit box does not expose.
     */
    private boolean canPresentNatively() {
        return myLinkHandler == null;
    }

    private UnifiedMessageBoxBuilderImpl<V> fallback() {
        UnifiedMessageBoxBuilderImpl<V> unified = new UnifiedMessageBoxBuilderImpl<>();
        copyInto(unified);
        return unified;
    }

    private static QMessageBox.Icon toIcon(MessageSeverity severity) {
        return switch (severity) {
            case NONE -> QMessageBox.Icon.NoIcon;
            case INFO -> QMessageBox.Icon.Information;
            case WARNING -> QMessageBox.Icon.Warning;
            case ERROR -> QMessageBox.Icon.Critical;
            case QUESTION -> QMessageBox.Icon.Question;
        };
    }

    private static QMessageBox.StandardButton toStandardButton(MessageButtonRole role) {
        return switch (role) {
            case OK -> QMessageBox.StandardButton.Ok;
            case YES -> QMessageBox.StandardButton.Yes;
            case NO -> QMessageBox.StandardButton.No;
            case CANCEL -> QMessageBox.StandardButton.Cancel;
            case CLOSE -> QMessageBox.StandardButton.Close;
            case RETRY -> QMessageBox.StandardButton.Retry;
            case YES_TO_ALL -> QMessageBox.StandardButton.YesToAll;
            case NO_TO_ALL -> QMessageBox.StandardButton.NoToAll;
            case HELP -> QMessageBox.StandardButton.Help;
        };
    }
}
