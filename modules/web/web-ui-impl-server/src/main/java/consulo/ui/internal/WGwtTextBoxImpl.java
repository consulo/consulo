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
package consulo.ui.internal;

import com.vaadin.ui.AbstractComponent;
import consulo.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.shared.Size;
import consulo.ui.TextBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
public class WGwtTextBoxImpl extends AbstractComponent implements TextBox, VaadinWrapper {
  public WGwtTextBoxImpl(String text) {
    getState().caption = text;
  }

  @Nullable
  @Override
  public Component getParentComponent() {
    return (Component)getParent();
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {

  }

  @Override
  public void addValueListener(@NotNull ValueListener<String> valueListener) {

  }

  @Override
  public void removeValueListener(@NotNull ValueListener<String> valueListener) {

  }

  @Override
  public String getValue() {
    return getState().caption;
  }

  @RequiredUIAccess
  @Override
  public void setValue(String value, boolean fireEvents) {
    getState().caption = value;

    markAsDirty();
  }
}
