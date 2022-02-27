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
package com.intellij.ui.popup;

import consulo.application.ApplicationManager;
import consulo.ide.ui.popup.*;
import consulo.project.Project;
import consulo.application.util.function.Computable;
import consulo.util.lang.function.Condition;
import com.intellij.openapi.util.Pair;
import consulo.ui.ex.ActiveComponent;
import com.intellij.util.BooleanFunction;
import consulo.application.util.function.Processor;
import consulo.disposer.Disposer;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

/**
 * @author anna
 */
public class ComponentPopupBuilderImpl implements ComponentPopupBuilder {
  private String myTitle = "";
  private boolean myResizable;
  private boolean myMovable;
  private final JComponent myComponent;
  private final JComponent myPreferredFocusedComponent;
  private boolean myRequestFocus;
  private String myDimensionServiceKey;
  private Computable<Boolean> myCallback;
  private Project myProject;
  private boolean myCancelOnClickOutside = true;
  private boolean myCancelOnWindowDeactivation = true;
  private final Set<JBPopupListener> myListeners = new LinkedHashSet<>();
  private boolean myUseDimServiceForXYLocation;

  private IconButton myCancelButton;
  private MouseChecker myCancelOnMouseOutCallback;
  private boolean myCancelOnWindow;
  private ActiveIcon myTitleIcon = new ActiveIcon(Image.empty(0));
  private boolean myCancelKeyEnabled = true;
  private boolean myLocateByContent;
  private boolean myPlaceWithinScreen = true;
  private Processor<? super JBPopup> myPinCallback;
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
  private ActiveComponent myCommandButton;
  private List<? extends consulo.util.lang.Pair<ActionListener, KeyStroke>> myKeyboardActions = Collections.emptyList();
  private Component mySettingsButtons;
  private boolean myMayBeParent;
  private int myAdAlignment = SwingConstants.LEFT;
  private Predicate<? super KeyEvent> myKeyEventHandler;
  private Color myBorderColor;
  private boolean myNormalWindowLevel;
  private
  @Nullable
  Runnable myOkHandler;

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
  public ComponentPopupBuilder setDimensionServiceKey(final Project project, final String key, final boolean useForXYLocation) {
    myDimensionServiceKey = key;
    myUseDimServiceForXYLocation = useForXYLocation;
    myProject = project;
    return this;
  }

  @Override
  @Nonnull
  public ComponentPopupBuilder setCancelCallback(@Nonnull final Computable<Boolean> shouldProceed) {
    myCallback = shouldProceed;
    return this;
  }

  @Override
  @Nonnull
  public ComponentPopupBuilder setCancelButton(@Nonnull final IconButton cancelButton) {
    myCancelButton = cancelButton;
    return this;
  }

  @Override
  @Nonnull
  public ComponentPopupBuilder setCommandButton(@Nonnull ActiveComponent button) {
    myCommandButton = button;
    return this;
  }

  @Override
  @Nonnull
  public ComponentPopupBuilder setCouldPin(@Nullable final Processor<? super JBPopup> callback) {
    myPinCallback = callback;
    return this;
  }

  @Override
  @Nonnull
  public ComponentPopupBuilder setKeyboardActions(@Nonnull List<? extends consulo.util.lang.Pair<ActionListener, KeyStroke>> keyboardActions) {
    myKeyboardActions = keyboardActions;
    return this;
  }

  @Override
  @Nonnull
  public ComponentPopupBuilder setSettingButtons(@Nonnull Component button) {
    mySettingsButtons = button;
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
  public ComponentPopupBuilder setProject(Project project) {
    myProject = project;
    return this;
  }

  @Override
  @Nonnull
  public JBPopup createPopup() {
    AbstractPopup popup = new AbstractPopup()
            .init(myProject, myComponent, myPreferredFocusedComponent, myRequestFocus, myFocusable, myMovable, myDimensionServiceKey, myResizable, myTitle, myCallback, myCancelOnClickOutside,
                  myListeners, myUseDimServiceForXYLocation, myCommandButton, myCancelButton, myCancelOnMouseOutCallback, myCancelOnWindow, myTitleIcon, myCancelKeyEnabled, myLocateByContent,
                  myPlaceWithinScreen, myMinSize, myAlpha, myMaskProvider, myInStack, myModalContext, myFocusOwners, myAd, myAdAlignment, false, myKeyboardActions, mySettingsButtons, myPinCallback,
                  myMayBeParent, myShowShadow, myShowBorder, myBorderColor, myCancelOnWindowDeactivation, myKeyEventHandler);

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
  public ComponentPopupBuilder setRequestFocusCondition(@Nonnull Project project, @Nonnull Condition<? super Project> condition) {
    myRequestFocus = condition.value(project);
    return this;
  }

  @Override
  @Nonnull
  public ComponentPopupBuilder setTitleIcon(@Nonnull final ActiveIcon icon) {
    myTitleIcon = icon;
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
}
