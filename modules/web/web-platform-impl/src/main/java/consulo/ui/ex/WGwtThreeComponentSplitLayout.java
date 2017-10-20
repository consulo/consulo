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
package consulo.ui.ex;

import com.vaadin.ui.AbstractComponentContainer;
import com.vaadin.ui.Component;

import java.util.Iterator;

/**
 * @author VISTALL
 * @since 19-Oct-17
 */
public class WGwtThreeComponentSplitLayout extends AbstractComponentContainer {
  @Override
  public void replaceComponent(Component oldComponent, Component newComponent) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getComponentCount() {
    return 0;
  }

  @Override
  public Iterator<Component> iterator() {
    return null;
  }
}
