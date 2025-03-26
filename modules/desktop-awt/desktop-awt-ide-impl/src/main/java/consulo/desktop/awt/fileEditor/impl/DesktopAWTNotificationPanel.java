/*
 * Copyright 2013-2022 consulo.io
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
package consulo.desktop.awt.fileEditor.impl;

import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorColorsManager;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.internal.EditorNotificationBuilderEx;
import consulo.ide.impl.idea.ui.EditorNotificationPanel;
import consulo.language.editor.intention.IntentionActionWithOptions;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.HyperlinkAdapter;
import consulo.ui.ex.awt.HyperlinkLabel;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

/**
 * @author VISTALL
 * @since 18-Jul-22
 */
public class DesktopAWTNotificationPanel extends EditorNotificationPanel implements EditorNotificationBuilderEx {
    private final EditorColorsManager myEditorColorsManager;

    private EditorColorKey myBackgroundKey = EditorColors.NOTIFICATION_INFORMATION_BACKGROUND;

    public DesktopAWTNotificationPanel(EditorColorsManager editorColorsManager) {
        myEditorColorsManager = editorColorsManager;
    }

    @Override
    public Color getBackground() {
        if (myEditorColorsManager == null) {
            // init fix
            return super.getBackground();
        }
        
        ColorValue backColor = myEditorColorsManager.getGlobalScheme().getColor(myBackgroundKey);
        if (backColor != null) {
            return TargetAWT.to(backColor);
        }
        return super.getBackground();
    }

    @Nonnull
    @Override
    public EditorNotificationBuilder withText(@Nonnull LocalizeValue text) {
        setText(text.get());
        return this;
    }

    @Nonnull
    @Override
    public EditorNotificationBuilder withIcon(@Nonnull Image image) {
        icon(TargetAWT.to(image));
        return this;
    }

    @Nonnull
    @Override
    public EditorNotificationBuilder withType(@Nonnull NotificationType notificationType) {
        switch (notificationType) {
            case INFO:
                myBackgroundKey = EditorColors.NOTIFICATION_INFORMATION_BACKGROUND;
                break;
            case WARNING:
                myBackgroundKey = EditorColors.NOTIFICATION_WARNING_BACKGROUND;
                break;
            case ERROR:
                myBackgroundKey = EditorColors.NOTIFICATION_ERROR_BACKGROUND;
                break;
            default:
                throw new IllegalArgumentException("Can't set " + notificationType);
        }
        return this;
    }

    @Nonnull
    @Override
    public EditorNotificationBuilder withAction(@Nonnull LocalizeValue actionText, @Nonnull String actionRefId) {
        createActionLabel(actionText.get(), actionRefId);
        return this;
    }

    @Nonnull
    @Override
    public EditorNotificationBuilder withAction(@Nonnull LocalizeValue actionText,
                                                @Nonnull LocalizeValue actionTooltipText,
                                                @Nonnull ComponentEventListener<Component, ComponentEvent<Component>> action) {
        createActionLabel(actionText.get(), action).setToolTipText(StringUtil.nullize(actionTooltipText.get()));
        return this;
    }

    public HyperlinkLabel createActionLabel(final String text, @Nonnull ComponentEventListener<Component, ComponentEvent<Component>> action) {
        HyperlinkLabel label = new HyperlinkLabel(text, JBColor.BLUE, getBackground(), JBColor.BLUE);
        label.setOpaque(false);

        label.addHyperlinkListener(new HyperlinkAdapter() {
            @Override
            @RequiredUIAccess
            protected void hyperlinkActivated(HyperlinkEvent e) {
                action.onEvent(new consulo.ui.event.HyperlinkEvent(TargetAWT.wrap(label),
                    StringUtil.notNullize(e.getDescription()),
                    DesktopAWTInputDetails.convert(label, e.getInputEvent()))
                );
            }
        });
        myLinksPanel.add(label);
        return label;
    }

    @Nonnull
    @Override
    public EditorNotificationBuilder withGearAction(@Nonnull LocalizeValue tooltipText,
                                                    @Nonnull Image image,
                                                    @Nonnull ComponentEventListener<Component, ComponentEvent<Component>> action) {
        myGearButton.setIcon(image);
        myGearButton.setVisible(true);
        myGearButton.setToolTipText(tooltipText);
        myGearButton.addClickListener(action::onEvent);
        return this;
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        return this;
    }

    @Nullable
    @Override
    public IntentionActionWithOptions getIntentionAction() {
        return super.getIntentionAction();
    }
}
