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
package consulo.ui.web.internal;

import consulo.ui.web.internal.base.VaadinComponent;
import consulo.web.gwt.shared.ui.state.checkbox.CheckBoxRpc;
import consulo.web.gwt.shared.ui.state.checkbox.CheckBoxState;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-02-19
 */
public abstract class VaadinBooleanValueComponentBase extends VaadinComponent {
  private final CheckBoxRpc myRpc = value -> toUIComponent().setValueImpl(value, true);

  public VaadinBooleanValueComponentBase() {
    registerRpc(myRpc);
  }

  @Override
  public CheckBoxState getState() {
    return (CheckBoxState)super.getState();
  }

  @Nonnull
  @Override
  public WebBooleanValueComponentBase toUIComponent() {
    return (WebBooleanValueComponentBase)super.toUIComponent();
  }
}
