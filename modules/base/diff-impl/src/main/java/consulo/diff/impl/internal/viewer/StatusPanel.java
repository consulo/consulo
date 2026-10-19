/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.viewer;

import consulo.diff.localize.DiffLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.HorizontalLayout;

public class StatusPanel {
    private final HorizontalLayout myLayout;
    private final Label myTextLabel;
    private final Label myBusySpinner;

    @RequiredUIAccess
    public StatusPanel() {
        myTextLabel = Label.create();
        myTextLabel.setVisible(false);
        myBusySpinner = Label.create();
        myBusySpinner.setImage(Image.busy());
        myBusySpinner.setVisible(false);

        myLayout = HorizontalLayout.create(Space.X_SMALL);
        myLayout.add(myBusySpinner);
        myLayout.add(myTextLabel);
        myLayout.paddingBuilder().horizontalSet(Space.X_SMALL).apply();
    }

    public Component getComponent() {
        return myLayout;
    }

    @RequiredUIAccess
    public void update() {
        LocalizeValue message = getMessage();
        myTextLabel.setVisible(message.isNotEmpty());
        myTextLabel.setText(message);
    }

    @RequiredUIAccess
    public void setBusy(boolean busy) {
        myBusySpinner.setVisible(busy);
    }

    protected LocalizeValue getMessage() {
        int count = getChangesCount();
        if (count == -1) {
            return LocalizeValue.empty();
        }
        return DiffLocalize.diffCountDifferencesStatusText(count);
    }

    protected int getChangesCount() {
        return -1;
    }
}
