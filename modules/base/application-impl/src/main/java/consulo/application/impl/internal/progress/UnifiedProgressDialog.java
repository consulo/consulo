/*
 * Copyright 2013-2020 consulo.io
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
package consulo.application.impl.internal.progress;

import consulo.application.Application;
import consulo.application.internal.ProgressDialog;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.Label;
import consulo.ui.Window;
import consulo.ui.*;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
public class UnifiedProgressDialog implements ProgressDialog {
    /**
     * Null while a project is being opened - the progress of the open itself belongs to no project yet, and
     * the field is only ever a way to reach the application.
     */
    private final @Nullable Project myProject;
    private ProgressWindow myProgressWindow;

    private Window myWindow;

    private Label myTextLabel;
    private Label myTextLabel2;
    private ProgressBar myProgressBar;
    private Button myCancelButton;

    /**
     * Every window access is scheduled on the ui thread, so a hide can be requested long before the scheduled show
     * has run. Without this flag such a hide is a no-op and the window opens afterwards with nobody left to close
     * it - on a modal ui backend that leaves the whole ui blocked.
     */
    private volatile boolean myHideRequested;

    public UnifiedProgressDialog(@Nullable Project project, ProgressWindow progressWindow) {
        myProject = project;
        myProgressWindow = progressWindow;
    }

    @Override
    public @Nullable UIAccess getUIAccess() {
        Window window = myWindow;
        return window == null ? null : window.getUIAccess();
    }

    private @Nullable UIAccess uiAccess() {
        if (myProject != null) {
            UIAccess projectAccess = myProject.getUIAccess();
            if (projectAccess.isValid()) {
                return projectAccess;
            }
        }

        return getUIAccess();
    }

    private void giveOnUI(Runnable runnable) {
        UIAccess uiAccess = uiAccess();
        if (uiAccess != null && uiAccess.isValid()) {
            uiAccess.give(runnable);
        }
    }

    @Override
    public void startBlocking(CompletableFuture<?> stopCondition, Predicate<AWTEvent> isCancellationEvent) {
        System.out.println("startBlocking");
    }

    @Override
    public void hide() {
        myHideRequested = true;

        giveOnUI(() -> {
            if (myWindow == null) {
                return;
            }

            myWindow.close();
            myWindow = null;
            myTextLabel = null;
            myTextLabel2 = null;
            myProgressBar = null;
            myCancelButton = null;
        });
    }

    @Override
    public void background() {
        System.out.println("background");
    }

    @Override
    public void update() {
        giveOnUI(() -> {
            if (myWindow == null) {
                return;
            }

            myTextLabel.setText(myProgressWindow.getText());
            myTextLabel2.setText(myProgressWindow.getText2());
            myProgressBar.setValue((int) (myProgressWindow.getFraction() * 100));
        });
    }

    @Override
    public void show() {
        giveOnUI(() -> {
            if (myHideRequested || myWindow != null) {
                return;
            }

            VerticalLayout verticalLayout = VerticalLayout.create();

            verticalLayout.add(myTextLabel = Label.create());

            DockLayout progressLayout = DockLayout.create();
            progressLayout.center(myProgressBar = ProgressBar.create());

            if (myProgressWindow.myShouldShowCancel) {
                myCancelButton = Button.create(CommonLocalize.buttonCancel(), event -> myProgressWindow.cancel());
                progressLayout.right(myCancelButton);
            }

            verticalLayout.add(progressLayout);
            verticalLayout.add(myTextLabel2 = Label.create());

            myWindow = Window.create(
                "",
                WindowOptions.builder().disableClose().disableResize().disableModal().build()
            );
            myWindow.setSize(new Size2D(288, 123));
            myWindow.setContent(verticalLayout);
            myWindow.show();
        });
    }

    @Override
    public void runRepaintRunnable() {
        update();
    }

    @Override
    public void changeCancelButtonText(LocalizeValue text) {
        giveOnUI(() -> {
            if (myCancelButton != null) {
                myCancelButton.setText(text);
            }
        });
    }

    @Override
    public void enableCancelButtonIfNeeded(boolean value) {
        giveOnUI(() -> {
            if (myCancelButton != null) {
                myCancelButton.setEnabled(value);
            }
        });
    }

    @Override
    public boolean isPopupWasShown() {
        return myWindow != null && myWindow.isActive();
    }

    @Override
    public void dispose() {
        hide();
    }
}
