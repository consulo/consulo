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
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.ThemableLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 30/05/2023
 */
public class BorderLayoutEx extends VerticalLayout {
  public enum Constraint {
    NORTH,
    WEST,
    CENTER,
    EAST,
    SOUTH,
  }

  private final HorizontalLayout myTopLayout = new HorizontalLayout();
  private final HorizontalLayout myBottomLayout = new HorizontalLayout();
  private final HorizontalLayout myCenterLayout = new HorizontalLayout();

  private final Div myWestHolder = new Div();
  private final Div myCenterHolder = new Div();
  private final Div myEastHolder = new Div();

  public BorderLayoutEx() {
    noPaddingMargin(this);

    myCenterLayout.setSizeFull();

    myCenterLayout.setPadding(false);
    myCenterLayout.setMargin(false);

    myCenterHolder.setSizeFull();

    myCenterLayout.add(myWestHolder);
    myCenterLayout.add(myCenterHolder);
    myCenterLayout.setFlexGrow(1, myCenterHolder);
    myCenterLayout.add(myEastHolder);

    add(noPaddingMargin(myTopLayout));
    add(noPaddingMargin(myCenterLayout));
    add(noPaddingMargin(myBottomLayout));
  }

  @Override
  public void removeAll() {
    myTopLayout.removeAll();
    myBottomLayout.removeAll();

    myWestHolder.removeAll();
    myCenterHolder.removeAll();
    myEastHolder.removeAll();
  }

  private void validate() {
    myTopLayout.setVisible(myTopLayout.getComponentCount() > 0);
    myBottomLayout.setVisible(myBottomLayout.getComponentCount() > 0);

    myWestHolder.setVisible(myWestHolder.getComponentCount() > 0);
    myEastHolder.setVisible(myEastHolder.getComponentCount() > 0);
  }

  private <T extends Component & HasSize & ThemableLayout> T noPaddingMargin(T component) {
    component.setMargin(false);
    component.setPadding(false);
    return component;
  }

  @Override
  public void addClassNames(String... classNames) {
    for (String className : classNames) {
      addClassName(className);
    }
  }

  @Override
  public void addClassName(String className) {
    super.addClassName(className);

    myTopLayout.addClassName(className + "-top");
    myBottomLayout.addClassName(className + "-bottom");
    myCenterLayout.addClassName(className + "-center");
    myCenterLayout.addClassName(LumoUtility.Overflow.AUTO);
  }

  public void addComponent(@Nullable Component component, Constraint constraint) {
    setComponent(component, constraint);
  }

  public void setComponent(@Nullable Component component, Constraint constraint) {
    switch (constraint) {
      case NORTH:
        myTopLayout.removeAll();

        if (component != null) {
          myTopLayout.add(component);
        }
        break;
      case WEST:
        myWestHolder.removeAll();

        if (component != null) {
          myWestHolder.add(component);
        }
        break;
      case CENTER:
        myCenterHolder.removeAll();
        
        if (component != null) {
          ((HasSize)component).setSizeFull();

          myCenterHolder.add(component);
        }
        break;
      case EAST:
        myEastHolder.removeAll();

        if (component != null) {
          myEastHolder.add(component);
        }
        break;
      case SOUTH:
        myBottomLayout.removeAll();

        if (component != null) {
          myBottomLayout.add(component);
        }
        break;
    }

    if (component != null) {
      ComponentUtil.setData(component, Constraint.class, constraint);
    }

    validate();
  }
}
