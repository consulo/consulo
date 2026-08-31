/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.TaskInfo;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.NotificationType;
import consulo.ui.ex.popup.BalloonHandler;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Headless {@link StatusBarEx}: every method is a no-op/empty stub so no UI is created.
 *
 * @author VISTALL
 */
public class HeadlessStatusBar implements StatusBarEx {
    private final Project myProject;

    public HeadlessStatusBar(Project project) {
        myProject = project;
    }

    @Override
    public void updateWidget(String id) {
    }

    @Override
    public void updateWidget(Predicate<StatusBarWidget> widgetPredicate) {
    }

    @Override
    public <W extends StatusBarWidget> Optional<W> findWidget(Predicate<StatusBarWidget> predicate) {
        return Optional.empty();
    }

    @Override
    public void fireNotificationPopup(JComponent content, Color backgroundColor) {
    }

    @Override
    public StatusBar createChild() {
        return this;
    }

    @Override
    public StatusBar findChild(Component c) {
        return this;
    }

    @Override
    public IdeFrame getFrame() {
        return null;
    }

    @Override
    public void install(IdeFrame frame) {
    }

    @Override
    public @Nullable Project getProject() {
        return myProject;
    }

    @Override
    public BalloonHandler notifyProgressByBalloon(
        NotificationType type,
        String htmlBody,
        @Nullable Image icon,
        @Nullable HyperlinkListener listener
    ) {
        return null;
    }

    @Override
    public void startRefreshIndication(String tooltipText) {
    }

    @Override
    public void stopRefreshIndication() {
    }

    @Override
    public void addProgress(ProgressIndicator indicator, TaskInfo info) {
    }

    @Override
    public List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses() {
        return List.of();
    }

    @Override
    public void updateWidgets() {
    }

    @Override
    public boolean isProcessWindowOpen() {
        return false;
    }

    @Override
    public void setProcessWindowOpen(boolean open) {
    }

    @Override
    public Dimension getSize() {
        return new Dimension();
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public void addWidget(StatusBarWidget widget, List<String> order, Disposable parentDisposable) {
    }

    @Override
    public void removeWidget(String id) {
    }

    @Override
    public void dispose() {
    }
}
