/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.codeStyle.ui.internal.arrangement;

import consulo.language.codeStyle.arrangement.match.StdArrangementMatchRule;
import consulo.language.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import consulo.language.codeStyle.arrangement.model.ArrangementCompositeMatchCondition;
import consulo.language.codeStyle.arrangement.model.ArrangementMatchCondition;
import consulo.language.codeStyle.arrangement.model.ArrangementMatchConditionVisitor;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.language.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import consulo.language.codeStyle.arrangement.std.ArrangementUiComponent;
import consulo.ui.ex.awt.GridBag;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * {@link ArrangementUiComponent Component} for showing {@link ArrangementCompositeMatchCondition composite nodes}.
 * <p/>
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 * @since 8/8/12 10:51 AM
 */
public class ArrangementAndMatchConditionComponent extends JPanel implements ArrangementUiComponent {

  @Nonnull
  private final List<ArrangementUiComponent> myComponents = new ArrayList<>();
  @Nonnull
  private final Set<ArrangementSettingsToken> myAvailableTokens = new HashSet<>();

  @Nonnull
  private final ArrangementCompositeMatchCondition mySetting;
  @Nullable private      Rectangle                          myScreenBounds;
  @Nullable
  private      ArrangementUiComponent             myComponentUnderMouse;

  public ArrangementAndMatchConditionComponent(@Nonnull StdArrangementMatchRule rule,
                                               @Nonnull ArrangementCompositeMatchCondition setting,
                                               @Nonnull ArrangementMatchNodeComponentFactory factory,
                                               @Nonnull ArrangementStandardSettingsManager manager,
                                               boolean allowModification)
  {
    mySetting = setting;
    setOpaque(false);
    setLayout(new GridBagLayout());
    final Map<ArrangementSettingsToken, ArrangementMatchCondition> operands = new HashMap<>();
    ArrangementMatchConditionVisitor visitor = new ArrangementMatchConditionVisitor() {
      @Override
      public void visit(@Nonnull ArrangementAtomMatchCondition condition) {
        operands.put(condition.getType(), condition);
      }

      @Override
      public void visit(@Nonnull ArrangementCompositeMatchCondition condition) {
        assert false;
      }
    };
    for (ArrangementMatchCondition operand : setting.getOperands()) {
      operand.invite(visitor);
    }

    List<ArrangementSettingsToken> ordered = manager.sort(operands.keySet());
    GridBagConstraints constraints = new GridBag().anchor(GridBagConstraints.EAST).insets(0, 0, 0, ArrangementConstants.HORIZONTAL_GAP);
    for (ArrangementSettingsToken key : ordered) {
      ArrangementMatchCondition operand = operands.get(key);
      assert operand != null;
      ArrangementUiComponent component = factory.getComponent(operand, rule, allowModification);
      myComponents.add(component);
      myAvailableTokens.addAll(component.getAvailableTokens());
      JComponent uiComponent = component.getUiComponent();
      add(uiComponent, constraints);
    }
  }

  @Nonnull
  @Override
  public ArrangementMatchCondition getMatchCondition() {
    return mySetting;
  }

  @Override
  public void setData(@Nonnull Object data) {
    // Do nothing
  }

  @Nonnull
  @Override
  public JComponent getUiComponent() {
    return this;
  }

  @Nullable
  @Override
  public Rectangle getScreenBounds() {
    return myScreenBounds;
  }

  @Override
  public void setSelected(boolean selected) {
    for (ArrangementUiComponent component : myComponents) {
      component.setSelected(selected);
    }
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  @Override
  public void paint(Graphics g) {
    Point point = UIUtil.getLocationOnScreen(this);
    if (point != null) {
      Rectangle bounds = getBounds();
      myScreenBounds = new Rectangle(point.x, point.y, bounds.width, bounds.height);
    }
    super.paint(g);
  }

  @Override
  public Rectangle onMouseMove(@Nonnull MouseEvent event) {
    Point location = event.getLocationOnScreen();
    for (ArrangementUiComponent component : myComponents) {
      Rectangle bounds = component.getScreenBounds();
      if (bounds == null || !bounds.contains(location)) {
        continue;
      }
      if (myComponentUnderMouse == null) {
        myComponentUnderMouse = component;
        Rectangle rectangleOnEnter = myComponentUnderMouse.onMouseEntered(event);
        Rectangle rectangleOnMove = myComponentUnderMouse.onMouseMove(event);
        if (rectangleOnEnter != null && rectangleOnMove != null) {
          return myScreenBounds; // Repaint row
        }
        else if (rectangleOnEnter != null) {
          return rectangleOnEnter;
        }
        else {
          return rectangleOnMove;
        }
      }
      else {
        if (myComponentUnderMouse != component) {
          myComponentUnderMouse.onMouseExited();
          myComponentUnderMouse = component;
          component.onMouseEntered(event);
          return myScreenBounds; // Repaint row.
        }
        else {
          return component.onMouseMove(event);
        }
      }
    }
    if (myComponentUnderMouse == null) {
      return null;
    }
    else {
      Rectangle result = myComponentUnderMouse.onMouseExited();
      myComponentUnderMouse = null;
      return result;
    }
  }

  @Override
  public void onMouseRelease(@Nonnull MouseEvent event) {
    Point location = event.getLocationOnScreen();
    for (ArrangementUiComponent component : myComponents) {
      Rectangle bounds = component.getScreenBounds();
      if (bounds != null && bounds.contains(location)) {
        component.onMouseRelease(event);
        return;
      }
    }
  }

  @Override
  public Rectangle onMouseEntered(@Nonnull MouseEvent event) {
    Point location = event.getLocationOnScreen();
    for (ArrangementUiComponent component : myComponents) {
      Rectangle bounds = component.getScreenBounds();
      if (bounds != null && bounds.contains(location)) {
        myComponentUnderMouse = component;
        return component.onMouseEntered(event);
      }
    }
    return null;
  }

  @Nullable
  @Override
  public Rectangle onMouseExited() {
    if (myComponentUnderMouse != null) {
      Rectangle result = myComponentUnderMouse.onMouseExited();
      myComponentUnderMouse = null;
      return result;
    }
    return null;
  }

  @Nullable
  @Override
  public ArrangementSettingsToken getToken() {
    return myComponentUnderMouse == null ? null : myComponentUnderMouse.getToken();
  }

  @Nonnull
  @Override
  public Set<ArrangementSettingsToken> getAvailableTokens() {
    return myAvailableTokens;
  }

  @Override
  public void chooseToken(@Nonnull ArrangementSettingsToken data) throws IllegalArgumentException, UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isSelected() {
    return myComponentUnderMouse != null && myComponentUnderMouse.isSelected();
  }

  @Override
  public void reset() {
    for (ArrangementUiComponent component : myComponents) {
      component.reset();
    }
  }

  @Override
  public int getBaselineToUse(int width, int height) {
    return -1;
  }

  @Override
  public void setListener(@Nonnull Listener listener) {
    for (ArrangementUiComponent component : myComponents) {
      component.setListener(listener);
    }
  }

  @Override
  public void handleMouseClickOnSelected() {
    for (ArrangementUiComponent component : myComponents) {
      component.handleMouseClickOnSelected();
    }
  }

  @Override
  public boolean alwaysCanBeActive() {
    return false;
  }

  @Override
  public String toString() {
    return String.format("(%s)", StringUtil.join(myComponents, " and "));
  }
}
