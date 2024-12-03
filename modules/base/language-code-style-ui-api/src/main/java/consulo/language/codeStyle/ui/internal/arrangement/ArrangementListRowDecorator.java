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
package consulo.language.codeStyle.ui.internal.arrangement;

import consulo.application.localize.ApplicationLocalize;
import consulo.language.codeStyle.arrangement.model.ArrangementMatchCondition;
import consulo.language.codeStyle.arrangement.std.ArrangementSettingsToken;
import consulo.language.codeStyle.arrangement.std.ArrangementUiComponent;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Set;

/**
 * @author Denis Zhdanov
 * @since 10/31/12 5:00 PM
 */
public class ArrangementListRowDecorator extends JPanel implements ArrangementUiComponent {

    @Nonnull
    private final JLabel mySortLabel = new JBLabel(PlatformIconGroup.objectbrowserSorted());

    @Nonnull
    private final ArrangementRuleIndexControl myRowIndexControl;
    @Nonnull
    private final ArrangementUiComponent myDelegate;
    @Nonnull
    private final ArrangementMatchingRulesControl myControl;
    @Nonnull
    private final JToggleButton myEditButton;

    @Nullable
    private Rectangle myScreenBounds;

    private boolean myBeingEdited;
    private boolean myUnderMouse;

    public ArrangementListRowDecorator(@Nonnull ArrangementUiComponent delegate,
                                       @Nonnull ArrangementMatchingRulesControl control) {
        myDelegate = delegate;
        myControl = control;

        mySortLabel.setVisible(false);

        myEditButton = new JToggleButton(TargetAWT.to(PlatformIconGroup.actionsEdit()));
        myEditButton.putClientProperty("JButton.buttonType", "borderless");
        myEditButton.setToolTipText(ApplicationLocalize.arrangementActionRuleEditDescription().get());
        myEditButton.setVisible(false);

        FontMetrics metrics = getFontMetrics(getFont());
        int maxWidth = 0;
        for (int i = 0; i <= 99; i++) {
            maxWidth = Math.max(metrics.stringWidth(String.valueOf(i)), maxWidth);
        }
        int height = metrics.getHeight() - metrics.getDescent() - metrics.getLeading();
        int diameter = Math.max(maxWidth, height) * 5 / 3;
        myRowIndexControl = new ArrangementRuleIndexControl(diameter, height);

        setOpaque(true);
        init();
    }

    public void setError(@Nullable String message) {
        myRowIndexControl.setError(StringUtil.isNotEmpty(message));
        setToolTipText(message);
    }

    private void init() {
        setLayout(new GridBagLayout());
        GridBag constraints = new GridBag().anchor(GridBagConstraints.CENTER)
            .insets(0, ArrangementConstants.HORIZONTAL_PADDING, 0, ArrangementConstants.HORIZONTAL_GAP * 2);
        add(myRowIndexControl, constraints);
        add(new InsetsPanel(mySortLabel), new GridBag().anchor(GridBagConstraints.CENTER).insets(0, 0, 0, ArrangementConstants.HORIZONTAL_GAP));
        add(myDelegate.getUiComponent(), new GridBag().weightx(1).anchor(GridBagConstraints.WEST));
        add(myEditButton, new GridBag().anchor(GridBagConstraints.EAST));
        setBorder(IdeBorderFactory.createEmptyBorder(ArrangementConstants.VERTICAL_GAP));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Point point = UIUtil.getLocationOnScreen(this);
        if (point != null) {
            Rectangle bounds = getBounds();
            myScreenBounds = new Rectangle(point.x, point.y, bounds.width, bounds.height);
        }

        FontMetrics metrics = g.getFontMetrics();
        int baseLine = SimpleColoredComponent.getTextBaseLine(metrics, metrics.getHeight());
        myRowIndexControl.setBaseLine(
            baseLine + ArrangementConstants.VERTICAL_GAP + myDelegate.getUiComponent().getBounds().y - myRowIndexControl.getBounds().y
        );
        super.paintComponent(g);
    }

    public void setRowIndex(int row) {
        myRowIndexControl.setIndex(row);
    }

    public void setUnderMouse(boolean underMouse) {
        myUnderMouse = underMouse;
        if (myUnderMouse) {
            setBackground(UIUtil.getDecoratedRowColor());
        }
        else {
            setBackground(UIUtil.getListBackground());
        }
    }

    public void setBeingEdited(boolean beingEdited) {
        if (myBeingEdited && !beingEdited) {
            myEditButton.setSelected(false);
        }
        if (!beingEdited && !myUnderMouse) {
            myEditButton.setVisible(false);
        }
        if (beingEdited && !myBeingEdited) {
            myEditButton.setVisible(true);
            myEditButton.setSelected(true);
        }
        myBeingEdited = beingEdited;
    }

    @Nonnull
    @Override
    public ArrangementMatchCondition getMatchCondition() {
        return myDelegate.getMatchCondition();
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
        myDelegate.setSelected(selected);
    }

    @Override
    public void setData(@Nonnull Object data) {
        myDelegate.setData(data);
    }

    public void setShowSortIcon(boolean show) {
        mySortLabel.setVisible(show);
    }

    @Override
    public Rectangle onMouseEntered(@Nonnull MouseEvent e) {
        setBackground(UIUtil.getDecoratedRowColor());
        myEditButton.setVisible(myControl.getSelectedModelRows().size() <= 1);
        return myDelegate.onMouseEntered(e);
    }

    @Nullable
    @Override
    public Rectangle onMouseMove(@Nonnull MouseEvent event) {
        myEditButton.setVisible(myControl.getSelectedModelRows().size() <= 1);
        Rectangle bounds = getButtonScreenBounds();
        if (!myBeingEdited && bounds != null) {
            boolean selected = bounds.contains(event.getLocationOnScreen());
            boolean wasSelected = myEditButton.isSelected();
            myEditButton.setSelected(selected);
            if (selected ^ wasSelected) {
                return myScreenBounds;
            }
        }

        return myDelegate.onMouseMove(event);
    }

    @Override
    public void onMouseRelease(@Nonnull MouseEvent event) {
        myEditButton.setVisible(myControl.getSelectedModelRows().size() <= 1);
        Rectangle bounds = getButtonScreenBounds();
        if (bounds != null && bounds.contains(event.getLocationOnScreen())) {
            if (myBeingEdited) {
                myControl.hideEditor();
                myBeingEdited = false;
            }
            else {
                int row = myControl.getRowByRenderer(this);
                if (row >= 0) {
                    myControl.showEditor(row);
                    myControl.scrollRowToVisible(row);
                    myBeingEdited = true;
                }
            }
            event.consume();
            return;
        }
        myDelegate.onMouseRelease(event);
    }

    @Nullable
    @Override
    public Rectangle onMouseExited() {
        setBackground(UIUtil.getListBackground());
        if (!myBeingEdited) {
            myEditButton.setVisible(false);
        }
        return myDelegate.onMouseExited();
    }

    @Nullable
    private Rectangle getButtonScreenBounds() {
        if (myScreenBounds == null) {
            return null;
        }
        Rectangle bounds = myEditButton.getBounds();
        return new Rectangle(bounds.x + myScreenBounds.x, bounds.y + myScreenBounds.y, bounds.width, bounds.height);
    }

    @Nullable
    @Override
    public ArrangementSettingsToken getToken() {
        return myDelegate.getToken();
    }

    @Nonnull
    @Override
    public Set<ArrangementSettingsToken> getAvailableTokens() {
        return myDelegate.getAvailableTokens();
    }

    @Override
    public void chooseToken(@Nonnull ArrangementSettingsToken data) throws IllegalArgumentException, UnsupportedOperationException {
        myDelegate.chooseToken(data);
    }

    @Override
    public boolean isSelected() {
        return myDelegate.isSelected();
    }

    @Override
    public void reset() {
        myDelegate.reset();
    }

    @Override
    public int getBaselineToUse(int width, int height) {
        return myDelegate.getBaselineToUse(width, height);
    }

    @Override
    public void setListener(@Nonnull Listener listener) {
        myDelegate.setListener(listener);
    }

    @Override
    public void handleMouseClickOnSelected() {
        myDelegate.handleMouseClickOnSelected();
    }

    @Override
    public boolean alwaysCanBeActive() {
        return false;
    }

    @Override
    public String toString() {
        return "list row decorator for " + myDelegate.toString();
    }
}
