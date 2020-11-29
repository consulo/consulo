/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.component;

import com.intellij.application.options.codeStyle.arrangement.ArrangementConstants;
import com.intellij.application.options.codeStyle.arrangement.action.ArrangementRemoveConditionAction;
import com.intellij.application.options.codeStyle.arrangement.animation.ArrangementAnimationPanel;
import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.application.options.codeStyle.arrangement.util.InsetsPanel;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.codeStyle.arrangement.model.ArrangementAtomMatchCondition;
import com.intellij.psi.codeStyle.arrangement.std.*;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.RoundedLineBorder;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtilRt;
import com.intellij.util.ui.GridBag;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.ui.Size;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Set;

/**
 * {@link ArrangementUiComponent} for {@link ArrangementAtomMatchCondition} representation.
 * <p/>
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 * @since 8/8/12 10:06 AM
 */
public class ArrangementAtomMatchConditionComponent implements ArrangementUiComponent {

  @Nonnull
  private static final BorderStrategy TEXT_BORDER_STRATEGY       = new NameBorderStrategy();
  @Nonnull
  private static final BorderStrategy PREDEFINED_BORDER_STRATEGY = new PredefinedConditionBorderStrategy();

  @Nonnull
  private final SimpleColoredComponent myTextControl = new SimpleColoredComponent() {
    @Nonnull
    @Override
    public Dimension getMinimumSize() {
      return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
      return getPreferredSize();
    }

    @Nonnull
    @Override
    public Dimension getPreferredSize() {
      return myTextControlSize == null ? super.getPreferredSize() : myTextControlSize;
    }

    @Override
    public String toString() {
      return "text component for " + myText;
    }
  };

  @Nonnull
  private final Set<ArrangementSettingsToken> myAvailableTokens = ContainerUtilRt.newHashSet();

  @Nonnull
  private final BorderStrategy                myBorderStrategy;
  @Nonnull
  private final String                        myText;
  @Nonnull
  private final ArrangementColorsProvider     myColorsProvider;
  @Nonnull
  private final RoundedLineBorder             myBorder;
  @Nonnull
  private final ArrangementAtomMatchCondition myCondition;
  @Nonnull
  private final ArrangementAnimationPanel     myAnimationPanel;

  @Nullable private final ActionButton                                     myCloseButton;
  @Nullable
  private final Rectangle                                        myCloseButtonBounds;
  @Nullable private final Consumer<ArrangementAtomMatchConditionComponent> myCloseCallback;

  @Nonnull
  private ColorValue myBackgroundColor;

  @Nullable
  private final Dimension myTextControlSize;
  @Nullable
  private       Rectangle myScreenBounds;
  @Nullable private       Listener  myListener;

  private boolean myInverted = false;
  private boolean myEnabled = true;
  private boolean mySelected;
  private boolean myCloseButtonHovered;

  // cached value for inverted atom condition, e.g. condition: 'static', opposite: 'not static'
  @Nullable private ArrangementAtomMatchCondition myOppositeCondition;
  @Nullable private String myInvertedText;

  public ArrangementAtomMatchConditionComponent(@Nonnull ArrangementStandardSettingsManager manager,
                                                @Nonnull ArrangementColorsProvider colorsProvider,
                                                @Nonnull ArrangementAtomMatchCondition condition,
                                                @Nullable Consumer<ArrangementAtomMatchConditionComponent> closeCallback)
  {
    myColorsProvider = colorsProvider;
    myCondition = condition;
    myAvailableTokens.add(condition.getType());
    myCloseCallback = closeCallback;
    ArrangementSettingsToken type = condition.getType();
    if (StdArrangementTokenType.REG_EXP.is(type)) {
      myBorderStrategy = TEXT_BORDER_STRATEGY;
    }
    else {
      myBorderStrategy = PREDEFINED_BORDER_STRATEGY;
    }
    if (type.equals(condition.getValue()) || condition.getValue() instanceof Boolean) {
      myText = type.getRepresentationValue();
    }
    else if (StdArrangementTokenType.REG_EXP.is(type)) {
      myText = String.format("%s %s", type.getRepresentationValue().toLowerCase(), condition.getValue());
    }
    else {
      myText = condition.getValue().toString();
    }
    myTextControl.setTextAlign(SwingConstants.CENTER);
    myTextControl.append(myText, SimpleTextAttributes.fromTextAttributes(colorsProvider.getTextAttributes(type, false)));
    myTextControl.setOpaque(false);
    int maxWidth = manager.getWidth(type);
    if (!StdArrangementTokenType.REG_EXP.is(type) && maxWidth > 0) {
      myTextControlSize = new Dimension(maxWidth, myTextControl.getPreferredSize().height);
    }
    else {
      myTextControlSize = myTextControl.getPreferredSize();
    }

    final ArrangementRemoveConditionAction action = new ArrangementRemoveConditionAction();
    Image buttonIcon = action.getTemplatePresentation().getIcon();
    Size buttonSize = new Size(buttonIcon.getWidth(), buttonIcon.getHeight());
    if (closeCallback == null) {
      myCloseButton = null;
      myCloseButtonBounds = null;
    }
    else {
      myCloseButton = new ActionButton(
              action,
              action.getTemplatePresentation().clone(),
              ArrangementConstants.MATCHING_RULES_CONTROL_PLACE,
              buttonSize)
      {
        @Override
        public Image getIcon() {
          return myCloseButtonHovered ? action.getTemplatePresentation().getHoveredIcon() : action.getTemplatePresentation().getIcon();
        }
      };
      myCloseButtonBounds = new Rectangle(0, 0, buttonIcon.getWidth(), buttonIcon.getHeight());
    }

    JPanel insetsPanel = new JPanel(new GridBagLayout()) {
      @Override
      public String toString() {
        return "insets panel for " + myText;
      }
    };

    GridBagConstraints constraints = new GridBag().anchor(GridBagConstraints.WEST).weightx(1)
            .insets(0, 0, 0, myCloseButton == null ? ArrangementConstants.BORDER_ARC_SIZE : 0);
    insetsPanel.add(myTextControl, constraints);
    insetsPanel.setBorder(IdeBorderFactory.createEmptyBorder(0, ArrangementConstants.HORIZONTAL_PADDING, 0, 0));
    insetsPanel.setOpaque(false);

    JPanel roundBorderPanel = new JPanel(new GridBagLayout()) {
      @Override
      public void paint(Graphics g) {
        Rectangle buttonBounds = getCloseButtonScreenBounds();
        if (buttonBounds != null) {
          Point mouseScreenLocation = MouseInfo.getPointerInfo().getLocation();
          myCloseButtonHovered = buttonBounds.contains(mouseScreenLocation);
        }

        Rectangle bounds = getBounds();
        g.setColor(TargetAWT.to(myBackgroundColor));
        g.fillRoundRect(0, 0, bounds.width, bounds.height, ArrangementConstants.BORDER_ARC_SIZE, ArrangementConstants.BORDER_ARC_SIZE);
        super.paint(g);
      }

      @Override
      public String toString() {
        return "round border panel for " + myText;
      }

      @Override
      protected void paintBorder(Graphics g) {
        myBorderStrategy.setup((Graphics2D)g);
        super.paintBorder(g);
      }
    };
    roundBorderPanel.add(insetsPanel, new GridBag().anchor(GridBagConstraints.WEST));
    if (myCloseButton != null) {
      roundBorderPanel.add(new InsetsPanel(myCloseButton), new GridBag().anchor(GridBagConstraints.EAST));
    }
    myBorder = myBorderStrategy.create();
    roundBorderPanel.setBorder(myBorder);
    roundBorderPanel.setOpaque(false);

    myAnimationPanel = new ArrangementAnimationPanel(roundBorderPanel, false, true) {
      @Override
      public void paint(Graphics g) {
        Point point = UIUtil.getLocationOnScreen(this);
        if (point != null) {
          Rectangle bounds = myAnimationPanel.getBounds();
          myScreenBounds = new Rectangle(point.x, point.y, bounds.width, bounds.height);
        }
        if (!myEnabled && g instanceof Graphics2D) {
          ((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        }
        super.paint(g);
      }
    };

    setSelected(false);
    if (myCloseButton != null) {
      myCloseButton.setVisible(false);
    }
    setData(myCondition.getValue());
  }

  @Nonnull
  @Override
  public ArrangementAtomMatchCondition getMatchCondition() {
    if (Boolean.valueOf(myInverted).equals(myCondition.getValue())) {
      if (myOppositeCondition == null) {
        myOppositeCondition = new ArrangementAtomMatchCondition(myCondition.getType(), !myInverted);
      }
      return myOppositeCondition;
    }
    return myCondition;
  }

  @Override
  public void setData(@Nonnull Object data) {
    if (data instanceof Boolean && myCondition.getType() instanceof InvertibleArrangementSettingsToken) {
      myInverted = !((Boolean)data);
      updateComponentText(mySelected);
    }
  }

  @Nonnull
  @Override
  public JComponent getUiComponent() {
    return myAnimationPanel;
  }

  @Nullable
  @Override
  public Rectangle getScreenBounds() {
    return myScreenBounds;
  }

  /**
   * Instructs current component that it should {@link #getUiComponent() draw} itself according to the given 'selected' state.
   *
   * @param selected  flag that indicates if current component should be drawn as 'selected'
   */
  @Override
  public void setSelected(boolean selected) {
    boolean notifyListener = selected != mySelected;
    mySelected = selected;
    TextAttributes attributes = updateComponentText(selected);
    myBorder.setColor(myColorsProvider.getBorderColor(selected));
    myBackgroundColor = attributes.getBackgroundColor();
    if (notifyListener && myListener != null) {
      myListener.stateChanged();
    }
  }

  @Nonnull
  private TextAttributes updateComponentText(boolean selected) {
    myTextControl.clear();
    TextAttributes attributes = myColorsProvider.getTextAttributes(myCondition.getType(), selected);
    myTextControl.append(getComponentText(), SimpleTextAttributes.fromTextAttributes(attributes));
    return attributes;
  }

  private String getComponentText() {
    if (myInverted) {
      if (StringUtil.isEmpty(myInvertedText)) {
        final ArrangementSettingsToken token = myCondition.getType();
        assert token instanceof InvertibleArrangementSettingsToken;
        myInvertedText = ((InvertibleArrangementSettingsToken)token).getInvertedRepresentationValue();
      }
      return myInvertedText;
    }
    return myText;
  }

  @Override
  public boolean isEnabled() {
    return myEnabled;
  }

  /**
   * Instructs current component that it should {@link #getUiComponent() draw} itself according to the given 'enabled' state.
   *
   * @param enabled  flag that indicates if current component should be drawn as 'enabled'
   */
  @Override
  public void setEnabled(boolean enabled) {
    myEnabled = enabled;
    if (!enabled) {
      setSelected(false);
    }
  }

  @Nullable
  @Override
  public Rectangle onMouseMove(@Nonnull MouseEvent event) {
    Rectangle buttonBounds = getCloseButtonScreenBounds();
    if (buttonBounds == null) {
      return null;
    }
    if (myCloseButton != null && !myCloseButton.isVisible()) {
      myCloseButton.setVisible(true);
      return buttonBounds;
    }
    boolean mouseOverButton = buttonBounds.contains(event.getLocationOnScreen());
    return (mouseOverButton ^ myCloseButtonHovered) ? buttonBounds : null;
  }

  @Override
  public void onMouseRelease(@Nonnull MouseEvent event) {
    Rectangle buttonBounds = getCloseButtonScreenBounds();
    if (buttonBounds != null && myCloseCallback != null && buttonBounds.contains(event.getLocationOnScreen())) {
      myCloseCallback.consume(this);
      event.consume();
    }
  }

  @Override
  public Rectangle onMouseEntered(@Nonnull MouseEvent e) {
    if (myCloseButton != null) {
      myCloseButton.setVisible(true);
      return getCloseButtonScreenBounds();
    }
    return null;
  }

  @Nullable
  @Override
  public Rectangle onMouseExited() {
    if (myCloseButton == null) {
      return null;
    }
    myCloseButton.setVisible(false);
    return getCloseButtonScreenBounds();
  }

  @Nullable
  private Rectangle getCloseButtonScreenBounds() {
    if (myCloseButton == null || myScreenBounds == null) {
      return null;
    }

    Rectangle buttonBounds = SwingUtilities.convertRectangle(myCloseButton.getParent(), myCloseButtonBounds, myAnimationPanel);
    buttonBounds.x += myScreenBounds.x;
    buttonBounds.y += myScreenBounds.y;
    return buttonBounds;
  }

  @Nonnull
  public ArrangementAnimationPanel getAnimationPanel() {
    return myAnimationPanel;
  }

  @Override
  public String toString() {
    return getComponentText();
  }

  @Nonnull
  @Override
  public ArrangementSettingsToken getToken() {
    return myCondition.getType();
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
    return mySelected;
  }

  @Override
  public void reset() {
    setSelected(false);
    setData(true);
  }

  @Override
  public int getBaselineToUse(int width, int height) {
    return -1;
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public void setListener(@Nonnull Listener listener) {
    myListener = listener;
  }

  @Override
  public void handleMouseClickOnSelected() {
    if (myInverted || !(myCondition.getType() instanceof InvertibleArrangementSettingsToken)) {
      setSelected(false);
    }
    setData(myInverted);
  }

  @Override
  public boolean alwaysCanBeActive() {
    return myInverted;
  }

  private interface BorderStrategy {
    RoundedLineBorder create();
    void setup(@Nonnull Graphics2D g);
  }

  private static class PredefinedConditionBorderStrategy implements BorderStrategy {
    @Override
    public RoundedLineBorder create() {
      return IdeBorderFactory.createRoundedBorder(ArrangementConstants.BORDER_ARC_SIZE);
    }

    @Override
    public void setup(@Nonnull Graphics2D g) {
    }
  }

  private static class NameBorderStrategy implements BorderStrategy {

    @Nonnull
    private final BasicStroke myStroke = new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1, new float[]{5, 5}, 0);

    @Override
    public RoundedLineBorder create() {
      return IdeBorderFactory.createRoundedBorder(ArrangementConstants.BORDER_ARC_SIZE, 2);
    }

    @Override
    public void setup(@Nonnull Graphics2D g) {
      g.setStroke(myStroke);
    }
  }
}
