/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.ui.popup;

import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.awt.*;
import java.awt.event.ActionListener;

/**
 * @see JBPopupFactory#createBalloonBuilder(javax.swing.JComponent)
 */
public interface BalloonBuilder {
  @Nonnull
  BalloonBuilder setBorderColor(@Nonnull Color color);

  @Nonnull
  BalloonBuilder setBorderInsets(@Nullable Insets insets);

  @Nonnull
  BalloonBuilder setFillColor(@Nonnull Color color);

  @Nonnull
  BalloonBuilder setHideOnClickOutside(boolean hide);

  @Nonnull
  BalloonBuilder setHideOnKeyOutside(boolean hide);

  @Nonnull
  BalloonBuilder setShowCallout(boolean show);

  @Nonnull
  BalloonBuilder setCloseButtonEnabled(boolean enabled);

  @Nonnull
  BalloonBuilder setFadeoutTime(long fadeoutTime);

  @Nonnull
  BalloonBuilder setAnimationCycle(int time);

  @Nonnull
  BalloonBuilder setHideOnFrameResize(boolean hide);

  @Nonnull
  BalloonBuilder setHideOnLinkClick(boolean hide);

  @Nonnull
  BalloonBuilder setClickHandler(ActionListener listener, boolean closeOnClick);

  @Nonnull
  BalloonBuilder setCalloutShift(int length);

  @Nonnull
  BalloonBuilder setPositionChangeXShift(int positionChangeXShift);

  @Nonnull
  BalloonBuilder setPositionChangeYShift(int positionChangeYShift);

  @Nonnull
  BalloonBuilder setHideOnAction(boolean hideOnAction);

  @Nonnull
  BalloonBuilder setDialogMode(boolean dialogMode);

  @Nonnull
  BalloonBuilder setTitle(@Nullable String title);

  @Nonnull
  BalloonBuilder setContentInsets(Insets insets);

  @Nonnull
  BalloonBuilder setShadow(boolean shadow);

  @Nonnull
  BalloonBuilder setSmallVariant(boolean smallVariant);

  @Nonnull
  BalloonBuilder setLayer(Balloon.Layer layer);

  @Nonnull
  BalloonBuilder setBlockClicksThroughBalloon(boolean block);

  @Nonnull
  BalloonBuilder setRequestFocus(boolean requestFocus);

  @Nonnull
  default BalloonBuilder setPointerSize(Dimension size) {
    return this;
  }

  default BalloonBuilder setCornerToPointerDistance(int distance) {
    return this;
  }

  BalloonBuilder setHideOnCloseClick(boolean hideOnCloseClick);

  /**
   * Links target balloon life cycle to the given object. I.e. current balloon will be auto-hide and collected as soon
   * as given anchor is disposed.
   * <p>
   * <b>Note:</b> given disposable anchor is assumed to correctly implement {@link #hashCode()} and {@link #equals(Object)}.
   *
   * @param anchor target anchor to link to
   * @return balloon builder which produces balloon linked to the given object life cycle
   */
  @Nonnull
  BalloonBuilder setDisposable(@Nonnull Disposable anchor);

  @Nonnull
  Balloon createBalloon();
}
