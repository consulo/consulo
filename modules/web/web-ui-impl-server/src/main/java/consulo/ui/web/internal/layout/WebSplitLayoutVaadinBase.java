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
package consulo.ui.web.internal.layout;

import com.vaadin.ui.Component;
import consulo.ui.layout.Layout;
import consulo.ui.web.internal.base.VaadinComponentContainer;
import consulo.web.gwt.shared.ui.state.layout.SplitLayoutState;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
* @author VISTALL
* @since 2019-02-17
*/
class WebSplitLayoutVaadinBase<T extends Layout> extends VaadinComponentContainer {
  private Component myFirstComponent;
  private Component mySecondComponent;

  public void setProportion(int percent) {
    getState().myProportion = percent;
    markAsDirty();
  }

  @Override
  public SplitLayoutState getState() {
    return (SplitLayoutState)super.getState();
  }

  public void setFirstComponent(@Nonnull Component component) {
    Component old = myFirstComponent;
    if (old != null) {
      removeComponent(old);
    }

    myFirstComponent = component;
    addComponent(component);
  }

  public void setSecondComponent(@Nonnull Component component) {
    Component old = mySecondComponent;
    if (old != null) {
      removeComponent(old);
    }

    mySecondComponent = component;
    addComponent(component);
  }

  @Override
  public int getComponentCount() {
    int i = 0;
    if (myFirstComponent != null) {
      i++;
    }
    if (mySecondComponent != null) {
      i++;
    }
    return i;
  }

  @Override
  public Iterator<Component> iterator() {
    List<Component> list = new ArrayList<>(getComponentCount());
    if (myFirstComponent != null) {
      list.add(myFirstComponent);
    }
    if (mySecondComponent != null) {
      list.add(mySecondComponent);
    }
    return list.iterator();
  }
}
