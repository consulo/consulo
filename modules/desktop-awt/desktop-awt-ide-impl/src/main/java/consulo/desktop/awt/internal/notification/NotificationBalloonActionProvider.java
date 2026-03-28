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
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Rectangle2D;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.ModifiedInputDetails;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
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
    private JComponent mySettingButton;
    private JComponent myCloseButton;
    private List<JComponent> myActions;

    public NotificationBalloonActionProvider(
        BalloonImpl balloon,
        @Nullable Component repaintPanel,
        BalloonLayoutData layoutData,
        @Nullable String displayGroupId
    ) {
        myLayoutData = layoutData;
        myDisplayGroupId = displayGroupId;
        myBalloon = balloon;
        myRepaintPanel = repaintPanel;
    }

    @Override
    public List<JComponent> createActions() {
        myActions = new ArrayList<>();

        if (!myLayoutData.showSettingButton || myDisplayGroupId == null
            || !NotificationsConfigurationImpl.getInstanceImpl().isRegistered(myDisplayGroupId)) {
            mySettingButton = null;
        }
        else {
            Button settingsButton = Button.create(LocalizeValue.empty());
            settingsButton.setIcon(PlatformIconGroup.actionsMorevertical());
            settingsButton.addStyle(ButtonStyle.INPLACE);
            settingsButton.addClickListener(event -> myBalloon.runWithSmartFadeoutPause(() -> ShowSettingsUtil.getInstance().showAndSelect(
                myLayoutData.project,
                NotificationsConfigurable.class,
                notificationsConfigurable -> notificationsConfigurable.enableSearch(myDisplayGroupId).run()
            )));

            mySettingButton = (JComponent) TargetAWT.to(settingsButton);
            mySettingButton.setToolTipText("Notification Settings");
            mySettingButton.setOpaque(false);
            myActions.add(mySettingButton);

            if (myRepaintPanel != null) {
                myLayoutData.showActions = () -> {
                    for (JComponent action : myActions) {
                        if (!action.isShowing()) {
                            return Boolean.FALSE;
                        }
                    }
                    return Boolean.TRUE;
                };
            }
        }

        Button closeButton = Button.create(LocalizeValue.empty());
        closeButton.setIcon(PlatformIconGroup.ideNotificationClose());
        closeButton.addStyle(ButtonStyle.INPLACE);
        closeButton.addClickListener(event -> {
            InputDetails details = event.getInputDetails();
            boolean altPressed = details instanceof ModifiedInputDetails mid && mid.withAlt();
            //noinspection SSBasedInspection
            SwingUtilities.invokeLater(() -> {
                if (altPressed) {
                    myLayoutData.closeAll.run();
                }
                else {
                    myBalloon.hide();
                }
            });
        });

        myCloseButton = (JComponent) TargetAWT.to(closeButton);
        myCloseButton.setToolTipText("Close Notification (Alt+Click close all)");
        myCloseButton.setOpaque(false);
        myActions.add(myCloseButton);

        return myActions;
    }

    @Override
    public void layout(Rectangle2D bounds) {
        Dimension closeSize = myCloseButton.getPreferredSize();
        ImmutableInsets borderInsets = myBalloon.getShadowBorderImmutableInsets();
        int x = bounds.maxX() - borderInsets.right() - closeSize.width - myLayoutData.configuration.rightActionsOffset.width;
        int y = bounds.minY() + borderInsets.top() + myLayoutData.configuration.rightActionsOffset.height;
        myCloseButton.setBounds(x, y, closeSize.width, closeSize.height);

        if (mySettingButton != null) {
            Dimension size = mySettingButton.getPreferredSize();
            mySettingButton.setBounds(x - size.width - myLayoutData.configuration.gearCloseSpace, y, size.width, size.height);
        }
    }
}
