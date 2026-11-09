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
import consulo.ui.Component;
import consulo.ui.TextArea;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.WidthAndHeight;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
final class IssueCommentDialogDescriptor extends DialogDescriptor {
    private final String myInitialText;
    private @Nullable TextArea myTextArea;

    private IssueCommentDialogDescriptor(LocalizeValue title, String initialText) {
        super(title);
        myInitialText = initialText;
    }

    @RequiredUIAccess
    static CompletableFuture<@Nullable String> showAsync(LocalizeValue title, String initialText) {
        IssueCommentDialogDescriptor descriptor = new IssueCommentDialogDescriptor(title, initialText);
        Dialog dialog = Application.get().getInstance(DialogService.class).build(descriptor);
        return dialog.showAsync().handleAsync(
            (value, error) -> error != null || value == null || descriptor.myTextArea == null ? null : StringUtil.nullize(descriptor.myTextArea.getValue(), true),
            UIAccess.current()
        );
    }

    @Override
    public @Nullable WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(40, 12);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        myTextArea = TextArea.create(myInitialText);
        myTextArea.addValueListener(event -> updateOkButtonState());
        DockLayout layout = DockLayout.create();
        layout.center(ScrollableLayout.create(myTextArea));
        return layout;
    }

    @Override
    public boolean doUpdateOkButtonState() {
        return myTextArea != null && !StringUtil.isEmptyOrSpaces(myTextArea.getValue());
    }
}
