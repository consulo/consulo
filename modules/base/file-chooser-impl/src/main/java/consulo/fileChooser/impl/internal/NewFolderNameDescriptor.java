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
package consulo.fileChooser.impl.internal;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TextBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.layout.VerticalLayout;

class NewFolderNameDescriptor extends DialogDescriptor {
    private final LocalizeValue myPrompt;
    private final TextBox myTextBox = TextBox.create();

    NewFolderNameDescriptor(LocalizeValue title, LocalizeValue prompt) {
        super(title);
        myPrompt = prompt;
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        myTextBox.addValueListener(event -> updateOkButtonState());

        VerticalLayout layout = VerticalLayout.create();
        layout.add(Label.create(myPrompt));
        layout.add(myTextBox);
        return layout;
    }

    @RequiredUIAccess
    @Override
    public Component getPreferredFocusedComponent() {
        return myTextBox;
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return !myTextBox.getValueOrError().isBlank();
    }

    String getFolderName() {
        return myTextBox.getValueOrError();
    }
}
