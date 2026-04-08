// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui;

import consulo.ui.ex.awt.HyperlinkAdapter;
import consulo.webBrowser.BrowserUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

@ApiStatus.Internal
public final class ActionLinkListener extends HyperlinkAdapter {
    private static final String ACTION_EVENT_LINK_COMMAND = "perform";
    public static final String ERROR_ACTION_HREF = "ERROR_ACTION";

    private final JComponent myComponent;
    private @Nullable Action myAction;

    public ActionLinkListener(@Nonnull JComponent component) {
        myComponent = component;
        component.registerKeyboardAction(
            e -> {
                Action action = myAction;
                if (action != null) {
                    action.actionPerformed(e);
                }
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_FOCUSED
        );
    }

    public @Nullable Action getAction() {
        return myAction;
    }

    public void setAction(@Nullable Action action) {
        myAction = action;
    }

    @Override
    protected void hyperlinkActivated(@Nonnull HyperlinkEvent event) {
        if (ERROR_ACTION_HREF.equals(event.getDescription())) {
            ActionEvent actionEvent = new ActionEvent(myComponent, ActionEvent.ACTION_PERFORMED, ACTION_EVENT_LINK_COMMAND);
            Action action = myAction;
            if (action != null) {
                action.actionPerformed(actionEvent);
            }
        }
        else {
            BrowserUtil.browse(event.getDescription());
        }
    }
}
