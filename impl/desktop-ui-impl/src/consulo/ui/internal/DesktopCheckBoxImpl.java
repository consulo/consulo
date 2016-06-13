/*
 * Copyright 2013-2016 must-be.org
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

import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.ValueComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * @author VISTALL
 * @since 09-Jun-16
 */
public class DesktopCheckBoxImpl extends JCheckBox implements CheckBox {
  private static class ItemListenerImpl implements ItemListener {
    private ValueComponent.ValueListener<Boolean> myValueListener;

    public ItemListenerImpl(ValueComponent.ValueListener<Boolean> valueListener) {
      myValueListener = valueListener;
    }

    @Override
    public int hashCode() {
      return myValueListener.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof ItemListenerImpl && ((ItemListenerImpl)obj).myValueListener.equals(((ItemListenerImpl)obj).myValueListener);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
      if (e.getID() == ItemEvent.ITEM_STATE_CHANGED) {
        final CheckBox source = (CheckBox)e.getSource();
        myValueListener.valueChanged(new ValueEvent<Boolean>(source, source.getValue()));
      }
    }
  }

  public DesktopCheckBoxImpl(String text, boolean selected) {
    super(text, selected);
  }

  @NotNull
  @Override
  public Boolean getValue() {
    return isSelected();
  }

  @Override
  public void setValue(@NotNull Boolean value) {
    setSelected(value);
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @Override
  public void addValueListener(@NotNull ValueComponent.ValueListener<Boolean> valueListener) {
    addItemListener(new ItemListenerImpl(valueListener));

  }

  @Override
  public void removeValueListener(@NotNull ValueComponent.ValueListener<Boolean> valueListener) {
    removeItemListener(new ItemListenerImpl(valueListener));
  }
}
