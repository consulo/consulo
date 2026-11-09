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
package consulo.task.impl.internal.issue;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.task.impl.internal.TaskSearcher;
import consulo.task.localize.TaskLocalize;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.TextBox;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.util.FormBuilder;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
final class IssueSearcherDialogDescriptor extends DialogDescriptor {
    private final IssueSearcherItem myInitial;
    private @Nullable TextBox myNameBox;
    private @Nullable TextBox myQueryBox;
    private @Nullable CheckBox myIncludeClosedBox;
    private @Nullable CheckBox myShareBox;

    private IssueSearcherDialogDescriptor(LocalizeValue title, IssueSearcherItem initial) {
        super(title);
        myInitial = initial;
    }

    @RequiredUIAccess
    static CompletableFuture<@Nullable IssueSearcherItem> showAsync(LocalizeValue title, IssueSearcherItem initial) {
        IssueSearcherDialogDescriptor descriptor = new IssueSearcherDialogDescriptor(title, initial);
        Dialog dialog = Application.get().getInstance(DialogService.class).build(descriptor);
        return dialog.showAsync().handleAsync(
            (value, error) -> error != null || value == null ? null : descriptor.createItem(),
            UIAccess.current()
        );
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        myNameBox = TextBox.create(myInitial.searcher.name);
        myNameBox.addValueListener(event -> updateOkButtonState());
        myQueryBox = TextBox.create(myInitial.searcher.query);
        myIncludeClosedBox = CheckBox.create(TaskLocalize.searchersCheckboxIncludeClosed(), myInitial.searcher.includeClosed);
        myShareBox = CheckBox.create(TaskLocalize.searchersCheckboxShare(), myInitial.project);
        FormBuilder builder = FormBuilder.create()
            .addLabeled(TaskLocalize.searchersLabelName(), myNameBox)
            .addLabeled(TaskLocalize.searchersLabelQuery(), myQueryBox);
        builder.addBottom(myIncludeClosedBox);
        builder.addBottom(myShareBox);
        return builder.build();
    }

    @Override
    public boolean doUpdateOkButtonState() {
        TextBox nameBox = myNameBox;
        return nameBox != null && !StringUtil.isEmptyOrSpaces(nameBox.getValue());
    }

    @RequiredUIAccess
    private @Nullable IssueSearcherItem createItem() {
        if (myNameBox == null || myQueryBox == null || myIncludeClosedBox == null || myShareBox == null) {
            return null;
        }
        TaskSearcher searcher = new TaskSearcher();
        searcher.name = StringUtil.notNullize(myNameBox.getValue()).trim();
        searcher.type = myInitial.searcher.type;
        searcher.query = StringUtil.notNullize(myQueryBox.getValue()).trim();
        searcher.includeClosed = myIncludeClosedBox.getValueOrError();
        return new IssueSearcherItem(searcher, myShareBox.getValueOrError(), false);
    }
}
