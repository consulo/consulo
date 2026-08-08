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
package consulo.ui.ex.impl.internal.popup;

import consulo.component.ComponentManager;
import consulo.ui.Component;
import consulo.ui.HasFocus;
import consulo.ui.HeavyPopup;
import consulo.ui.PopupOptions;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.InputDetails;
import org.jspecify.annotations.Nullable;

import java.awt.event.InputEvent;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-08-08
 */
public class UnifiedComponentPopupImpl extends UnifiedPopupImpl {
    private final Component myContent;
    private final @Nullable Component myPreferableFocusComponent;
    private final PopupOptions myOptions;
    private final @Nullable Supplier<Boolean> myCancelCallback;

    private @Nullable String myTitle;
    private @Nullable HeavyPopup myPopup;

    public UnifiedComponentPopupImpl(
        Component content,
        @Nullable Component preferableFocusComponent,
        @Nullable String title,
        PopupOptions options,
        @Nullable Supplier<Boolean> cancelCallback
    ) {
        myContent = content;
        myPreferableFocusComponent = preferableFocusComponent;
        myTitle = title;
        myOptions = options;
        myCancelCallback = cancelCallback;
    }

    @Override
    @RequiredUIAccess
    public void showCenteredInCurrentWindow(ComponentManager project) {
        show();
    }

    @Override
    @RequiredUIAccess
    public void showBy(Component component, @Nullable InputDetails inputDetails) {
        show();
    }

    @Override
    @RequiredUIAccess
    public void showAtPoint(Component target, int x, int y, int anchorHeight) {
        show();
    }

    @RequiredUIAccess
    private void show() {
        if (isDisposed() || myPopup != null) {
            return;
        }

        HeavyPopup popup = HeavyPopup.create(myOptions);
        popup.setTitle(myTitle);
        popup.setContent(myContent);
        popup.addCloseListener(event -> cancel(null));

        myPopup = popup;

        fireBeforeShown();

        popup.showInCenterOf(null);

        if (myPreferableFocusComponent instanceof HasFocus hasFocus) {
            hasFocus.focus();
        }
    }

    @Override
    @RequiredUIAccess
    public void cancel(@Nullable InputEvent e) {
        if (isDisposed()) {
            return;
        }

        HeavyPopup popup = myPopup;
        myPopup = null;

        if (popup != null && popup.isVisible()) {
            popup.close();
        }

        finish();
    }

    @Override
    public boolean canClose() {
        Supplier<Boolean> cancelCallback = myCancelCallback;
        return cancelCallback == null || Boolean.TRUE.equals(cancelCallback.get());
    }

    @Override
    public boolean isVisible() {
        HeavyPopup popup = myPopup;
        return popup != null && popup.isVisible();
    }

    @Override
    public void setCaption(String title) {
        myTitle = title;

        HeavyPopup popup = myPopup;
        if (popup != null) {
            popup.setTitle(title);
        }
    }
}
