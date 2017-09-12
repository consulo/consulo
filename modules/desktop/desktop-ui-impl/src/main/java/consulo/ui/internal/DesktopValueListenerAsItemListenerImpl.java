/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.internal;

import consulo.ui.ValueComponent;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author VISTALL
 * @since 13-Jun-16
 */
class DesktopValueListenerAsItemListenerImpl<E> implements ItemListener {
  private ValueComponent.ValueListener<E> myValueListener;
  private boolean mySelectEvent;

  public DesktopValueListenerAsItemListenerImpl(ValueComponent.ValueListener<E> valueListener, boolean selectEvent) {
    myValueListener = valueListener;
    mySelectEvent = selectEvent;
  }

  @Override
  public int hashCode() {
    return myValueListener.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof DesktopValueListenerAsItemListenerImpl &&
           ((DesktopValueListenerAsItemListenerImpl)obj).myValueListener.equals(((DesktopValueListenerAsItemListenerImpl)obj).myValueListener);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void itemStateChanged(ItemEvent e) {
    if(mySelectEvent) {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        final ValueComponent source = (ValueComponent)e.getSource();
        myValueListener.valueChanged(new ValueComponent.ValueEvent<E>(source, (E)source.getValue()));
      }
    }
    else {
      if (e.getID() == ItemEvent.ITEM_STATE_CHANGED) {
        final ValueComponent source = (ValueComponent)e.getSource();
        myValueListener.valueChanged(new ValueComponent.ValueEvent<E>(source, (E)source.getValue()));
      }
    }
  }
}
