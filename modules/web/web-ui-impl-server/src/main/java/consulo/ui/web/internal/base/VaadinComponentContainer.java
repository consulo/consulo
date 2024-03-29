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
package consulo.web.internal.ui.base;

import com.vaadin.shared.ui.AbstractComponentContainerState;
import com.vaadin.ui.AbstractComponentContainer;
import consulo.ui.Component;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-02-17
 */
public abstract class VaadinComponentContainer extends AbstractComponentContainer implements FromVaadinComponentWrapper, ComponentHolder {
  private Component myComponent;

  @Override
  public void setComponent(Component component) {
    myComponent = component;
  }

  @Override
  public void replaceComponent(com.vaadin.ui.Component oldComponent, com.vaadin.ui.Component newComponent) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Component toUIComponent() {
    return myComponent;
  }

  @Override
  public AbstractComponentContainerState getState() {
    return super.getState();
  }
}
