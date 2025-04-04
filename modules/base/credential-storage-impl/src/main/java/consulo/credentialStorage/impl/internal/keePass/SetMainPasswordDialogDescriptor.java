/*
 * Copyright 2013-2025 consulo.io
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
package consulo.credentialStorage.impl.internal.keePass;

import consulo.credentialStorage.localize.CredentialStorageLocalize;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2025-04-04
 */
public class SetMainPasswordDialogDescriptor extends DialogDescriptor {

    public record NewMasterPasswordResult(byte[] password) implements DialogValue {
    }

    private PasswordBox myPasswordBox;
    private final Function<byte[], LocalizeValue> myOkHandler;
    private final LocalizeValue myTopNote;

    public SetMainPasswordDialogDescriptor(LocalizeValue title, Function<byte[], LocalizeValue> okHandler, LocalizeValue topNote) {
        super(title);
        myOkHandler = okHandler;
        myTopNote = topNote;
    }

    @RequiredUIAccess
    @Override
    public boolean canHandle(@Nonnull AnAction action, @Nullable DialogValue value, @Nonnull Window window) {
        if (value instanceof NewMasterPasswordResult newMasterPasswordResult) {
            LocalizeValue error = myOkHandler.apply(newMasterPasswordResult.password());
            if (error != LocalizeValue.of())  {
                Alerts.okError(error).showAsync(window);
                return false;
            }
        }

        return super.canHandle(action, value);
    }

    @Nonnull
    @Override
    public DialogValue getOkValue() {
        return new NewMasterPasswordResult(myPasswordBox.getValueOrError().getBytes(StandardCharsets.UTF_8));
    }

    @RequiredUIAccess
    @Nonnull
    @Override
    public Component createCenterComponent(@Nonnull Disposable uiDisposable) {
        myPasswordBox = PasswordBox.create();
        VerticalLayout layout = VerticalLayout.create();
        if (myTopNote != LocalizeValue.of()) {
            layout.add(Label.create(myTopNote));
        }
        layout.add(LabeledBuilder.filled(CredentialStorageLocalize.keePassRowMasterPassword(), myPasswordBox));
        
        return layout;
    }

    @RequiredUIAccess
    @Override
    public Component getPreferredFocusedComponent() {
        return myPasswordBox;
    }

    @Override
    public boolean hasBorderAtButtonLayout() {
        return false;
    }
}
