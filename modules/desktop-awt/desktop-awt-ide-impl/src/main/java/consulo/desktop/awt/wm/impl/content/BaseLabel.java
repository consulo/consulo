/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.desktop.awt.wm.impl.content;

import consulo.dataContext.DataManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.toolWindow.ToolWindow;

import javax.accessibility.AccessibleContext;
import javax.swing.*;
import java.awt.*;

public class BaseLabel extends JLabel {
    protected DesktopToolWindowContentUi myUi;

    private Color myActiveFg;
    private Color myPassiveFg;

    public BaseLabel(DesktopToolWindowContentUi ui) {
        myUi = ui;
        setOpaque(false);

        DataManager.registerDataProvider(this, dataId -> {
            if (dataId == Content.KEY) {
                return getContent();
            }

            return null;
        });
    }

    @Override
    public void updateUI() {
        setActiveFg(JBColor.foreground());
        setPassiveFg(JBColor.foreground());
        super.updateUI();
    }

    @Override
    public Font getFont() {
        return UIUtil.getLabelFont();
    }

    public void setActiveFg(Color fg) {
        myActiveFg = fg;
    }

    public void setPassiveFg(Color passiveFg) {
        myPassiveFg = passiveFg;
    }

    @Override
    @RequiredUIAccess
    protected void paintComponent(Graphics g) {
        Color fore = myUi.myWindow.isActive() ? myActiveFg : myPassiveFg;
        setForeground(fore);
        super.paintComponent(g);
    }

    protected void updateTextAndIcon(Content content) {
        if (content == null) {
            setText(null);
            setIcon(null);
        }
        else {
            setText(content.getDisplayName());
            setActiveFg(myActiveFg);
            setPassiveFg(myPassiveFg);

            setToolTipText(content.getDescription());

            boolean show = Boolean.TRUE.equals(content.getUserData(ToolWindow.SHOW_CONTENT_ICON));
            if (show) {
                setIcon(TargetAWT.to(content.getIcon()));
            }
            else {
                setIcon(null);
            }
        }
    }

    public Content getContent() {
        return null;
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleBaseLabel();
        }
        return accessibleContext;
    }

    protected class AccessibleBaseLabel extends AccessibleJLabel {
    }
}
