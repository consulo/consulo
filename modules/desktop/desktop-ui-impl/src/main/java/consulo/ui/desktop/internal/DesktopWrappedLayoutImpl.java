/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.desktop.internal;

import com.intellij.openapi.wm.IdeFocusManager;
import consulo.awt.TargetAWT;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.WrappedLayout;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 25-Oct-17
 */
public class DesktopWrappedLayoutImpl extends JPanel implements WrappedLayout, SwingWrapper {
  public DesktopWrappedLayoutImpl() {
    setLayout(new BorderLayout());
    setOpaque(false);
  }

  public DesktopWrappedLayoutImpl(JComponent wrapped) {
    setLayout(new BorderLayout());
    add(wrapped, BorderLayout.CENTER);
    setOpaque(false);
  }

  public DesktopWrappedLayoutImpl(LayoutManager layout, JComponent wrapped) {
    super(layout);
    add(wrapped);
    setOpaque(false);
  }

  public DesktopWrappedLayoutImpl(boolean isDoubleBuffered) {
    super(isDoubleBuffered);
    setOpaque(false);
  }

  public DesktopWrappedLayoutImpl(LayoutManager layout) {
    super(layout);
    setOpaque(false);
  }

  public DesktopWrappedLayoutImpl(LayoutManager layout, boolean isDoubleBuffered) {
    super(layout, isDoubleBuffered);
    setOpaque(false);
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public WrappedLayout set(@Nullable Component component) {
    setContent(component == null ? null : (JComponent)TargetAWT.to(component));
    return this;
  }

  public void setContent(JComponent wrapped) {
    if (wrapped == getTargetComponent()) {
      return;
    }

    removeAll();
    setLayout(new BorderLayout());
    if (wrapped != null) {
      add(wrapped, BorderLayout.CENTER);
    }
    validate();
  }

  @Override
  public void requestFocus() {
    if (getTargetComponent() == this) {
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(super::requestFocus);
      return;
    }
    IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(getTargetComponent());
  }

  @Override
  public boolean requestFocusInWindow() {
    if (getTargetComponent() == this) {
      return super.requestFocusInWindow();
    }
    return getTargetComponent().requestFocusInWindow();
  }

  @Override
  public final boolean requestFocus(boolean temporary) {
    if (getTargetComponent() == this) {
      return super.requestFocus(temporary);
    }
    return getTargetComponent().requestFocus(temporary);
  }

  public JComponent getTargetComponent() {
    if (getComponentCount() == 1) {
      return (JComponent)getComponent(0);
    }
    else {
      return this;
    }
  }
}
