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
import consulo.ui.LightPopup;
import consulo.ui.UIAccess;
import consulo.ui.Point2D;
import consulo.ui.Popup;
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
    private @Nullable Popup myPopup;

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
        show(null, null, 0);
    }

    @Override
    @RequiredUIAccess
    public void showBy(Component component, @Nullable InputDetails inputDetails) {
        show(component, null, 0);
    }

    @Override
    @RequiredUIAccess
    public void showAtPoint(Component target, int x, int y, int anchorHeight) {
        show(target, new Point2D(x, y), anchorHeight);
    }

    @RequiredUIAccess
    private void show(@Nullable Component anchor, @Nullable Point2D anchorPoint, int anchorHeight) {
        if (isDisposed() || myPopup != null) {
            return;
        }

        // a popup which was given something to hang off is a light popup - only one with no target at all is placed
        Popup popup = anchor != null ? LightPopup.create(myOptions) : HeavyPopup.create(myOptions);
        popup.setTitle(myTitle);
        popup.setContent(myContent);
        popup.addCloseListener(event -> cancel(null));

        myPopup = popup;

        fireBeforeShown();

        if (anchor != null && anchorPoint != null) {
            popup.showAt(anchor, anchorPoint.x(), anchorPoint.y(), anchorHeight);
        }
        else if (anchor != null && popup instanceof LightPopup light) {
            light.showBy(anchor);
        }
        else if (popup instanceof HeavyPopup heavy) {
            heavy.showInCenterOf(null);
        }

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

        Popup popup = myPopup;
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
        Popup popup = myPopup;
        return popup != null && popup.isVisible();
    }

    @Override
    public @Nullable UIAccess getUIAccess() {
        Popup popup = myPopup;
        return popup == null ? null : popup.getUIAccess();
    }

    @Override
    public void setCaption(String title) {
        myTitle = title;

        Popup popup = myPopup;
        if (popup != null) {
            popup.setTitle(title);
        }
    }
}
