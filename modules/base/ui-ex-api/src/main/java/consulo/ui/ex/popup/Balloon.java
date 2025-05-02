/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ui.ex.popup;

import consulo.disposer.Disposable;
import consulo.ui.ex.LightweightWindow;
import consulo.ui.ex.PositionTracker;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.popup.event.JBPopupListener;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @see JBPopupFactory
 */
public interface Balloon extends Disposable, PositionTracker.Client<Balloon>, LightweightWindow {

    public static int getNormalInset() {
        return 3;
    }

    String KEY = "Balloon.property";

    void show(PositionTracker<Balloon> tracker, Position preferredPosition);

    void show(RelativePoint target, Position preferredPosition);

    void show(JLayeredPane pane);

    void showInCenterOf(JComponent component);

    Dimension getPreferredSize();

    void setBounds(Rectangle bounds);

    void addListener(@Nonnull JBPopupListener listener);

    void hide();

    void hide(boolean ok);

    void setAnimationEnabled(boolean enabled);

    boolean wasFadedIn();

    boolean wasFadedOut();

    boolean isDisposed();

    void setTitle(String title);

    default boolean isAnimationEnabled() {
        return false;
    }

    default boolean isBlockClicks() {
        return false;
    }

    default boolean isMovingForward(@Nonnull RelativePoint target) {
        return false;
    }

    default boolean isClickProcessor() {
        return isBlockClicks();
    }

    default JComponent getComponent() {
        throw new UnsupportedOperationException("Wrong Platform");
    }

    enum Position {
        below,
        above,
        atLeft,
        atRight
    }

    enum Layer {
        normal,
        top
    }
}
