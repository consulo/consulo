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

import com.intellij.util.SmartList;
import consulo.ui.RequiredUIThread;
import consulo.ui.ValueComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class WGwtBooleanValueComponentImpl extends WGwtBaseComponent implements ValueComponent<Boolean> {
  private boolean mySelected;
  private List<ValueComponent.ValueListener<Boolean>> myValueListeners = new SmartList<ValueComponent.ValueListener<Boolean>>();

  public WGwtBooleanValueComponentImpl(boolean selected) {
    mySelected = selected;
  }

  @Override
  protected void getState(Map<String, Serializable> map) {
    super.getState(map);

    putIfNotDefault("selected", mySelected, true, map);
  }

  @Override
  public void invokeListeners(String type, Map<String, Serializable> variables) {
    if ("select".equals(type)) {
      mySelected = (Boolean)variables.get("selected");

      fireValueListeners();
    }
  }

  private void fireValueListeners() {
    ValueEvent<Boolean> event = new ValueEvent<Boolean>(this, getValue());
    for (ValueComponent.ValueListener<Boolean> valueListener : myValueListeners) {
      valueListener.valueChanged(event);
    }
  }

  @NotNull
  @Override
  public Boolean getValue() {
    return mySelected;
  }

  @Override
  @RequiredUIThread
  public void setValue(@Nullable final Boolean value) {
    if (value == null) {
      throw new IllegalArgumentException();
    }

    if (mySelected == value) {
      return;
    }

    mySelected = value;

    fireValueListeners();

    markAsChanged();
  }

  @Override
  public void addValueListener(@NotNull ValueComponent.ValueListener<Boolean> valueListener) {
    myValueListeners.add(valueListener);
  }

  @Override
  public void removeValueListener(@NotNull ValueComponent.ValueListener<Boolean> valueListener) {
    myValueListeners.remove(valueListener);
  }
}
