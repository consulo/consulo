// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.popup;

import consulo.annotation.DeprecationInfo;
import consulo.component.ComponentManager;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author max
 */
public interface ComponentPopupBuilder {
    
    default ComponentPopupBuilder setTitle(LocalizeValue title) {
        setTitle(title.get());
        return this;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    
    ComponentPopupBuilder setTitle(String title);

    
    ComponentPopupBuilder setResizable(boolean forceResizable);

    
    ComponentPopupBuilder setMovable(boolean forceMovable);

    
    ComponentPopupBuilder setRequestFocus(boolean requestFocus);

    
    ComponentPopupBuilder setFocusable(boolean focusable);

    
    ComponentPopupBuilder setRequestFocusCondition(
        ComponentManager project,
        Predicate<? super ComponentManager> condition
    );

    /**
     * @see consulo.application.ui.DimensionService
     */
    ComponentPopupBuilder setDimensionServiceKey(@Nullable ComponentManager project, String key, boolean useForXYLocation);

    
    ComponentPopupBuilder setCancelCallback(Supplier<Boolean> shouldProceed);

    
    ComponentPopupBuilder setCancelOnClickOutside(boolean cancel);

    
    ComponentPopupBuilder addListener(JBPopupListener listener);

    
    ComponentPopupBuilder setCancelOnMouseOutCallback(MouseChecker shouldCancel);

    
    JBPopup createPopup();

    
    ComponentPopupBuilder setCancelButton(Image icon, LocalizeValue tooltipText);

    
    ComponentPopupBuilder setCancelOnOtherWindowOpen(boolean cancelOnWindow);

    
    ComponentPopupBuilder setCancelKeyEnabled(boolean enabled);

    
    ComponentPopupBuilder setLocateByContent(boolean byContent);

    
    ComponentPopupBuilder setLocateWithinScreenBounds(boolean within);

    
    ComponentPopupBuilder setMinSize(Dimension minSize);

    /**
     * Use this method to customize shape of popup window (e.g. to use bounded corners).
     */
    @SuppressWarnings("UnusedDeclaration")//used in 'Presentation Assistant' plugin
    
    ComponentPopupBuilder setMaskProvider(MaskProvider maskProvider);

    
    ComponentPopupBuilder setAlpha(float alpha);

    
    ComponentPopupBuilder setBelongsToGlobalPopupStack(boolean isInStack);

    
    ComponentPopupBuilder setProject(ComponentManager project);

    
    ComponentPopupBuilder addUserData(Object object);

    
    ComponentPopupBuilder setModalContext(boolean modal);

    
    ComponentPopupBuilder setFocusOwners(Component[] focusOwners);

    /**
     * Adds "advertising" text to the bottom (e.g.: hints in code completion popup).
     */
    ComponentPopupBuilder setAdText(@Nullable String text);

    
    ComponentPopupBuilder setAdText(@Nullable String text, int textAlignment);

    
    ComponentPopupBuilder setShowShadow(boolean show);

    
    ComponentPopupBuilder setCouldPin(@Nullable Predicate<? super JBPopup> callback);

    
    ComponentPopupBuilder setKeyboardActions(List<? extends Pair<ActionListener, KeyStroke>> keyboardActions);

    
    ComponentPopupBuilder setMayBeParent(boolean mayBeParent);

    ComponentPopupBuilder setCancelOnWindowDeactivation(boolean cancelOnWindowDeactivation);

    /**
     * Allows to define custom strategy for processing {@link JBPopup#dispatchKeyEvent(KeyEvent)}.
     */
    ComponentPopupBuilder setKeyEventHandler(Predicate<? super KeyEvent> handler);

    
    ComponentPopupBuilder setShowBorder(boolean show);

    
    ComponentPopupBuilder setNormalWindowLevel(boolean b);

    
    default ComponentPopupBuilder setBorderColor(Color color) {
        return this;
    }

    /**
     * Set a handler to be called when popup is closed via {@link JBPopup#closeOk(InputEvent)}.
     */
    ComponentPopupBuilder setOkHandler(@Nullable Runnable okHandler);

    
    ComponentPopupBuilder setHeaderLeftActions(List<? extends AnAction> actions);

    
    ComponentPopupBuilder setHeaderRightActions(List<? extends AnAction> actions);
}
