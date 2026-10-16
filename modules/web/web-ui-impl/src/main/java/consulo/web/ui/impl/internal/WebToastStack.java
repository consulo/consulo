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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;
import consulo.ui.Toast;
import consulo.ui.internal.ToastInternal;
import consulo.web.ui.impl.internal.base.TargetVaadin;
import org.jspecify.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The toasts floating over one window of a tab: every toast in a notification of its own in the bottom right corner -
 * vaadin stacks them, the newest at the bottom, above dialogs too - with the card of the notification taken away, the
 * toast draws itself. The corner is the one of the area of the window: the browser keeps how far it is from the corner
 * of the page in {@code --consulo-toast-right} and {@code --consulo-toast-bottom}, which pad the notifications.
 *
 * @author VISTALL
 * @since 2026-10-16
 */
public final class WebToastStack {
    private final Map<Toast, Notification> myNotifications = new IdentityHashMap<>();

    public void show(Toast toast) {
        if (myNotifications.containsKey(toast)) {
            return;
        }
        if (toast instanceof ToastInternal internal) {
            internal.setShadow(true);
        }
        Notification notification = new Notification();
        notification.setPosition(Notification.Position.BOTTOM_END);
        notification.setDuration(0);
        notification.addThemeName("consulo-toast");
        notification.add(TargetVaadin.to(toast));
        myNotifications.put(toast, notification);
        notification.open();
    }

    public void setArea(@Nullable Component area) {
        if (area != null) {
            area.getElement().executeJs(
                "const area = this;" +
                    "if (window.consuloToastArea) { window.consuloToastArea.disconnect(); }" +
                    "const style = document.documentElement.style;" +
                    "const update = () => {" +
                    "  if (!area.isConnected) { return; }" +
                    "  const bounds = area.getBoundingClientRect();" +
                    "  const page = document.documentElement;" +
                    "  style.setProperty('--consulo-toast-right', Math.max(0, page.clientWidth - bounds.right) + 'px');" +
                    "  style.setProperty('--consulo-toast-bottom', Math.max(0, page.clientHeight - bounds.bottom) + 'px');" +
                    "};" +
                    "const observer = new ResizeObserver(update);" +
                    "observer.observe(area);" +
                    "observer.observe(document.body);" +
                    "window.consuloToastArea = observer;" +
                    "update();"
            );
            return;
        }

        UI ui = UI.getCurrent();
        if (ui != null) {
            ui.getPage().executeJs(
                "if (window.consuloToastArea) { window.consuloToastArea.disconnect(); window.consuloToastArea = null; }" +
                    "document.documentElement.style.removeProperty('--consulo-toast-right');" +
                    "document.documentElement.style.removeProperty('--consulo-toast-bottom');"
            );
        }
    }

    public void hide(Toast toast) {
        Notification notification = myNotifications.remove(toast);
        if (notification != null) {
            notification.close();
        }
    }
}
