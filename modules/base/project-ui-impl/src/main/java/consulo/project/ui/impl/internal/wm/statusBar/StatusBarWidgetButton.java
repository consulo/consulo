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
package consulo.project.ui.impl.internal.wm.statusBar;

import consulo.dataContext.DataManager;
import consulo.localize.LocalizeValue;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.ImageBox;
import consulo.ui.PopupPosition;
import consulo.ui.PseudoComponent;
import consulo.ui.ToolTip;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public final class StatusBarWidgetButton implements PseudoComponent {
    private static final long REOPEN_THRESHOLD_MS = 200;

    private final StatusBarWidget myWidget;
    private final StatusBarWidget.WidgetPresentation myPresentation;
    private final Button myButton;
    private @Nullable Image mySuffixIcon;

    private @Nullable JBPopup myPopup;
    private long myPopupClosedAt;

    @RequiredUIAccess
    public StatusBarWidgetButton(StatusBarWidget widget, StatusBarWidget.WidgetPresentation presentation) {
        myWidget = widget;
        myPresentation = presentation;

        myButton = Button.create(LocalizeValue.empty());
        myButton.addStyle(ButtonStyle.TOOLBAR);
        myButton.setFocusable(false);
        myButton.setVisible(false);
        myButton.addClickListener(this::onClick);

        StatusBarWidgetContextMenu.install(myButton, widget);
    }

    public StatusBarWidget getWidget() {
        return myWidget;
    }

    @RequiredUIAccess
    public void beforeUpdate() {
        myWidget.beforeUpdate();

        String text = switch (myPresentation) {
            case StatusBarWidget.TextPresentation presentation -> presentation.getText();
            case StatusBarWidget.MultipleTextValuesPresentation presentation -> StringUtil.notNullize(presentation.getSelectedValue());
            default -> "";
        };

        Image icon = switch (myPresentation) {
            case StatusBarWidget.IconPresentation presentation -> presentation.getIcon();
            case StatusBarWidget.MultipleTextValuesPresentation presentation -> presentation.getIcon();
            default -> null;
        };

        Image secondaryIcon = myPresentation instanceof StatusBarWidget.MultipleTextValuesPresentation presentation
            ? presentation.getSecondaryIcon()
            : null;

        myButton.setText(LocalizeValue.of(text));
        myButton.setIcon(icon);
        updateSuffix(secondaryIcon);
        myButton.setToolTip(createToolTip());
        myButton.setEnabled(myPresentation.isEnabled());
        myButton.setVisible(icon != null || !text.isEmpty());
    }

    @RequiredUIAccess
    private void updateSuffix(@Nullable Image secondaryIcon) {
        if (Objects.equals(mySuffixIcon, secondaryIcon)) {
            return;
        }
        mySuffixIcon = secondaryIcon;
        myButton.setSuffixComponent(secondaryIcon == null ? null : ImageBox.create(secondaryIcon));
    }

    private @Nullable ToolTip createToolTip() {
        LocalizeValue title = myPresentation.getTooltipText();
        String shortcut = myPresentation.getShortcutText();
        if (title.isEmpty() && StringUtil.isEmpty(shortcut)) {
            return null;
        }
        return ToolTip.create().setTitle(title).setShortcut(LocalizeValue.ofNullable(shortcut));
    }

    @RequiredUIAccess
    private void onClick(ClickEvent event) {
        if (myPresentation instanceof StatusBarWidget.MultipleTextValuesPresentation presentation) {
            togglePopup(presentation, event);
            return;
        }

        ComponentEventListener<Component, ClickEvent> listener = myPresentation.getClickListener();
        if (listener != null) {
            listener.onEvent(event);
        }
    }

    @RequiredUIAccess
    private void togglePopup(StatusBarWidget.MultipleTextValuesPresentation presentation, ClickEvent event) {
        JBPopup shown = myPopup;
        if (shown != null && shown.isVisible()) {
            shown.cancel();
            return;
        }

        if (System.currentTimeMillis() - myPopupClosedAt < REOPEN_THRESHOLD_MS) {
            return;
        }

        ListPopup popup = presentation.getPopupStep(DataManager.getInstance().getDataContext(myButton));
        if (popup == null) {
            return;
        }

        myPopup = popup;
        popup.addListener(new JBPopupListener() {
            @Override
            public void onClosed(LightweightWindowEvent closedEvent) {
                if (myPopup == popup) {
                    myPopup = null;
                }
                myPopupClosedAt = System.currentTimeMillis();
            }
        });
        popup.showBy(myButton, event.getInputDetails(), PopupPosition.TOP);
    }

    @RequiredUIAccess
    @Override
    public Component getComponent() {
        return myButton;
    }
}
