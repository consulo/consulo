// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.action;

import consulo.annotation.component.ActionAPI;
import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

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

        @Nonnull
        public Builder text(@Nonnull LocalizeValue text) {
            myText = text;
            return this;
        }

        @Nonnull
        public Builder setPopup() {
            myPopup = true;
            return this;
        }

        @Nonnull
        public Builder add(@Nonnull AnAction anAction) {
            myActions.add(anAction);
            return this;
        }

        @Nonnull
        public Builder addAll(@Nonnull List<? extends AnAction> items) {
            myActions.addAll(items);
            return this;
        }

        @Nonnull
        public Builder addAll(@Nonnull AnAction... actions) {
            for (AnAction action : actions) {
                myActions.add(action);
            }
            return this;
        }

        @Nonnull
        public Builder addSeparator() {
            return add(AnSeparator.create());
        }

        @Nonnull
        public Builder addSeparator(@Nonnull LocalizeValue separatorText) {
            return add(AnSeparator.create(separatorText));
        }

        public int size() {
            return myActions.size();
        }

        public boolean isEmpty() {
            return myActions.isEmpty();
        }

        @Nonnull
        public abstract ActionGroup build();
    }

    private static class ImmutableActionGroup extends DumbAwareActionGroup {
        private final AnAction[] myChildren;
        private final boolean myPopup;

        private ImmutableActionGroup(AnAction[] chilren, boolean popup, LocalizeValue actionText) {
            myChildren = chilren;
            myPopup = popup;

            if (actionText != LocalizeValue.empty()) {
                getTemplatePresentation().setTextValue(actionText);
            }
        }

        @Override
        public boolean isPopup() {
            return myPopup;
        }

        @Nonnull
        @Override
        public AnAction[] getChildren(@Nullable AnActionEvent e) {
            return myChildren;
        }
    }

    private static class ImmutableBuilder extends Builder {
        private ImmutableBuilder() {
        }

        @Nonnull
        @Override
        public ActionGroup build() {
            return new ImmutableActionGroup(ContainerUtil.toArray(myActions, ARRAY_FACTORY), myPopup, myText);
        }
    }

    @Nonnull
    public static ActionGroup of(@Nonnull AnAction... actions) {
        return newImmutableBuilder().addAll(actions).build();
    }

    @Nonnull
    public static Builder newImmutableBuilder() {
        return new ImmutableBuilder();
    }

    public static final ActionGroup EMPTY_GROUP = new DumbAwareActionGroup() {
        @Nonnull
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
    public ActionGroup(@Nls(capitalization = Nls.Capitalization.Title) String shortName, boolean popup) {
        super(shortName);
        setPopup(popup);
    }

    @Deprecated
    public ActionGroup(@Nls(capitalization = Nls.Capitalization.Title) String text, @Nls(capitalization = Nls.Capitalization.Sentence) String description, Image icon) {
        super(text, description, icon);
    }

    protected ActionGroup(@Nonnull LocalizeValue text) {
        super(text);
    }

    protected ActionGroup(@Nonnull LocalizeValue text, boolean popup) {
        super(text);
        setPopup(popup);
    }

    protected ActionGroup(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description) {
        super(text, description);
    }

    protected ActionGroup(@Nonnull LocalizeValue text, @Nonnull LocalizeValue description, @Nullable Image icon) {
        super(text, description, icon);
    }

    /**
     * This method can be called in popup menus if {@link #canBePerformed(DataContext)} is {@code true}.
     */
    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
    }

    /**
     * @return {@code true} if {@link #actionPerformed(AnActionEvent)} should be called.
     */
    public boolean canBePerformed(@Nonnull DataContext context) {
        return false;
    }

    /**
     * Returns the type of the group.
     *
     * @return {@code true} if the group is a popup, {@code false} otherwise
     */
    public boolean isPopup() {
        return getTemplatePresentation().isPopupGroup();
    }

    public boolean isPopup(@Nonnull String place) {
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
    @Nonnull
    public abstract AnAction[] getChildren(@Nullable AnActionEvent e);

    @Nonnull
    public AnAction[] getChildren(@Nullable AnActionEvent e, @Nonnull ActionManager actionManager) {
        return getChildren(null);
    }

    @Override
    public boolean isDumbAware() {
        return super.isDumbAware() || getClass() == DefaultActionGroup.class;
    }

    @Nonnull
    public List<AnAction> postProcessVisibleChildren(@Nonnull List<AnAction> visibleChildren) {
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
