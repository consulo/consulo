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
import consulo.task.localize.TaskLocalize;
import consulo.ui.ComboBox;
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

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
final class IssueLinkDialogDescriptor extends DialogDescriptor {
    record Result(String type, String issueId) {
    }

    private final List<String> myTypes;
    private @Nullable ComboBox<String> myTypeBox;
    private @Nullable TextBox myIssueBox;

    private IssueLinkDialogDescriptor(List<String> types) {
        super(TaskLocalize.issuesActionLinkAdd());
        myTypes = types;
    }

    @RequiredUIAccess
    static CompletableFuture<@Nullable Result> showAsync(List<String> types) {
        IssueLinkDialogDescriptor descriptor = new IssueLinkDialogDescriptor(types);
        Dialog dialog = Application.get().getInstance(DialogService.class).build(descriptor);
        return dialog.showAsync().handleAsync((value, error) -> {
            if (error != null || value == null || descriptor.myTypeBox == null || descriptor.myIssueBox == null) {
                return null;
            }
            String type = descriptor.myTypeBox.getValue();
            return type == null ? null : new Result(type, StringUtil.notNullize(descriptor.myIssueBox.getValue()).trim());
        }, UIAccess.current());
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        myTypeBox = ComboBox.create(myTypes);
        if (!myTypes.isEmpty()) {
            myTypeBox.setValue(myTypes.get(0));
        }
        myIssueBox = TextBox.create();
        myIssueBox.addValueListener(event -> updateOkButtonState());
        return FormBuilder.create()
            .addLabeled(TaskLocalize.issuesLinkLabelType(), myTypeBox)
            .addLabeled(TaskLocalize.issuesLinkLabelIssue(), myIssueBox)
            .build();
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return myIssueBox != null && !StringUtil.isEmptyOrSpaces(myIssueBox.getValue());
    }
}
