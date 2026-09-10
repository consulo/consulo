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
package consulo.web.ui.impl.internal.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.ThemableLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 30/05/2023
 */
public class BorderLayoutEx extends VerticalLayout {
  /**
   * The dock is three rows with a row of its own in the middle, so a gap has to reach both the rows and what
   * stands beside the center - setting it on the outer layout alone would leave west and east flush.
   */
  public void setGapClass(String gapClass) {
    addClassName(gapClass);
    myCenterLayout.addClassName(gapClass);
  }

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
    myTopLayout.setWidthFull();
    myBottomLayout.setWidthFull();

    noPaddingMargin(myCenterLayout);

    myCenterHolder.setSizeFull();
    // a flex item is not allowed to shrink below its content unless it is told to, so a wide center - a status
    // bar with a running task in it - grew past its share of the row and drew over what sits east of it
    myCenterHolder.getStyle().set("min-width", "0").set("overflow", "hidden");

    // the sides keep the width of what they hold - a menu bar which is allowed to shrink starts folding its
    // items into an overflow button instead
    myWestHolder.getStyle().set("flex", "0 0 auto");
    myEastHolder.getStyle().set("flex", "0 0 auto");

    centerContent(myWestHolder);
    centerContent(myEastHolder);

    myCenterLayout.add(myWestHolder);
    myCenterLayout.add(myCenterHolder);
    myCenterLayout.setFlexGrow(1, myCenterHolder);
    myCenterLayout.add(myEastHolder);

    add(noPaddingMargin(myTopLayout));
    add(noPaddingMargin(myCenterLayout));
    add(noPaddingMargin(myBottomLayout));

    setFlexGrow(1, myCenterLayout);
    myCenterLayout.getStyle().set("min-width", "0");
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

  /**
   * A side of an awt border layout is as tall as the row it sits in, and something drawn on its edge - the
   * line which separates it from the centre - is only a separator while it runs the whole height.
   */
  private void fillHeight(Component component) {
    // a label stretched over the row draws its glyph at the top of that height, and the holder centring it then has
    // nothing left to centre - everything else keeps the full height a side of an awt border layout has
    if (component instanceof VaadinLabelComponentBase) {
      return;
    }

    if (component instanceof HasSize hasSize) {
      hasSize.setHeightFull();
    }
  }

  private static void centerContent(Div holder) {
    holder.getStyle().set("display", "flex").set("align-items", "center");
  }

  private <T extends Component & HasSize & ThemableLayout> T noPaddingMargin(T component) {
    component.setMargin(false);
    component.setPadding(false);
    // spacing renders as gap - without disabling it the ide frame gets 8px above and below its content
    component.setSpacing(false);
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
    myCenterLayout.addClassName(AuraUtility.Overflow.AUTO);
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
          fillHeight(component);

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
          fillHeight(component);

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
