/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.wm.statusBar;

import consulo.application.Application;
import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.FocusManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.toolWindow.ToolWindowSettings;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2023-11-13
 */
public class BaseToolWindowsSwitcher implements Disposable, UISettingsListener {
    private final StatusBar myStatusBar;

    private @Nullable ToolWindowSettings mySettings;

    protected JBPopup popup;
    protected boolean wasExited = false;
    protected Button myButton;

    @RequiredUIAccess
    public BaseToolWindowsSwitcher(StatusBar statusBar) {
        myStatusBar = statusBar;

        myButton = Button.create(LocalizeValue.empty());
        myButton.addStyle(ButtonStyle.TOOLBAR);
        myButton.addClickListener(event -> performAction());
        myButton.setFocusable(false);

        Disposer.register(this, FocusManager.get().addListener(this::update));

        Application.get().getMessageBus().connect(this).subscribe(UISettingsListener.class, this);
    }

    @Override
    public void uiSettingsChanged(UISettings uiSettings) {
        update();
    }

    public void performAction() {
        ToolWindowSettings settings = settings();
        if (settings != null) {
            settings.setHideToolStripes(!settings.isHideToolStripes());
            UISettings.getInstance().fireUISettingsChanged();
        }
    }

    /**
     * A frame hands its status bar a project only after the bar is built, so the settings cannot be resolved in the
     * constructor. Once resolved they are kept - looking them up on every update would ask a project which may be
     * gone by then, and a disposed container answers a lookup by throwing.
     */
    private @Nullable ToolWindowSettings settings() {
        if (mySettings == null) {
            Project project = myStatusBar == null ? null : myStatusBar.getProject();
            if (project != null && !project.isDisposed()) {
                mySettings = ToolWindowSettings.getInstance(project);
            }
        }
        return mySettings;
    }

    @RequiredUIAccess
    public void update() {
        myButton.setToolTipText(LocalizeValue.empty());
        ToolWindowSettings settings = settings();
        if (settings != null) {
            boolean changes = false;

            if (!myButton.isVisible()) {
                myButton.setVisible(true);
                changes = true;
            }

            Image icon = settings.isHideToolStripes()
                ? PlatformIconGroup.generalTbshown()
                : PlatformIconGroup.generalTbhidden();
            if (icon != myButton.getIcon()) {
                myButton.setIcon(icon);
                changes = true;
            }

            if (changes) {
                myButton.forceRepaint();
            }
        }
        else {
            myButton.setVisible(false);
            myButton.setToolTipText(LocalizeValue.empty());
        }
    }

    public boolean isActive() {
        return settings() != null;
    }


    public Component getUIComponent() {
        return myButton;
    }

    @Override
    public void dispose() {
        Disposer.dispose(this);
        popup = null;
    }
}
