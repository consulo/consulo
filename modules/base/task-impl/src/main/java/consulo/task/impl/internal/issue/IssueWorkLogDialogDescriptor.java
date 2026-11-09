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
import consulo.task.localize.TaskLocalize;
import consulo.ui.Component;
import consulo.ui.TextArea;
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
public final class IssueWorkLogDialogDescriptor extends DialogDescriptor {
    public record Result(String timeSpent, String comment) {
    }

    private final String myInitialTime;
    private final String myInitialComment;
    private @Nullable TextBox myTimeBox;
    private @Nullable TextArea myCommentArea;

    private IssueWorkLogDialogDescriptor(LocalizeValue title, String initialTime, String initialComment) {
        super(title);
        myInitialTime = initialTime;
        myInitialComment = initialComment;
    }

    @RequiredUIAccess
    public static CompletableFuture<@Nullable Result> showAsync(LocalizeValue title, String initialTime, String initialComment) {
        IssueWorkLogDialogDescriptor descriptor = new IssueWorkLogDialogDescriptor(title, initialTime, initialComment);
        Dialog dialog = Application.get().getInstance(DialogService.class).build(descriptor);
        return dialog.showAsync().handleAsync((value, error) -> {
            if (error != null || value == null || descriptor.myTimeBox == null || descriptor.myCommentArea == null) {
                return null;
            }
            return new Result(StringUtil.notNullize(descriptor.myTimeBox.getValue()).trim(), StringUtil.notNullize(descriptor.myCommentArea.getValue()));
        }, UIAccess.current());
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        myTimeBox = TextBox.create(myInitialTime).withPlaceholder(LocalizeValue.of("1h 30m"));
        myTimeBox.addValueListener(event -> updateOkButtonState());
        myCommentArea = TextArea.create(myInitialComment);
        return FormBuilder.create()
            .addLabeled(TaskLocalize.issuesWorklogLabelTime(), myTimeBox)
            .addLabeled(TaskLocalize.issuesWorklogLabelComment(), myCommentArea)
            .build();
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return myTimeBox != null && !StringUtil.isEmptyOrSpaces(myTimeBox.getValue());
    }
}
