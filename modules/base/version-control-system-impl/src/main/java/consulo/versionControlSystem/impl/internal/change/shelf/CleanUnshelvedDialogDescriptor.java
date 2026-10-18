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
package consulo.versionControlSystem.impl.internal.change.shelf;

import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.RadioGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.versionControlSystem.localize.VcsLocalize;

import java.util.List;

/**
 * Asks which already unshelved change lists to delete.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
class CleanUnshelvedDialogDescriptor extends DialogDescriptor {
    private final RadioGroup<CleanUnshelvedScope> myScope = RadioGroup.create();
    private CleanUnshelvedTimePeriod myTimePeriod = CleanUnshelvedTimePeriod.WEEK;

    CleanUnshelvedDialogDescriptor() {
        super(VcsLocalize.shelveCleanUnshelvedTitle());
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        ComboBox<CleanUnshelvedTimePeriod> timePeriod = ComboBox.create(List.of(CleanUnshelvedTimePeriod.values()));
        timePeriod.setTextRenderer(value -> value == null ? LocalizeValue.empty() : value.getText());
        timePeriod.setValue(myTimePeriod);
        timePeriod.addValueListener(event -> {
            CleanUnshelvedTimePeriod value = event.getValue();
            if (value != null) {
                myTimePeriod = value;
            }
        });

        VerticalLayout layout = VerticalLayout.create();
        layout.add(Label.create(VcsLocalize.shelveDeleteAlreadyUnshelvedLabel()));
        layout.add(myScope.newButton(VcsLocalize.shelveCleanUnshelvedSystem(), CleanUnshelvedScope.CREATED_AUTOMATICALLY));
        layout.add(HorizontalLayout.create()
            .add(myScope.newButton(VcsLocalize.shelveCleanUnshelvedOlderThan(), CleanUnshelvedScope.OLDER_THAN))
            .add(timePeriod));
        layout.add(myScope.newButton(VcsLocalize.shelveCleanUnshelvedAll(), CleanUnshelvedScope.ALL));

        myScope.addValueListener(value -> timePeriod.setEnabled(value == CleanUnshelvedScope.OLDER_THAN));
        myScope.setValue(CleanUnshelvedScope.CREATED_AUTOMATICALLY);
        timePeriod.setEnabled(false);
        return layout;
    }

    @Override
    protected DialogOkAction createOkAction() {
        return new DialogOkAction(CommonLocalize.buttonDelete());
    }

    CleanUnshelvedScope getScope() {
        CleanUnshelvedScope scope = myScope.getValue();
        return scope == null ? CleanUnshelvedScope.CREATED_AUTOMATICALLY : scope;
    }

    CleanUnshelvedTimePeriod getTimePeriod() {
        return myTimePeriod;
    }
}
