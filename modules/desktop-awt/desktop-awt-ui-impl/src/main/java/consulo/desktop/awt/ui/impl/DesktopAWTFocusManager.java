/*
 * Copyright 2013-2020 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.disposer.Disposable;
import consulo.ui.FocusManager;
import consulo.ui.event.GlobalFocusListener;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.beans.PropertyChangeListener;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 * @author VISTALL
 * @since 2020-11-21
 */
public class DesktopAWTFocusManager implements FocusManager {
  public static final DesktopAWTFocusManager ourInstance = new DesktopAWTFocusManager();

  private volatile Reference<Component> myLastFocused = new WeakReference<>(null);

  private DesktopAWTFocusManager() {
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("permanentFocusOwner", evt -> {
      if (evt.getNewValue() instanceof Component component) {
        myLastFocused = new WeakReference<>(component);
      }
    });
  }

  @Override
  public consulo.ui.@Nullable Component getFocusedComponent() {
    Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (owner == null) {
      owner = myLastFocused.get();
    }

    if (owner == null) {
      return null;
    }
    return owner instanceof FromSwingComponentWrapper wrapper ? wrapper.toUIComponent() : TargetAWT.wrap(owner);
  }

  @Override
  public Disposable addListener(GlobalFocusListener focusListener) {
    PropertyChangeListener listener = evt -> focusListener.focusChanged();
    KeyboardFocusManager keyboardFocusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    keyboardFocusManager.addPropertyChangeListener("focusOwner", listener);
    return () -> keyboardFocusManager.removePropertyChangeListener("focusOwner", listener);
  }
}
