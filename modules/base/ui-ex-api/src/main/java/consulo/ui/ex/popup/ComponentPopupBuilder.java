// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.popup;

import consulo.application.util.function.Processor;
import consulo.component.ComponentManager;
import consulo.ui.ex.ActiveComponent;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.util.lang.Pair;
import consulo.util.lang.function.Condition;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

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
  @Nonnull
  ComponentPopupBuilder setTitle(String title);

  @Nonnull
  ComponentPopupBuilder setResizable(boolean forceResizable);

  @Nonnull
  ComponentPopupBuilder setMovable(boolean forceMovable);

  @Nonnull
  ComponentPopupBuilder setRequestFocus(boolean requestFocus);

  @Nonnull
  ComponentPopupBuilder setFocusable(boolean focusable);

  @Nonnull
  ComponentPopupBuilder setRequestFocusCondition(@Nonnull ComponentManager project, @Nonnull Condition<? super ComponentManager> condition);

  /**
   * @see consulo.ide.impl.idea.openapi.util.DimensionService
   */
  @Nonnull
  ComponentPopupBuilder setDimensionServiceKey(@Nullable ComponentManager project, @NonNls String key, boolean useForXYLocation);

  @Nonnull
  ComponentPopupBuilder setCancelCallback(@Nonnull Supplier<Boolean> shouldProceed);

  @Nonnull
  ComponentPopupBuilder setCancelOnClickOutside(boolean cancel);

  @Nonnull
  ComponentPopupBuilder addListener(@Nonnull JBPopupListener listener);

  @Nonnull
  ComponentPopupBuilder setCancelOnMouseOutCallback(@Nonnull MouseChecker shouldCancel);

  @Nonnull
  JBPopup createPopup();

  @Nonnull
  ComponentPopupBuilder setCancelButton(@Nonnull IconButton cancelButton);

  @Nonnull
  ComponentPopupBuilder setCancelOnOtherWindowOpen(boolean cancelOnWindow);

  @Nonnull
  ComponentPopupBuilder setTitleIcon(@Nonnull ActiveIcon icon);

  @Nonnull
  ComponentPopupBuilder setCancelKeyEnabled(boolean enabled);

  @Nonnull
  ComponentPopupBuilder setLocateByContent(boolean byContent);

  @Nonnull
  ComponentPopupBuilder setLocateWithinScreenBounds(boolean within);

  @Nonnull
  ComponentPopupBuilder setMinSize(Dimension minSize);

  /**
   * Use this method to customize shape of popup window (e.g. to use bounded corners).
   */
  @SuppressWarnings("UnusedDeclaration")//used in 'Presentation Assistant' plugin
  @Nonnull
  ComponentPopupBuilder setMaskProvider(MaskProvider maskProvider);

  @Nonnull
  ComponentPopupBuilder setAlpha(float alpha);

  @Nonnull
  ComponentPopupBuilder setBelongsToGlobalPopupStack(boolean isInStack);

  @Nonnull
  ComponentPopupBuilder setProject(ComponentManager project);

  @Nonnull
  ComponentPopupBuilder addUserData(Object object);

  @Nonnull
  ComponentPopupBuilder setModalContext(boolean modal);

  @Nonnull
  ComponentPopupBuilder setFocusOwners(@Nonnull Component[] focusOwners);

  /**
   * Adds "advertising" text to the bottom (e.g.: hints in code completion popup).
   */
  @Nonnull
  ComponentPopupBuilder setAdText(@Nullable String text);

  @Nonnull
  ComponentPopupBuilder setAdText(@Nullable String text, int textAlignment);

  @Nonnull
  ComponentPopupBuilder setShowShadow(boolean show);

  @Nonnull
  ComponentPopupBuilder setCommandButton(@Nonnull ActiveComponent commandButton);

  @Nonnull
  ComponentPopupBuilder setCouldPin(@Nullable Processor<? super JBPopup> callback);

  @Nonnull
  ComponentPopupBuilder setKeyboardActions(@Nonnull List<? extends Pair<ActionListener, KeyStroke>> keyboardActions);

  @Nonnull
  ComponentPopupBuilder setSettingButtons(@Nonnull Component button);

  @Nonnull
  ComponentPopupBuilder setMayBeParent(boolean mayBeParent);

  ComponentPopupBuilder setCancelOnWindowDeactivation(boolean cancelOnWindowDeactivation);

  /**
   * Allows to define custom strategy for processing {@link JBPopup#dispatchKeyEvent(KeyEvent)}.
   */
  @Nonnull
  ComponentPopupBuilder setKeyEventHandler(@Nonnull Predicate<? super KeyEvent> handler);

  @Nonnull
  ComponentPopupBuilder setShowBorder(boolean show);

  @Nonnull
  ComponentPopupBuilder setNormalWindowLevel(boolean b);

  @Nonnull
  default ComponentPopupBuilder setBorderColor(Color color) {
    return this;
  }

  /**
   * Set a handler to be called when popup is closed via {@link JBPopup#closeOk(InputEvent)}.
   */
  @Nonnull
  ComponentPopupBuilder setOkHandler(@Nullable Runnable okHandler);
}
