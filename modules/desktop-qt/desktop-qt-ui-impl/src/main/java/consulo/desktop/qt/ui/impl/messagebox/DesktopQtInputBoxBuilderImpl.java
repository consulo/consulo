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

import consulo.desktop.qt.ui.impl.TargetQt;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.DialogCancelledException;
import consulo.ui.ValueComponent;
import consulo.ui.Window;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.impl.internal.UnifiedInputBoxBuilderImpl;
import consulo.ui.impl.BaseInputBoxBuilder;

import io.qt.widgets.QApplication;
import io.qt.widgets.QInputDialog;
import io.qt.widgets.QLineEdit;
import io.qt.widgets.QWidget;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import consulo.ui.impl.MessagePresentation;

/**
 * An input box drawn by the toolkit itself. The toolkit box takes one value and offers no way to
 * vet it, so a request carrying a validator, a help affordance or its own image is handed to the
 * assembled one instead.
 *
 * @author VISTALL
 * @since 2026-09-04
 */
public class DesktopQtInputBoxBuilderImpl<V, C extends ValueComponent<V>> extends BaseInputBoxBuilder<V, C> {
    /**
     * How a value is carried through the toolkit box, which only speaks text.
     */
    public enum Mode {
        TEXT,
        PASSWORD,
        INTEGER
    }

    private final Mode myMode;
    private final Supplier<C> myEditorFactory;

    public DesktopQtInputBoxBuilderImpl(Mode mode, Supplier<C> editorFactory) {
        myMode = mode;
        myEditorFactory = editorFactory;
    }

    @Override
    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    public CompletableFuture<V> showAsync(@Nullable Window owner) {
        if (!canPresentNatively()) {
            return fallback().showAsync(owner);
        }

        QWidget parent = owner == null ? QApplication.activeWindow() : TargetQt.to(owner);

        QInputDialog dialog = new QInputDialog(parent);
        dialog.setWindowTitle(MessagePresentation.title(myTitle).get());
        dialog.setLabelText(myText.get());

        if (myMode == Mode.INTEGER) {
            dialog.setInputMode(QInputDialog.InputMode.IntInput);
            if (myInitialValue instanceof Integer initial) {
                dialog.setIntValue(initial);
            }
        }
        else {
            dialog.setInputMode(QInputDialog.InputMode.TextInput);
            if (myMode == Mode.PASSWORD) {
                dialog.setTextEchoMode(QLineEdit.EchoMode.Password);
            }
            if (myInitialValue != null) {
                dialog.setTextValue(String.valueOf(myInitialValue));
            }
        }

        dialog.setOkButtonText((myConfirmText.isNotEmpty() ? myConfirmText : CommonLocalize.buttonOk()).get());
        dialog.setCancelButtonText((myCancelText.isNotEmpty() ? myCancelText : CommonLocalize.buttonCancel()).get());

        CompletableFuture<V> result = new CompletableFuture<>();

        dialog.finished.connect(code -> {
            if (result.isDone()) {
                return;
            }

            boolean accepted = code == io.qt.widgets.QDialog.DialogCode.Accepted.value();

            V value = accepted ? normalize(read(dialog)) : null;
            if (value != null) {
                result.complete(value);
            }
            else {
                result.completeExceptionally(new DialogCancelledException());
            }

            dialog.disposeLater();
        });

        dialog.destroyed.connect(destroyed -> {
            if (!result.isDone()) {
                result.completeExceptionally(new DialogCancelledException());
            }
        });

        dialog.open();

        return result;
    }

    @SuppressWarnings("unchecked")
    private @Nullable V read(QInputDialog dialog) {
        return myMode == Mode.INTEGER ? (V)Integer.valueOf(dialog.intValue()) : (V)dialog.textValue();
    }

    /**
     * The toolkit box has no hook to vet a value, no place for a help affordance and no image slot.
     */
    private boolean canPresentNatively() {
        return myValidator == null && myHelpAction == null && myIcon == null && mySetups.isEmpty();
    }

    private UnifiedInputBoxBuilderImpl<V, C> fallback() {
        UnifiedInputBoxBuilderImpl<V, C> unified = new UnifiedInputBoxBuilderImpl<>(myEditorFactory);
        copyInto(unified);
        return unified;
    }
}
