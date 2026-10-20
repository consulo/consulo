/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.project.ui.wm;

import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * @author spleaner
 */
public interface StatusBar extends Disposable {
    Key<StatusBar> KEY = Key.create(StatusBar.class);

    /**
     * The id {@link #notifyByBalloon} takes for the progress area of the bar.
     */
    String PROGRESS_WIDGET_ID = "progress";

    void updateWidget(String id);

    void updateWidget(Predicate<StatusBarWidget> widgetPredicate);

    
    <W extends StatusBarWidget> Optional<W> findWidget(Predicate<StatusBarWidget> predicate);

    StatusBar createChild();

    StatusBar findChild(Component c);

    IdeFrame getFrame();

    void install(IdeFrame frame);

    @Nullable Project getProject();

    
    default consulo.ui.Component getUIComponent() {
        throw new AbstractMethodError();
    }

    @Deprecated
    @DeprecationInfo("AWT Dependency")
    default JComponent getComponent() {
        throw new AbstractMethodError();
    }

    /**
     * Shows a balloon pointing at the widget {@code widgetId}, replacing the one shown for it before. Nothing is shown
     * while the bar carries no such widget.
     */
    @RequiredUIAccess
    default void notifyByBalloon(String widgetId, NotificationType type, String htmlBody) {
        notifyByBalloon(widgetId, type, htmlBody, null, null);
    }

    @RequiredUIAccess
    void notifyByBalloon(
        String widgetId,
        NotificationType type,
        String htmlBody,
        @Nullable Image icon,
        @Nullable ComponentEventListener<consulo.ui.Component, HyperlinkEvent> linkListener
    );

    @RequiredUIAccess
    void hideWidgetBalloon(String widgetId);
}
