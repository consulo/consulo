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
package consulo.ui.ex.impl.internal.popup;

import consulo.component.ComponentManager;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.popup.ComponentPopupBuilder;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.MaskProvider;
import consulo.ui.ex.popup.MouseChecker;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Accepts a configuration and cannot build from it - the content handed over is a swing component, which the
 * unified frontends have no window for yet.
 * <p>
 * The builder exists anyway because configuring is not showing. A help tooltip builds and configures its popup
 * builder the moment it is installed on a component and only creates the popup when the pointer rests on it -
 * install runs on every status bar widget update, once per keystroke, and a factory that refuses at the first
 * step turns each of those updates into a logged failure. So the configuration is taken and dropped, and it is
 * {@link #createPopup()} that still names the missing piece.
 *
 * @author VISTALL
 */
public class UnifiedComponentPopupBuilder implements ComponentPopupBuilder {
    @Override
    public ComponentPopupBuilder setTitle(String title) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setResizable(boolean forceResizable) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setMovable(boolean forceMovable) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setRequestFocus(boolean requestFocus) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setFocusable(boolean focusable) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setRequestFocusCondition(ComponentManager project, Predicate<? super ComponentManager> condition) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setDimensionServiceKey(@Nullable ComponentManager project, String key, boolean useForXYLocation) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setCancelCallback(Supplier<Boolean> shouldProceed) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setCancelOnClickOutside(boolean cancel) {
        return this;
    }

    @Override
    public ComponentPopupBuilder addListener(JBPopupListener listener) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setCancelOnMouseOutCallback(MouseChecker shouldCancel) {
        return this;
    }

    @Override
    public JBPopup createPopup() {
        throw new UnsupportedOperationException("swing content cannot be shown on the unified frontends");
    }

    @Override
    public ComponentPopupBuilder setCancelButton(Image icon, LocalizeValue tooltipText) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setCancelOnOtherWindowOpen(boolean cancelOnWindow) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setCancelKeyEnabled(boolean enabled) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setLocateByContent(boolean byContent) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setLocateWithinScreenBounds(boolean within) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setMinSize(Dimension minSize) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setMaskProvider(MaskProvider maskProvider) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setAlpha(float alpha) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setBelongsToGlobalPopupStack(boolean isInStack) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setProject(ComponentManager project) {
        return this;
    }

    @Override
    public ComponentPopupBuilder addUserData(Object object) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setModalContext(boolean modal) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setFocusOwners(Component[] focusOwners) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setAdText(@Nullable String text) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setAdText(@Nullable String text, int textAlignment) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setShowShadow(boolean show) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setCouldPin(@Nullable Predicate<? super JBPopup> callback) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setKeyboardActions(List<? extends Pair<ActionListener, KeyStroke>> keyboardActions) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setMayBeParent(boolean mayBeParent) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setCancelOnWindowDeactivation(boolean cancelOnWindowDeactivation) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setKeyEventHandler(Predicate<? super KeyEvent> handler) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setShowBorder(boolean show) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setNormalWindowLevel(boolean b) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setOkHandler(@Nullable Runnable okHandler) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setHeaderLeftActions(List<? extends AnAction> actions) {
        return this;
    }

    @Override
    public ComponentPopupBuilder setHeaderRightActions(List<? extends AnAction> actions) {
        return this;
    }
}
