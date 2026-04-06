// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.action;

import consulo.annotation.component.ActionAPI;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a group of actions.
 *
 * @see DefaultActionGroup
 * @see ComputableActionGroup
 * @see CheckedActionGroup
 * @see CompactActionGroup
 */
@ActionAPI
public abstract class ActionGroup extends AnAction {
    public abstract static class Builder {
        protected final List<AnAction> myActions = new ArrayList<>();
        protected boolean myPopup;
        protected LocalizeValue myText = LocalizeValue.empty();

        protected Builder() {
        }

        
        public Builder text(LocalizeValue text) {
            myText = text;
            return this;
        }

        
        public Builder setPopup() {
            myPopup = true;
            return this;
        }

        
        public Builder add(AnAction anAction) {
            myActions.add(anAction);
            return this;
        }

        
        public Builder addAll(List<? extends AnAction> items) {
            myActions.addAll(items);
            return this;
        }

        
        public Builder addAll(AnAction... actions) {
            for (AnAction action : actions) {
                myActions.add(action);
            }
            return this;
        }

        
        public Builder addSeparator() {
            return add(AnSeparator.create());
        }

        
        public Builder addSeparator(LocalizeValue separatorText) {
            return add(AnSeparator.create(separatorText));
        }

        public int size() {
            return myActions.size();
        }

        public boolean isEmpty() {
            return myActions.isEmpty();
        }

        
        public abstract ActionGroup build();
    }

    private static class ImmutableActionGroup extends DumbAwareActionGroup {
        private final AnAction[] myChildren;
        private final boolean myPopup;

        private ImmutableActionGroup(AnAction[] chilren, boolean popup, LocalizeValue actionText) {
            myChildren = chilren;
            myPopup = popup;

            if (actionText.isNotEmpty()) {
                getTemplatePresentation().setText(actionText);
            }
        }

        @Override
        public boolean isPopup() {
            return myPopup;
        }

        
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            return myChildren;
        }
    }

    private static class ImmutableBuilder extends Builder {
        private ImmutableBuilder() {
        }

        
        @Override
        public ActionGroup build() {
            return new ImmutableActionGroup(ContainerUtil.toArray(myActions, ARRAY_FACTORY), myPopup, myText);
        }
    }

    
    public static ActionGroup of(AnAction... actions) {
        return newImmutableBuilder().addAll(actions).build();
    }

    
    public static Builder newImmutableBuilder() {
        return new ImmutableBuilder();
    }

    public static final ActionGroup EMPTY_GROUP = new DumbAwareActionGroup() {
        
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            return EMPTY_ARRAY;
        }
    };

    /**
     * Creates a new {@code ActionGroup} with shortName set to {@code null} and
     * popup set to {@code false}.
     */
    public ActionGroup() {
        // avoid eagerly creating template presentation
    }

    /**
     * Creates a new {@code ActionGroup} with the specified shortName
     * and popup.
     *
     * @param shortName Text that represents a short name for this action group
     * @param popup     {@code true} if this group is a popup, {@code false}
     *                  otherwise
     */
    @Deprecated
    public ActionGroup( String shortName, boolean popup) {
        super(shortName);
        setPopup(popup);
    }

    @Deprecated
    public ActionGroup( String text,  String description, Image icon) {
        super(text, description, icon);
    }

    protected ActionGroup(LocalizeValue text) {
        super(text);
    }

    protected ActionGroup(LocalizeValue text, boolean popup) {
        super(text);
        setPopup(popup);
    }

    protected ActionGroup(LocalizeValue text, LocalizeValue description) {
        super(text, description);
    }

    protected ActionGroup(LocalizeValue text, LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    /**
     * This method can be called in popup menus if {@link Presentation#isPerformGroup()} is {@code true}.
     */
    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
    }

    /**
     * Returns the type of the group.
     *
     * @return {@code true} if the group is a popup, {@code false} otherwise
     */
    public boolean isPopup() {
        return getTemplatePresentation().isPopupGroup();
    }

    public boolean isPopup(String place) {
        return isPopup();
    }

    /**
     * Sets the type of the group.
     *
     * @param popup If {@code true} the group will be shown as a popup in menus.
     */
    public final void setPopup(boolean popup) {
        getTemplatePresentation().setPopupGroup(popup);
    }

    /**
     * Returns the children of the group.
     *
     * @return An array representing children of this group. All returned children must be not {@code null}.
     */
    public abstract AnAction[] getChildren(@Nullable AnActionEvent e);

    
    public AnAction[] getChildren(@Nullable AnActionEvent e, ActionManager actionManager) {
        return getChildren(null);
    }

    @Override
    public boolean isDumbAware() {
        return super.isDumbAware() || getClass() == DefaultActionGroup.class;
    }

    
    public List<AnAction> postProcessVisibleChildren(List<AnAction> visibleChildren) {
        return Collections.unmodifiableList(visibleChildren);
    }

    public boolean hideIfNoVisibleChildren() {
        return false;
    }

    public boolean disableIfNoVisibleChildren() {
        return true;
    }

    /**
     * By default button representing popup action group displays 'dropdown' icon.
     */
    public boolean showBelowArrow() {
        return true;
    }
}
