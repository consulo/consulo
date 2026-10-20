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
package consulo.project.ui.impl.internal.notification;

import consulo.project.ui.notification.NotificationType;
import consulo.ui.Component;
import consulo.ui.LightPopup;
import consulo.ui.PopupPosition;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The balloons a status bar shows over its widgets and its progress area, at most one per place.
 *
 * @author VISTALL
 * @since 2026-10-20
 */
public final class StatusBarBalloonHolder {
    private final Map<String, LightPopup> myBalloons = new HashMap<>();

    @RequiredUIAccess
    public void show(
        String key,
        @Nullable Component target,
        NotificationType type,
        String text,
        @Nullable Image icon,
        @Nullable ComponentEventListener<Component, HyperlinkEvent> linkListener
    ) {
        hide(key);

        if (target == null) {
            return;
        }

        LightPopup popup = NotificationBalloon.create(PopupPosition.TOP, type, text, icon, linkListener);
        popup.addCloseListener(event -> myBalloons.remove(key, popup));
        myBalloons.put(key, popup);
        popup.showBy(target);
    }

    @RequiredUIAccess
    public void hide(String key) {
        LightPopup popup = myBalloons.get(key);
        if (popup != null) {
            popup.close();
        }
    }
}
