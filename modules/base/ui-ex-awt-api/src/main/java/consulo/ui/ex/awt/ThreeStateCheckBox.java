/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.ui.ex.awt.accessibility.AccessibleContextUtil;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.*;
import java.awt.event.ItemEvent;

/**
 * @author spleaner
 */
public class ThreeStateCheckBox extends JCheckBox {
    private State myState;
    private boolean myThirdStateEnabled = true;

    public enum State {
        SELECTED,
        NOT_SELECTED,
        DONT_CARE
    }

    public ThreeStateCheckBox() {
        this(null, null, State.DONT_CARE);
    }

    public ThreeStateCheckBox(final State initial) {
        this(null, null, initial);
    }

    public ThreeStateCheckBox(final String text) {
        this(text, null, State.DONT_CARE);
    }

    public ThreeStateCheckBox(final String text, final State initial) {
        this(text, null, initial);
    }

    public ThreeStateCheckBox(final String text, final Icon icon) {
        this(text, icon, State.DONT_CARE);
    }

    public ThreeStateCheckBox(final String text, final Icon icon, final State initial) {
        super(text, icon);

        setModel(new ToggleButtonModel() {
            @Override
            public void setSelected(boolean selected) {
                setState(nextState());
                fireStateChanged();
                fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this, ItemEvent.SELECTED));
            }

            @Override
            public boolean isSelected() {
                return myState == State.SELECTED;
            }
        });

        setState(initial);
    }

    private State nextState() {
        switch (myState) {
            case SELECTED:
                return State.NOT_SELECTED;
            case NOT_SELECTED:
                if (myThirdStateEnabled) {
                    return State.DONT_CARE;
                }
                else {
                    return State.SELECTED;
                }
            default:
                return State.SELECTED;
        }
    }

    public boolean isThirdStateEnabled() {
        return myThirdStateEnabled;
    }

    public void setThirdStateEnabled(final boolean thirdStateEnabled) {
        myThirdStateEnabled = thirdStateEnabled;
    }

    @Override
    public void setSelected(final boolean b) {
        setState(b ? State.SELECTED : State.NOT_SELECTED);
    }

    public void setState(State state) {
        myState = state;

        String value = state == State.DONT_CARE ? "indeterminate" : null;
        putClientProperty("JButton.selectedState", value);

        repaint();
    }

    public State getState() {
        return myState;
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleThreeStateCheckBox();
        }
        return accessibleContext;
    }

    /**
     * Emulate accessibility behavior of tri-state checkboxes, as tri-state checkboxes
     * are not part of the JAB specification.
     */
    protected class AccessibleThreeStateCheckBox extends AccessibleJCheckBox {
        @Override
        public AccessibleRole getAccessibleRole() {
            if (myThirdStateEnabled) {
                // Return TOGGLE_BUTTON so that screen readers don't announce the "not checked" state
                return AccessibleRole.TOGGLE_BUTTON;
            }

            return super.getAccessibleRole();
        }

        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            if (myThirdStateEnabled) {
                // Remove CHECKED so that screen readers don't announce the "checked" state
                AccessibleStateSet set = super.getAccessibleStateSet();
                set.remove(AccessibleState.CHECKED);
                return set;
            }

            return super.getAccessibleStateSet();
        }

        @Override
        public String getAccessibleName() {
            if (myThirdStateEnabled) {
                // Add a state description suffix to the accessible name, so that screen readers
                // announce the state as part of the accessible name.
                return addStateDescription(super.getAccessibleName());
            }
            return super.getAccessibleName();
        }

        private String addStateDescription(String name) {
            switch (getState()) {
                case SELECTED:
                    return AccessibleContextUtil.combineAccessibleStrings(name, " ", "checked");
                case NOT_SELECTED:
                    return AccessibleContextUtil.combineAccessibleStrings(name, " ", "not checked");
                case DONT_CARE:
                    return AccessibleContextUtil.combineAccessibleStrings(name, " ", "partially checked");
                default:
                    return name;
            }
        }
    }
}
