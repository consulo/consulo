/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.popup;

import consulo.application.ApplicationManager;
import consulo.component.ComponentManager;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.popup.*;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import consulo.util.lang.function.Condition;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * @author anna
 */
public class ComponentPopupBuilderImpl implements ComponentPopupBuilder {
    public record CancelButtonInfo(Image icon, LocalizeValue tooltipText) {
    }

    private String myTitle = "";
    private boolean myResizable;
    private boolean myMovable;
    private final JComponent myComponent;
    private final JComponent myPreferredFocusedComponent;
    private boolean myRequestFocus;
    private String myDimensionServiceKey;
    private Supplier<Boolean> myCallback;
    private Project myProject;
    private boolean myCancelOnClickOutside = true;
    private boolean myCancelOnWindowDeactivation = true;
    private final Set<JBPopupListener> myListeners = new LinkedHashSet<>();
    private boolean myUseDimServiceForXYLocation;

    private CancelButtonInfo myCancelButtonInfo;

    private MouseChecker myCancelOnMouseOutCallback;
    private boolean myCancelOnWindow;
    private ActiveIcon myTitleIcon = new ActiveIcon(Image.empty(0));
    private boolean myCancelKeyEnabled = true;
    private boolean myLocateByContent;
    private boolean myPlaceWithinScreen = true;
    private Predicate<? super JBPopup> myPinCallback;
    private Dimension myMinSize;
    private MaskProvider myMaskProvider;
    private float myAlpha;
    private List<Object> myUserData;

    private boolean myInStack = true;
    private boolean myModalContext = true;
    private Component[] myFocusOwners = new Component[0];

    private String myAd;
    private boolean myShowShadow = true;
    private boolean myShowBorder = true;
    private boolean myFocusable = true;
    private List<? extends Pair<ActionListener, KeyStroke>> myKeyboardActions = Collections.emptyList();
    private boolean myMayBeParent;
    private int myAdAlignment = SwingConstants.LEFT;
    private Predicate<? super KeyEvent> myKeyEventHandler;
    private Color myBorderColor;
    private boolean myNormalWindowLevel;
    @Nullable
    private Runnable myOkHandler;

    private List<AnAction> myHeaderLeftActions = List.of();
    private List<AnAction> myHeaderRightActions = List.of();

    public ComponentPopupBuilderImpl(@Nonnull JComponent component, JComponent preferredFocusedComponent) {
        myComponent = component;
        myPreferredFocusedComponent = preferredFocusedComponent;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setMayBeParent(boolean mayBeParent) {
        myMayBeParent = mayBeParent;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setTitle(String title) {
        myTitle = title;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setResizable(final boolean resizable) {
        myResizable = resizable;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setMovable(final boolean movable) {
        myMovable = movable;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setCancelOnClickOutside(final boolean cancel) {
        myCancelOnClickOutside = cancel;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setCancelOnMouseOutCallback(@Nonnull final MouseChecker shouldCancel) {
        myCancelOnMouseOutCallback = shouldCancel;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder addListener(@Nonnull final JBPopupListener listener) {
        myListeners.add(listener);
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setRequestFocus(final boolean requestFocus) {
        myRequestFocus = requestFocus;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setFocusable(final boolean focusable) {
        myFocusable = focusable;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setDimensionServiceKey(final ComponentManager project, final String key, final boolean useForXYLocation) {
        myDimensionServiceKey = key;
        myUseDimServiceForXYLocation = useForXYLocation;
        myProject = (Project)project;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setCancelCallback(@Nonnull final Supplier<Boolean> shouldProceed) {
        myCallback = shouldProceed;
        return this;
    }

    @Nonnull
    @Override
    public ComponentPopupBuilder setCancelButton(@Nonnull Image icon, @Nonnull LocalizeValue tooltipText) {
        myCancelButtonInfo = new CancelButtonInfo(icon, tooltipText);
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setCouldPin(@Nullable final Predicate<? super JBPopup> callback) {
        myPinCallback = callback;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setKeyboardActions(@Nonnull List<? extends Pair<ActionListener, KeyStroke>> keyboardActions) {
        myKeyboardActions = keyboardActions;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setCancelOnOtherWindowOpen(final boolean cancelOnWindow) {
        myCancelOnWindow = cancelOnWindow;
        return this;
    }

    @Override
    public ComponentPopupBuilder setCancelOnWindowDeactivation(boolean cancelOnWindowDeactivation) {
        myCancelOnWindowDeactivation = cancelOnWindowDeactivation;
        return this;
    }

    @Nonnull
    @Override
    public ComponentPopupBuilder setKeyEventHandler(@Nonnull Predicate<? super KeyEvent> handler) {
        myKeyEventHandler = handler;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setProject(ComponentManager project) {
        myProject = (Project)project;
        return this;
    }

    @Override
    @Nonnull
    public JBPopup createPopup() {
        AbstractPopup popup = new AbstractPopup().init(
            myProject,
            myComponent,
            myPreferredFocusedComponent,
            myRequestFocus,
            myFocusable,
            myMovable,
            myDimensionServiceKey,
            myResizable,
            myTitle,
            myCallback,
            myCancelOnClickOutside,
            myListeners,
            myUseDimServiceForXYLocation,
            myCancelButtonInfo,
            myCancelOnMouseOutCallback,
            myCancelOnWindow,
            myTitleIcon,
            myCancelKeyEnabled,
            myLocateByContent,
            myPlaceWithinScreen,
            myMinSize,
            myAlpha,
            myMaskProvider,
            myInStack,
            myModalContext,
            myFocusOwners,
            myAd,
            myAdAlignment,
            false,
            myKeyboardActions,
            myPinCallback,
            myMayBeParent,
            myShowShadow,
            myShowBorder,
            myBorderColor,
            myCancelOnWindowDeactivation,
            myKeyEventHandler,
            myHeaderLeftActions,
            myHeaderRightActions
        );

        popup.setNormalWindowLevel(myNormalWindowLevel);
        popup.setOkHandler(myOkHandler);

        if (myUserData != null) {
            popup.setUserData(myUserData);
        }
        Disposer.register(ApplicationManager.getApplication(), popup);
        return popup;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setRequestFocusCondition(
        @Nonnull ComponentManager project,
        @Nonnull Predicate<? super ComponentManager> condition
    ) {
        myRequestFocus = condition.test(project);
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setCancelKeyEnabled(final boolean enabled) {
        myCancelKeyEnabled = enabled;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setLocateByContent(final boolean byContent) {
        myLocateByContent = byContent;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setLocateWithinScreenBounds(final boolean within) {
        myPlaceWithinScreen = within;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setMinSize(final Dimension minSize) {
        myMinSize = minSize;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setMaskProvider(MaskProvider maskProvider) {
        myMaskProvider = maskProvider;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setAlpha(final float alpha) {
        myAlpha = alpha;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setBelongsToGlobalPopupStack(final boolean isInStack) {
        myInStack = isInStack;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder addUserData(final Object object) {
        if (myUserData == null) {
            myUserData = new ArrayList<>();
        }
        myUserData.add(object);
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setModalContext(final boolean modal) {
        myModalContext = modal;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setFocusOwners(@Nonnull final Component[] focusOwners) {
        myFocusOwners = focusOwners;
        return this;
    }

    @Override
    @Nonnull
    public ComponentPopupBuilder setAdText(@Nullable final String text) {
        return setAdText(text, SwingConstants.LEFT);
    }

    @Nonnull
    @Override
    public ComponentPopupBuilder setAdText(@Nullable String text, int textAlignment) {
        myAd = text;
        myAdAlignment = textAlignment;
        return this;
    }

    @Nonnull
    @Override
    public ComponentPopupBuilder setShowShadow(boolean show) {
        myShowShadow = show;
        return this;
    }

    @Nonnull
    @Override
    public ComponentPopupBuilder setShowBorder(boolean show) {
        myShowBorder = show;
        return this;
    }

    @Nonnull
    @Override
    public ComponentPopupBuilder setNormalWindowLevel(boolean b) {
        myNormalWindowLevel = b;
        return this;
    }

    @Nonnull
    @Override
    public ComponentPopupBuilder setBorderColor(Color color) {
        myBorderColor = color;
        return this;
    }

    @Nonnull
    @Override
    public ComponentPopupBuilder setOkHandler(@Nullable Runnable okHandler) {
        myOkHandler = okHandler;
        return this;
    }

    @Nonnull
    @Override
    public ComponentPopupBuilder setHeaderLeftActions(@Nonnull List<? extends AnAction> headerLeftActions) {
        if (myHeaderLeftActions.isEmpty()) {
            myHeaderLeftActions = new ArrayList<>();
        }
        myHeaderLeftActions.addAll(headerLeftActions);
        return this;
    }

    @Nonnull
    @Override
    public ComponentPopupBuilder setHeaderRightActions(@Nonnull List<? extends AnAction> headerRightActions) {
        if (myHeaderRightActions.isEmpty()) {
            myHeaderRightActions = new ArrayList<>();
        }
        myHeaderRightActions.addAll(headerRightActions);
        return this;
    }
}
