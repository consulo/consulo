/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.event;

import consulo.ui.Component;
import consulo.ui.event.details.InputDetails;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2019-10-31
 */
public class UIEvent<T extends Component> {
  @Nonnull
  private final T myComponent;
  @Nullable
  private final InputDetails myInputDetails;

  private boolean myConsumed;

  public UIEvent(@Nonnull T component) {
    this(component, null);
  }

  public UIEvent(@Nonnull T component, @Nullable InputDetails inputDetails) {
    myComponent = component;
    myInputDetails = inputDetails;
  }

  @Nullable
  public InputDetails getInputDetails() {
    return myInputDetails;
  }

  @Nonnull
  public T getComponent() {
    return myComponent;
  }

  public boolean isConsumed() {
    return myConsumed;
  }

  public void consume() {
    myConsumed = true;
  }
}
