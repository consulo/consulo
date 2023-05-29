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
import org.vaadin.addons.johannest.borderlayout.BorderLayout;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 29/05/2023
 */
public class SingleComponentLayout extends BorderLayout {
  public void setContent(Component center) {
    removeContent();

    if (center != null) {
      addComponent(center, Constraint.CENTER);
    }
  }

  public void removeIfContent(Component center) {
    if (Objects.equals(this, center.getParent().orElse(null))) {
      remove(center);
    }
  }

  private void removeContent() {
    Component oldComponent = getComponent(Constraint.CENTER);
    if (oldComponent != null && Objects.equals(this, oldComponent.getParent().orElse(null))) {
      removeLayoutComponent(oldComponent);
    }
  }
}
