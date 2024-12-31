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
package consulo.ui.ex.action;

import consulo.annotation.DeprecationInfo;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Size;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Represents a toolbar with a visual presentation.
 *
 * @see ActionManager#createActionToolbar(String, ActionGroup, boolean)
 */
public interface ActionToolbar {
    enum Style {
        HORIZONTAL,
        VERTICAL {
            @Override
            public boolean isHorizontal() {
                return false;
            }
        },
        /**
         * Inplace toolbar inside some components like {@link consulo.ui.TextBox}
         *
         * Always horizontal
         */
        INPLACE,
        /**
         * Toolbar where all buttons are regulars (not short version without text, and borders) {@link consulo.ui.Button}
         *
         * Always horizontal
         */
        BUTTON;

        public boolean isHorizontal() {
            return true;
        }
    }

    String ACTION_TOOLBAR_PROPERTY_KEY = "ACTION_TOOLBAR";

    /**
     * This is default layout policy for the toolbar. It defines that
     * all toolbar component are in one row / column and they are not wrapped
     * when toolbar is small
     */
    int NOWRAP_LAYOUT_POLICY = 0;
    /**
     * This is experimental layout policy which allow toolbar to
     * wrap components in multiple rows.
     */
    int WRAP_LAYOUT_POLICY = 1;
    /**
     * This is experimental layout policy which allow toolbar auto-hide and show buttons that don't fit into actual side
     */
    int AUTO_LAYOUT_POLICY = 2;

    /**
     * This is default minimum size of the toolbar button, without scaling
     */
    Size DEFAULT_MINIMUM_BUTTON_SIZE = new Size(25, 25);

    /**
     * Constraint that's passed to <code>Container.add</code> when ActionButton is added to the toolbar.
     */
    String ACTION_BUTTON_CONSTRAINT = "Constraint.ActionButton";

    /**
     * Constraint that's passed to <code>Container.add</code> when a custom component is added to the toolbar.
     */
    String CUSTOM_COMPONENT_CONSTRAINT = "Constraint.CustomComponent";

    /**
     * Constraint that's passed to <code>Container.add</code> when a Separator is added to the toolbar.
     */
    String SEPARATOR_CONSTRAINT = "Constraint.Separator";

    /**
     * Constraint that's passed to <code>Container.add</code> when a secondary action is added to the toolbar.
     */
    @Deprecated
    String SECONDARY_ACTION_CONSTRAINT = "Constraint.SecondaryAction";

    /**
     * @return component which represents the tool bar on UI
     */
    @Nonnull
    default javax.swing.JComponent getComponent() {
        return (javax.swing.JComponent) TargetAWT.to(getUIComponent());
    }

    /**
     * @return component which represents the tool bar on UI
     */
    @Nonnull
    default Component getUIComponent() {
        throw new AbstractMethodError();
    }

    /**
     * @return current layout policy
     * @see #NOWRAP_LAYOUT_POLICY
     * @see #WRAP_LAYOUT_POLICY
     */
    int getLayoutPolicy();

    /**
     * Sets new component layout policy. Method accepts {@link #WRAP_LAYOUT_POLICY} and
     * {@link #NOWRAP_LAYOUT_POLICY} values.
     */
    void setLayoutPolicy(int layoutPolicy);

    /**
     * Forces update of the all actions in the toolbars. Actions, however, normally updated automatially every 500msec.
     */
    @RequiredUIAccess
    void updateActionsImmediately();

    boolean hasVisibleActions();

    /**
     * @param component will be used for datacontext computations
     */
    default void setTargetComponent(final javax.swing.JComponent component) {
        throw new AbstractMethodError();
    }

    default void setTargetUIComponent(@Nonnull Component component) {
        setTargetComponent((javax.swing.JComponent) TargetAWT.to(component));
    }

    DataContext getToolbarDataContext();

    @Nonnull
    List<AnAction> getActions();

    /**
     * Enables showing titles of separators as labels in the toolbar (off by default).
     */
    default void setShowSeparatorTitles(boolean showSeparatorTitles) {
    }

    /**
     * By default minimum size is to show chevron only.
     * If this option is {@code true} toolbar shows at least one (the first) component plus chevron (if need)
     */
    @Deprecated
    default void setForceShowFirstComponent(boolean showFirstComponent) {
    }

    // region deprecated method
    /**
     * Sets minimum size of toolbar button. By default all buttons
     * at toolbar has 25x25 pixels size.
     *
     * @throws IllegalArgumentException if <code>size</code>
     *                                  is <code>null</code>
     */
    @Deprecated
    default void setMinimumButtonSize(@Nonnull java.awt.Dimension size) {
        setMinimumButtonSize(new Size(size.width, size.height));
    }

    /**
     * Sets minimum size of toolbar button. By default all buttons
     * at toolbar has 25x25 pixels size.
     *
     * @throws IllegalArgumentException if <code>size</code>
     *                                  is <code>null</code>
     */
    @Deprecated
    default void setMinimumButtonSize(@Nonnull Size size) {
    }

    @Deprecated
    default void setReservePlaceAutoPopupIcon(final boolean reserve) {
    }

    @Deprecated
    @DeprecationInfo("Use different style")
    default void setMiniMode(boolean minimalMode) {
    }

    @Deprecated
    default void setSecondaryActionsTooltip(@Nonnull LocalizeValue secondaryActionsTooltip) {
    }

    @Deprecated
    default void setSecondaryActionsIcon(Image icon) {
    }

    @Deprecated
    default void setSecondaryActionsIcon(Image icon, boolean hideDropdownIcon) {
    }

    @Deprecated
    default void setSecondaryActionsShortcut(@Nonnull String secondaryActionsShortcut) {
    }

    /**
     * Forces the minimum size of the toolbar to show all buttons, When set to {@code true}. By default ({@code false}) the
     * toolbar will shrink further and show the auto popup chevron button.
     */
    @Deprecated
    default void setForceMinimumSize(boolean force) {
    }
    
    //endregion
}
