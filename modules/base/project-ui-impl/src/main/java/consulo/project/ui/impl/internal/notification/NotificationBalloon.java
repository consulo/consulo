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
import consulo.ui.HtmlView;
import consulo.ui.Label;
import consulo.ui.LightPopup;
import consulo.ui.PopupOptions;
import consulo.ui.PopupPosition;
import consulo.ui.PopupStyle;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.RGBColor;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.style.ComponentColors;
import consulo.ui.util.ColorValueUtil;
import org.jspecify.annotations.Nullable;

import java.net.URL;

/**
 * The balloon a tool window button or a status bar widget reports with - a glassy popup pointing at it, coloured
 * like a toast of the same type.
 *
 * @author VISTALL
 * @since 2026-10-20
 */
public final class NotificationBalloon {
    private static final RGBColor TRANSPARENT = new RGBColor(0, 0, 0, 0);

    private NotificationBalloon() {
    }

    @RequiredUIAccess
    public static LightPopup create(
        PopupPosition position,
        NotificationType type,
        String text,
        @Nullable Image icon,
        @Nullable ComponentEventListener<Component, HyperlinkEvent> linkListener
    ) {
        LightPopup popup = LightPopup.create(
            PopupOptions.builder()
                .position(position)
                .arrow()
                .disableRequestFocus()
                .build()
        );
        popup.addStyle(style(type));
        popup.addStyle(PopupStyle.GLASSY);
        popup.setContent(createContent(popup, text, icon, linkListener));
        return popup;
    }

    private static PopupStyle style(NotificationType type) {
        return switch (type) {
            case INFORMATION -> PopupStyle.INFO;
            case SUCCESS -> PopupStyle.SUCCESS;
            case WARNING -> PopupStyle.WARNING;
            case ERROR -> PopupStyle.ERROR;
        };
    }

    @RequiredUIAccess
    private static Component createContent(
        LightPopup popup,
        String text,
        @Nullable Image icon,
        @Nullable ComponentEventListener<Component, HyperlinkEvent> linkListener
    ) {
        HtmlView view = HtmlView.create();
        view.setSizeToContent(true);
        view.setBackgroundColor(TRANSPARENT);
        view.render(new HtmlView.RenderData(text.replace("\n", "<br>"), css(), new URL[0]))
            .whenCompleteAsync((result, error) -> popup.pack(), UIAccess.current());
        view.addHyperlinkListener(event -> {
            popup.close();
            if (linkListener != null) {
                linkListener.onEvent(event);
            }
        });

        DockLayout layout = DockLayout.create();
        layout.paddingBuilder().verticalSet(Space.MEDIUM).horizontalSet(Space.MEDIUM).apply();
        if (icon != null) {
            Label iconLabel = Label.create();
            iconLabel.setImage(icon);
            iconLabel.paddingBuilder().rightSet(Space.MEDIUM).apply();
            layout.left(iconLabel);
        }
        layout.center(view);
        return layout;
    }

    private static String css() {
        return "html, body { margin: 0; padding: 0; background-color: transparent; color: "
            + ColorValueUtil.toCssColor(ComponentColors.TEXT_FOREGROUND) + "; }";
    }
}
