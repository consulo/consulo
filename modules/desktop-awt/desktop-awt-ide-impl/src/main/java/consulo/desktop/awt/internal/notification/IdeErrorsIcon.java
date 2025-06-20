// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.internal.notification;

import consulo.application.AllIcons;
import consulo.application.internal.MessagePool;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.ui.ex.awt.AnimatedIcon.Blinking;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import javax.swing.*;
import java.awt.*;

import static consulo.ui.ex.awt.EmptyIcon.ICON_16;

class IdeErrorsIcon extends JLabel {
    private final boolean myEnableBlink;

    IdeErrorsIcon(boolean enableBlink) {
        myEnableBlink = enableBlink;
    }

    void setState(MessagePool.State state) {
        Icon myUnreadIcon = !myEnableBlink ? TargetAWT.to(AllIcons.Ide.FatalError) : new Blinking(AllIcons.Ide.FatalError);
        if (state != null && state != MessagePool.State.NoErrors) {
            setIcon(state == MessagePool.State.ReadErrors ? TargetAWT.to(AllIcons.Ide.FatalError_read) : myUnreadIcon);
            setToolTipText(ExternalServiceLocalize.errorNotificationTooltip().get());
            if (!myEnableBlink) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }
        else {
            setIcon(ICON_16);
            setToolTipText(null);
            if (!myEnableBlink) {
                setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }
}