/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.notification;

import consulo.desktop.awt.ui.ImmutableInsets;
import consulo.desktop.awt.ui.popup.BalloonImpl;
import consulo.desktop.awt.uiOld.BalloonLayoutData;
import consulo.ide.impl.idea.notification.impl.NotificationsConfigurable;
import consulo.ide.impl.idea.notification.impl.NotificationsConfigurationImpl;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Rectangle2D;
import consulo.ui.ex.awt.JBRectangle;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexander Lobas
 */
public class NotificationBalloonActionProvider implements BalloonImpl.ActionProvider {
    private final BalloonImpl myBalloon;
    private final BalloonLayoutData myLayoutData;
    private final String myDisplayGroupId;
    private final Component myRepaintPanel;
    private BalloonImpl.ActionButton mySettingButton;
    private BalloonImpl.ActionButton myCloseButton;
    private List<BalloonImpl.ActionButton> myActions;

    private static final Rectangle CloseHoverBounds = new JBRectangle(5, 5, 12, 10);

    public NotificationBalloonActionProvider(
        @Nonnull BalloonImpl balloon,
        @Nullable Component repaintPanel,
        @Nonnull BalloonLayoutData layoutData,
        @Nullable String displayGroupId
    ) {
        myLayoutData = layoutData;
        myDisplayGroupId = displayGroupId;
        myBalloon = balloon;
        myRepaintPanel = repaintPanel;
    }

    @Nonnull
    @Override
    public List<BalloonImpl.ActionButton> createActions() {
        myActions = new ArrayList<>();

        if (!myLayoutData.showSettingButton || myDisplayGroupId == null
            || !NotificationsConfigurationImpl.getInstanceImpl().isRegistered(myDisplayGroupId)) {
            mySettingButton = null;
        }
        else {
            mySettingButton = myBalloon.new ActionButton(
                PlatformIconGroup.ideNotificationGear(),
                PlatformIconGroup.ideNotificationGearhover(),
                LocalizeValue.localizeTODO("Configure VcsBranchMappingChangedNotification"),
                event -> myBalloon.runWithSmartFadeoutPause(() -> ShowSettingsUtil.getInstance().showAndSelect(
                    myLayoutData.project,
                    NotificationsConfigurable.class,
                    notificationsConfigurable -> notificationsConfigurable.enableSearch(myDisplayGroupId).run()
                ))
            ) {
                @Override
                public void repaint() {
                    super.repaint();
                    if (myRepaintPanel != null) {
                        myRepaintPanel.repaint();
                    }
                }
            };
            myActions.add(mySettingButton);

            if (myRepaintPanel != null) {
                myLayoutData.showActions = () -> {
                    for (BalloonImpl.ActionButton action : myActions) {
                        if (!action.isShowing() || !action.hasPaint()) {
                            return Boolean.FALSE;
                        }
                    }
                    return Boolean.TRUE;
                };
            }
        }

        myCloseButton = myBalloon.new ActionButton(
            PlatformIconGroup.ideNotificationClose(),
            PlatformIconGroup.ideNotificationClosehover(),
            LocalizeValue.localizeTODO("Close VcsBranchMappingChangedNotification (Alt-Click close all notifications)"),
            event -> {
                int modifiers = event.getModifiers();
                //noinspection SSBasedInspection
                SwingUtilities.invokeLater(() -> {
                    if ((modifiers & InputEvent.ALT_MASK) != 0) {
                        myLayoutData.closeAll.run();
                    }
                    else {
                        myBalloon.hide();
                    }
                });
            }
        ) {
            @Override
            protected void paintIcon(@Nonnull Graphics g, @Nonnull Image icon) {
                TargetAWT.to(icon).paintIcon(this, g, CloseHoverBounds.x, CloseHoverBounds.y);
            }
        };
        myActions.add(myCloseButton);

        return myActions;
    }

    @Override
    public void layout(@Nonnull Rectangle2D bounds) {
        Dimension closeSize = myCloseButton.getPreferredSize();
        ImmutableInsets borderInsets = myBalloon.getShadowBorderImmutableInsets();
        int x = bounds.maxX() - borderInsets.right() - closeSize.width - myLayoutData.configuration.rightActionsOffset.width;
        int y = bounds.minY() + borderInsets.top() + myLayoutData.configuration.rightActionsOffset.height;
        myCloseButton.setBounds(
            x - CloseHoverBounds.x,
            y - CloseHoverBounds.y,
            closeSize.width + CloseHoverBounds.width,
            closeSize.height + CloseHoverBounds.height
        );

        if (mySettingButton != null) {
            Dimension size = mySettingButton.getPreferredSize();
            mySettingButton.setBounds(x - size.width - myLayoutData.configuration.gearCloseSpace, y, size.width, size.height);
        }
    }
}