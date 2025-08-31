/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.language.codeStyle.arrangement.ArrangementColorsProvider;
import consulo.language.codeStyle.arrangement.ArrangementUtil;
import consulo.language.codeStyle.arrangement.match.ArrangementEntryMatcher;
import consulo.language.codeStyle.arrangement.match.ArrangementMatchRule;
import consulo.language.codeStyle.arrangement.match.StdArrangementEntryMatcher;
import consulo.language.codeStyle.arrangement.match.StdArrangementMatchRule;
import consulo.language.codeStyle.arrangement.model.ArrangementMatchCondition;
import consulo.language.codeStyle.arrangement.std.*;
import consulo.ui.ex.awt.GridBag;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.MultiRowFlowPanel;
import consulo.ui.ex.awt.UIUtil;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * Control for managing {@link ArrangementEntryMatcher matching rule conditions} for a single {@link ArrangementMatchRule}.
 * <p/>
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 * @since 8/14/12 9:54 AM
 */
public class ArrangementMatchingRuleEditor extends JPanel implements ArrangementUiComponent.Listener {

  @Nonnull
  private final Map<ArrangementSettingsToken, ArrangementUiComponent> myComponents = new HashMap<>();
  @Nonnull
  private final List<MultiRowFlowPanel> myRows = new ArrayList<>();

  @Nonnull
  private final ArrangementMatchingRulesControl myControl;
  @Nonnull
  private final ArrangementStandardSettingsManager mySettingsManager;
  @Nonnull
  private final ArrangementColorsProvider myColorsProvider;

  private int myRow = -1;
  private int myLabelWidth;

  @Nullable
  private JComponent myDefaultFocusRequestor;
  @Nullable
  private JComponent myFocusRequestor;

  private boolean mySkipStateChange;

  public ArrangementMatchingRuleEditor(@Nonnull ArrangementStandardSettingsManager settingsManager,
                                       @Nonnull ArrangementColorsProvider colorsProvider,
                                       @Nonnull ArrangementMatchingRulesControl control) {
    this(settingsManager, settingsManager.getSupportedMatchingTokens(), colorsProvider, control);
  }

  public ArrangementMatchingRuleEditor(@Nonnull ArrangementStandardSettingsManager settingsManager,
                                       @Nullable List<CompositeArrangementSettingsToken> tokens,
                                       @Nonnull ArrangementColorsProvider colorsProvider,
                                       @Nonnull ArrangementMatchingRulesControl control) {
    mySettingsManager = settingsManager;
    myColorsProvider = colorsProvider;
    myControl = control;
    init(tokens);
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        onMouseClicked(e);
      }
    });
  }

  private void init(@Nullable List<CompositeArrangementSettingsToken> tokens) {
    setLayout(new GridBagLayout());
    setBorder(IdeBorderFactory.createEmptyBorder(5));

    if (tokens != null) {
      for (CompositeArrangementSettingsToken token : tokens) {
        addToken(token);
      }
    }

    applyBackground(UIUtil.getListBackground());
  }

  private void addToken(@Nonnull CompositeArrangementSettingsToken rowToken) {
    List<CompositeArrangementSettingsToken> tokens = ArrangementUtil.flatten(rowToken);
    GridBag labelConstraints = new GridBag().anchor(GridBagConstraints.NORTHWEST).insets(ArrangementConstants.VERTICAL_PADDING, 0, 0, 0);
    MultiRowFlowPanel panel = new MultiRowFlowPanel(FlowLayout.LEFT, ArrangementConstants.HORIZONTAL_GAP, ArrangementConstants.VERTICAL_GAP);
    List<ArrangementSettingsToken> prevTokens = new ArrayList<>();
    StdArrangementTokenUiRole prevRole = null;
    ArrangementUiComponent component;
    JComponent uiComponent;
    for (CompositeArrangementSettingsToken token : tokens) {
      StdArrangementTokenUiRole role = token.getRole();
      if (role != prevRole && !prevTokens.isEmpty()) {
        component = ArrangementUtil.buildUiComponent(role, prevTokens, myColorsProvider, mySettingsManager);
        component.setListener(this);
        for (ArrangementSettingsToken prevToken : prevTokens) {
          myComponents.put(prevToken, component);
        }
        panel.add(component.getUiComponent());
        panel = addRowIfNecessary(panel);
        prevRole = null;
        prevTokens.clear();
      }
      component = ArrangementUtil.buildUiComponent(role, Collections.singletonList(token.getToken()), myColorsProvider, mySettingsManager);
      component.setListener(this);
      uiComponent = component.getUiComponent();
      switch (role) {
        case LABEL:
          panel = addRowIfNecessary(panel);
          add(uiComponent, labelConstraints);
          myLabelWidth = Math.max(myLabelWidth, uiComponent.getPreferredSize().width);
          prevRole = null;
          break;
        case TEXT_FIELD:
          panel = addRowIfNecessary(panel);

          ArrangementUiComponent textLabel = ArrangementUtil.buildUiComponent(StdArrangementTokenUiRole.LABEL, Collections.singletonList(token.getToken()), myColorsProvider, mySettingsManager);
          JComponent textLabelComponent = textLabel.getUiComponent();
          add(textLabelComponent, labelConstraints);
          myLabelWidth = Math.max(myLabelWidth, textLabelComponent.getPreferredSize().width);

          panel.add(uiComponent);
          panel = addRowIfNecessary(panel);
          prevRole = null;

          myComponents.put(token.getToken(), component);

          if (myDefaultFocusRequestor == null) {
            myDefaultFocusRequestor = uiComponent;
          }
          break;
        default:
          if (role == StdArrangementTokenUiRole.COMBO_BOX) {
            prevTokens.add(token.getToken());
            prevRole = role;
            break;
          }

          panel.add(uiComponent);
          myComponents.put(token.getToken(), component);
      }
    }

    if (prevRole != null && !prevTokens.isEmpty()) {
      component = ArrangementUtil.buildUiComponent(prevRole, prevTokens, myColorsProvider, mySettingsManager);
      panel.add(component.getUiComponent());
      component.setListener(this);
      for (ArrangementSettingsToken prevToken : prevTokens) {
        myComponents.put(prevToken, component);
      }
    }
    addRowIfNecessary(panel);
  }

  @Nonnull
  private MultiRowFlowPanel addRowIfNecessary(@Nonnull MultiRowFlowPanel panel) {
    if (panel.getComponentCount() <= 0) {
      return panel;
    }
    add(panel, new GridBag().anchor(GridBagConstraints.WEST).weightx(1).fillCellHorizontally().coverLine());
    myRows.add(panel);
    return new MultiRowFlowPanel(FlowLayout.LEFT, ArrangementConstants.HORIZONTAL_GAP, ArrangementConstants.VERTICAL_GAP);
  }

  @Override
  public void stateChanged() {
    if (!mySkipStateChange) {
      apply();
    }
  }

  @Nullable
  private Pair<ArrangementMatchCondition, ArrangementSettingsToken> buildCondition() {
    List<ArrangementMatchCondition> conditions = new ArrayList<>();
    ArrangementSettingsToken orderType = null;
    for (ArrangementUiComponent component : myComponents.values()) {
      if (!component.isEnabled() || !component.isSelected()) {
        continue;
      }
      ArrangementSettingsToken token = component.getToken();
      if (token != null && StdArrangementTokenType.ORDER.is(token)) {
        orderType = token;
      }
      else {
        conditions.add(component.getMatchCondition());
      }
    }
    if (!conditions.isEmpty()) {
      if (orderType == null) {
        orderType = StdArrangementTokens.Order.KEEP;
      }
      return Pair.create(ArrangementUtil.combine(conditions.toArray(new ArrangementMatchCondition[conditions.size()])), orderType);
    }
    else {
      return null;
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    if (myFocusRequestor != null) {
      if (myFocusRequestor.isFocusOwner()) {
        myFocusRequestor = null;
      }
      else {
        myFocusRequestor.requestFocusInWindow();
      }
    }
    super.paintComponent(g);
  }

  /**
   * Asks current editor to refresh its state in accordance with the arrangement rule shown at the given row.
   *
   * @param row row index of the rule which match condition should be edited (if defined);
   *            <code>'-1'</code> as an indication that no settings should be active
   */
  public void reset(int row) {
    // Reset state.
    myRow = row;
    myFocusRequestor = myDefaultFocusRequestor;
    mySkipStateChange = true;
    try {
      for (ArrangementUiComponent component : myComponents.values()) {
        component.reset();
      }
    }
    finally {
      mySkipStateChange = false;
    }

    ArrangementMatchingRulesModel model = myControl.getModel();
    if (row < 0 || row >= model.getSize()) {
      myRow = -1;
      return;
    }

    Object element = model.getElementAt(row);
    ArrangementSettingsToken orderType = element instanceof ArrangementMatchRule ? ((ArrangementMatchRule)element).getOrderType() : null;
    ArrangementMatchCondition condition;
    Map<ArrangementSettingsToken, Object> conditionTokens;

    if (element instanceof EmptyArrangementRuleComponent) {
      // We need to disable conditions which are not applicable for empty rules (e.g. we don't want to enable 'volatile' condition
      // for java rearranger if no 'field' condition is selected.
      condition = null;
      conditionTokens = new HashMap<>();
    }
    else if (!(element instanceof StdArrangementMatchRule)) {
      return;
    }
    else {
      condition = ((StdArrangementMatchRule)element).getMatcher().getCondition();
      conditionTokens = ArrangementUtil.extractTokens(condition);
    }

    mySkipStateChange = true;
    try {
      for (ArrangementUiComponent component : myComponents.values()) {
        ArrangementSettingsToken token = component.getToken();
        if (token != null && (component.getAvailableTokens().contains(orderType) || isEnabled(condition, token))) {
          component.setEnabled(true);
          if (component.getAvailableTokens().contains(orderType)) {
            component.chooseToken(orderType);
          }
          else {
            component.setSelected(conditionTokens.containsKey(token));
          }
          Object value = conditionTokens.get(token);
          if (value != null) {
            component.setData(value);
          }
        }
      }

      refreshConditions();
    }
    finally {
      mySkipStateChange = false;
    }
  }

  /**
   * Disable conditions not applicable at the current context (e.g. disable 'synchronized' if no 'method' is selected).
   */
  private void refreshConditions() {
    Pair<ArrangementMatchCondition, ArrangementSettingsToken> pair = buildCondition();
    ArrangementMatchCondition condition = pair == null ? null : pair.first;
    for (ArrangementUiComponent component : myComponents.values()) {
      ArrangementSettingsToken token = component.getToken();
      if (token == null) {
        continue;
      }
      boolean enabled = isEnabled(condition, token);
      component.setEnabled(enabled);
      if (!enabled) {
        component.setSelected(false);
      }
    }
  }

  private boolean isEnabled(@Nullable ArrangementMatchCondition condition, @Nonnull ArrangementSettingsToken token) {
    return ArrangementSectionRuleManager.isEnabled(token) || mySettingsManager.isEnabled(token, condition);
  }

  private void apply() {
    Pair<ArrangementMatchCondition, ArrangementSettingsToken> pair = buildCondition();
    Object modelValue;
    if (pair == null) {
      modelValue = new EmptyArrangementRuleComponent(myControl.getRowHeight(myRow));
    }
    else {
      modelValue = new StdArrangementMatchRule(new StdArrangementEntryMatcher(pair.first), pair.second);
    }
    myControl.getModel().set(myRow, modelValue);
    myControl.repaintRows(myRow, myRow, true);
  }

  public void applyAvailableWidth(int width) {
    for (MultiRowFlowPanel row : myRows) {
      row.setForcedWidth(width - myLabelWidth);
    }
    validate();
  }

  private void applyBackground(@Nonnull Color color) {
    setBackground(color);
    for (JComponent component : myRows) {
      component.setBackground(color);
    }
  }

  private void onMouseClicked(@Nonnull MouseEvent e) {
    if (myRow < 0) {
      return;
    }

    Point locationOnScreen = e.getLocationOnScreen();
    for (ArrangementUiComponent component : myComponents.values()) {
      Rectangle screenBounds = component.getScreenBounds();
      if (screenBounds == null || !screenBounds.contains(locationOnScreen)) {
        continue;
      }
      if (component.isEnabled()) {
        if (component.isSelected()) {
          // don't allow to remove start/end section indication
          Set<ArrangementSettingsToken> mutexes = ArrangementSectionRuleManager.getSectionMutexes();
          if (!mutexes.contains(component.getToken())) {
            removeCondition(component);
          }
        }
        else {
          addCondition(component);
        }
      }
      apply();
      return;
    }
  }

  private void addCondition(@Nonnull ArrangementUiComponent component) {
    mySkipStateChange = true;
    try {
      component.setSelected(true);
      Collection<Set<ArrangementSettingsToken>> mutexes = mySettingsManager.getMutexes();

      // Update 'mutex conditions', i.e. conditions which can't be active at the same time (e.g. type 'field' and type 'method').
      for (Set<ArrangementSettingsToken> mutex : mutexes) {
        updateMutexConditions(component, mutex);
      }
      updateMutexConditions(component, ArrangementSectionRuleManager.getSectionMutexes());
      refreshConditions();
    }
    finally {
      mySkipStateChange = false;
    }
  }

  private void updateMutexConditions(@Nonnull ArrangementUiComponent component, @Nonnull Set<ArrangementSettingsToken> mutex) {
    if (!mutex.contains(component.getToken())) {
      return;
    }
    for (ArrangementSettingsToken key : mutex) {
      if (key.equals(component.getToken())) {
        continue;
      }
      ArrangementUiComponent c = myComponents.get(key);
      if (c != null && c.isEnabled()) {
        removeCondition(c);
      }
    }
  }

  private void removeCondition(@Nonnull ArrangementUiComponent component) {
    component.setSelected(false);
    refreshConditions();
  }

  @Override
  public String toString() {
    return "matching rule editor";
  }
}
