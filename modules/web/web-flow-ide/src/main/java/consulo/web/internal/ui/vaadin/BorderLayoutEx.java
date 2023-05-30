/*
 * Copyright 2013-2023 consulo.io
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
package consulo.web.internal.ui.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * @author VISTALL
 * @since 30/05/2023
 */
public class BorderLayoutEx extends HorizontalLayout {
  public enum Constraint {
    NORTH,
    WEST,
    CENTER,
    EAST,
    SOUTH,
    PAGE_START,
    PAGE_END,
    LINE_START,
    LINE_END
  }

  public Component getComponent(Constraint constraint) {
    return null;
  }

   public void removeLayoutComponent(Component component) {

   }

   public void addComponent(Component component, Constraint constraint) {
    add(component);
   }
}
