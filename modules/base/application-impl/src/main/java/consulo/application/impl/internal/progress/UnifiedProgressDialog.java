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
import consulo.project.Project;
import consulo.ui.Label;
import consulo.ui.Window;
import consulo.ui.*;
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
    public void startBlocking(CompletableFuture<?> stopCondition, Predicate<AWTEvent> isCancellationEvent) {
        System.out.println("startBlocking");
    }

    @Override
    public void hide() {
        myHideRequested = true;

        Application.get().getLastUIAccess().give(() -> {
            if (myWindow == null) {
                return;
            }

            myWindow.close();
            myWindow = null;
            myTextLabel = null;
            myTextLabel2 = null;
            myProgressBar = null;
        });
    }

    @Override
    public void background() {
        System.out.println("background");
    }

    @Override
    public void update() {
        Application.get().getLastUIAccess().give(() -> {
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
        Application.get().getLastUIAccess().give(() -> {
            if (myHideRequested || myWindow != null) {
                return;
            }

            VerticalLayout verticalLayout = VerticalLayout.create();

            verticalLayout.add(myTextLabel = Label.create());
            verticalLayout.add(myProgressBar = ProgressBar.create());
            verticalLayout.add(myTextLabel2 = Label.create());

            myWindow = Window.create(
                Application.get().getName().get(),
                WindowOptions.builder().disableClose().disableResize().build()
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
        System.out.println("changeCancelButtonText=" + text);
    }

    @Override
    public void enableCancelButtonIfNeeded(boolean value) {
        //System.out.println("enableCancelButtonIfNeeded=" + value);
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
