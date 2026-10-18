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
package consulo.versionControlSystem.log.impl.internal.ui.filter;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.DatePicker;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.util.FormBuilder;
import consulo.versionControlSystem.log.VcsLogDateFilter;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogDateFilterImpl;
import consulo.versionControlSystem.log.localize.VersionControlSystemLogLocalize;
import org.jspecify.annotations.Nullable;

import java.util.Date;
import java.util.concurrent.CompletableFuture;

/**
 * Picks the period of the date filter: each bound is optional.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
final class DateFilterDialogDescriptor extends DialogDescriptor {
    private final @Nullable VcsLogDateFilter myInitial;

    private @Nullable CheckBox myAfterEnabled;
    private @Nullable DatePicker myAfter;
    private @Nullable CheckBox myBeforeEnabled;
    private @Nullable DatePicker myBefore;

    private DateFilterDialogDescriptor(@Nullable VcsLogDateFilter initial) {
        super(VersionControlSystemLogLocalize.filterSelectPeriodTitle());
        myInitial = initial;
    }

    @RequiredUIAccess
    static CompletableFuture<@Nullable VcsLogDateFilter> showAsync(@Nullable VcsLogDateFilter initial) {
        DateFilterDialogDescriptor descriptor = new DateFilterDialogDescriptor(initial);
        Dialog dialog = Application.get().getInstance(DialogService.class).build(descriptor);
        return dialog.showAsync().handleAsync(
            (value, error) -> error != null || value == null ? null : descriptor.createFilter(),
            UIAccess.current()
        );
    }

    @Override
    @RequiredUIAccess
    public Component createCenterComponent(Disposable uiDisposable) {
        Date after = myInitial == null ? null : myInitial.getAfter();
        Date before = myInitial == null ? null : myInitial.getBefore();

        myAfterEnabled = CheckBox.create(VersionControlSystemLogLocalize.filterDateAfter(), after != null);
        myAfter = createPicker(myAfterEnabled, after);
        myBeforeEnabled = CheckBox.create(VersionControlSystemLogLocalize.filterDateBefore(), before != null);
        myBefore = createPicker(myBeforeEnabled, before);

        return FormBuilder.create()
            .addLabeled(myAfterEnabled, myAfter)
            .addLabeled(myBeforeEnabled, myBefore)
            .build();
    }

    @RequiredUIAccess
    private static DatePicker createPicker(CheckBox enabled, @Nullable Date value) {
        DatePicker picker = DatePicker.create();
        picker.setValue(value == null ? new Date() : value);
        picker.setEnabled(enabled.getValueOrError());
        enabled.addValueListener(event -> picker.setEnabled(event.getValue()));
        return picker;
    }

    @RequiredUIAccess
    private VcsLogDateFilter createFilter() {
        return new VcsLogDateFilterImpl(valueOf(myAfterEnabled, myAfter), valueOf(myBeforeEnabled, myBefore));
    }

    @RequiredUIAccess
    private static @Nullable Date valueOf(@Nullable CheckBox enabled, @Nullable DatePicker picker) {
        if (enabled == null || picker == null || !enabled.getValueOrError()) {
            return null;
        }
        return picker.getValue();
    }
}
