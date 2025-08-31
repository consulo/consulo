/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.desktop.awt.ui.keymap;

import consulo.ui.ex.action.AnAction;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.reference.SoftReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class KeyProcessorContext {
  private final List<AnAction> myActions = new ArrayList<AnAction>();
  private WeakReference<JComponent> myFoundComponent;
  private boolean myHasSecondStroke;

  private DataContext myDataContext;
  private boolean isModalContext;
  private WeakReference<Component> myFocusOwner;
  private KeyEvent myInputEvent;

  @Nonnull
  List<AnAction> getActions() {
    return myActions;
  }

  @Nullable
  public JComponent getFoundComponent() {
    return SoftReference.dereference(myFoundComponent);
  }

  public void setFoundComponent(JComponent foundComponent) {
    myFoundComponent = new WeakReference<JComponent>(foundComponent);
  }

  public void setHasSecondStroke(boolean hasSecondStroke) {
    myHasSecondStroke = hasSecondStroke;
  }

  public boolean isHasSecondStroke() {
    return myHasSecondStroke;
  }

  public DataContext getDataContext() {
    return myDataContext;
  }

  public void setDataContext(DataContext dataContext) {
    myDataContext = dataContext;
  }

  public boolean isModalContext() {
    return isModalContext;
  }

  public void setModalContext(boolean modalContext) {
    isModalContext = modalContext;
  }

  @Nullable
  public Component getFocusOwner() {
    return SoftReference.dereference(myFocusOwner);
  }

  public void setFocusOwner(Component focusOwner) {
    myFocusOwner = new WeakReference<Component>(focusOwner);
  }

  public void setInputEvent(KeyEvent e) {
    myInputEvent = e;
  }

  public KeyEvent getInputEvent() {
    return myInputEvent;
  }

  public void clear() {
    myInputEvent = null;
    myActions.clear();
    myFocusOwner = null;
    myDataContext = null;
    myFoundComponent = null;
  }
}