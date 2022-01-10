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

import com.intellij.ui.components.JBList;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ValueComponent;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
class DesktopValueListenerAsListSelectionListener<E> implements ListSelectionListener {
  private DesktopListBoxImpl<E> myBox;
  private JBList<E> myDesktopListBox;
  private ValueComponent.ValueListener<E> myValueListener;

  public DesktopValueListenerAsListSelectionListener(DesktopListBoxImpl<E> box, JBList<E> desktopListBox, ValueComponent.ValueListener<E> valueListener) {
    myBox = box;
    myDesktopListBox = desktopListBox;
    myValueListener = valueListener;
  }

  @Override
  public int hashCode() {
    return myValueListener.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof DesktopValueListenerAsListSelectionListener &&
           ((DesktopValueListenerAsListSelectionListener)obj).myValueListener.equals(((DesktopValueListenerAsListSelectionListener)obj).myValueListener);
  }

  @Override
  @RequiredUIAccess
  public void valueChanged(ListSelectionEvent e) {
    myValueListener.valueChanged(new ValueComponent.ValueEvent<>(myBox, myDesktopListBox.getSelectedValue()));
  }
}
