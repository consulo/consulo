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
package consulo.ui;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 14-Jun-16
 */
public class BooleanValueGroup implements ValueComponent.ValueListener<Boolean> {
  private List<ValueComponent<Boolean>> myComponents = new ArrayList<ValueComponent<Boolean>>();

  @NotNull
  public BooleanValueGroup add(@NotNull ValueComponent<Boolean> component) {
    myComponents.add(component);

    component.addValueListener(this);

    return this;
  }

  @Override
  @RequiredUIAccess
  @SuppressWarnings("unchecked")
  public void valueChanged(@NotNull ValueComponent.ValueEvent<Boolean> event) {
    final Boolean value = event.getValue();
    if (value) {
      final Component selectComponent = event.getComponent();

      for (ValueComponent<Boolean> component : myComponents) {
        if (component == selectComponent) {
          continue;
        }
        setValueNoListener(component, false);
      }
    }
    else {
      // we can't set false
      final ValueComponent<Boolean> component = (ValueComponent<Boolean>)event.getComponent();
      setValueNoListener(component, true);
    }
  }

  private void setValueNoListener(ValueComponent<Boolean> component, boolean value) {
    component.removeValueListener(this);
    component.setValue(value);
    component.addValueListener(this);
  }
}
