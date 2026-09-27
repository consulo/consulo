// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.desktop.awt.codeInsight.intention.impl;

import consulo.codeEditor.markup.GutterMark;
import consulo.ide.impl.idea.ui.EditorNotificationPanel;
import consulo.language.editor.impl.internal.daemon.FileLevelHighlightComponentBuilder;
import consulo.language.editor.intention.IntentionAction;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ComponentContainer;
import consulo.ui.ex.awt.LightColors;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import java.awt.*;

/**
 * @author max
 */
public class FileLevelIntentionComponent extends EditorNotificationPanel implements ComponentContainer {
    @RequiredUIAccess
    public FileLevelIntentionComponent(FileLevelHighlightComponentBuilder builder) {
        super(getColor(builder.getNotificationType()));

        for (IntentionAction action : builder.getIntentionActions()) {
            LocalizeValue text = action.getText();
            createActionLabel(text.get(), () -> builder.invokeIntention(action, text));
        }

        myLabel.setText(builder.getDescription().get());
        LocalizeValue tooltip = builder.getTooltip();
        if (tooltip.isNotEmpty()) {
            myLabel.setToolTipText(tooltip.get());
        }
        GutterMark gutterMark = builder.getGutterMark();
        if (gutterMark != null) {
            myLabel.setIcon(TargetAWT.to(gutterMark.getIcon()));
        }

        if (builder.hasIntentions()) {
            myGearButton.setVisible(true);
            myGearButton.setIcon(PlatformIconGroup.generalGearplain());
            myGearButton.addClickListener(event -> builder.showIntentionOptions(event.getComponent(), event.getInputDetails()));
        }
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void dispose() {
    }

    private static Color getColor(NotificationType notificationType) {
        return switch (notificationType) {
            case ERROR -> LightColors.RED;
            case WARNING -> LightColors.YELLOW;
            default -> LightColors.GREEN;
        };
    }
}
