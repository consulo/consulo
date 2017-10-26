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

import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Component;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.WrappedLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

/**
 * @author VISTALL
 * @since 26-Oct-17
 */
public class WGwtWrappedLayoutImpl extends AbstractComponentContainer implements WrappedLayout, VaadinWrapper {
  private Component myComponent;

  @Override
  public void remove(@NotNull consulo.ui.Component component) {
    if (myComponent == component) {
      removeComponent(myComponent);
      myComponent = null;
    }
  }

  @Override
  public void replaceComponent(Component oldComponent, Component newComponent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getComponentCount() {
    return myComponent == null ? 0 : 1;
  }

  @Override
  public Iterator<Component> iterator() {
    return myComponent == null ? Collections.<Component>emptyList().iterator() : Arrays.asList(myComponent).iterator();
  }

  @RequiredUIAccess
  @NotNull
  @Override
  public WrappedLayout set(@Nullable consulo.ui.Component component) {
    if (myComponent != null) {
      removeComponent(myComponent);
    }

    myComponent = (Component)component;

    if (component != null) {
      addComponent((Component)component);
    }
    return this;
  }

  @RequiredUIAccess
  @Override
  public void setSize(@NotNull Size size) {

  }
}
