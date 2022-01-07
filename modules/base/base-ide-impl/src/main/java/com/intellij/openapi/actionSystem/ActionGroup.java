// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.actionSystem;

import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import kava.beans.PropertyChangeListener;
import kava.beans.PropertyChangeSupport;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a group of actions.
 *
 * @see com.intellij.openapi.actionSystem.DefaultActionGroup
 * @see com.intellij.openapi.actionSystem.ComputableActionGroup
 * @see com.intellij.openapi.actionSystem.CheckedActionGroup
 * @see com.intellij.openapi.actionSystem.CompactActionGroup
 */
public abstract class ActionGroup extends AnAction {
  public abstract static class Builder {
    protected final List<AnAction> myActions = new ArrayList<>();

    protected Builder() {
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

    public int size() {
      return myActions.size();
    }

    public boolean isEmpty() {
      return myActions.isEmpty();
    }

    @Nonnull
    public abstract ActionGroup build();
  }

  private static class ImmutableActionGroup extends ActionGroup {
    private AnAction[] myChildren;

    private ImmutableActionGroup(AnAction[] chilren) {
      myChildren = chilren;
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
      return new ImmutableActionGroup(ContainerUtil.toArray(myActions, ARRAY_FACTORY));
    }
  }

  @Nonnull
  public static Builder newImmutableBuilder() {
    return new ImmutableBuilder();
  }

  private boolean myPopup;
  private final PropertyChangeSupport myChangeSupport = new PropertyChangeSupport(this);
  public static final ActionGroup EMPTY_GROUP = new ActionGroup() {
    @Nonnull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
      return EMPTY_ARRAY;
    }
  };

  private Set<AnAction> mySecondaryActions;

  /**
   * The actual value is a Boolean.
   */
  @NonNls
  private static final String PROP_POPUP = "popup";

  private Boolean myDumbAware;

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
  public ActionGroup(@Nls(capitalization = Nls.Capitalization.Title) String shortName, boolean popup) {
    super(shortName);
    setPopup(popup);
  }

  public ActionGroup(@Nls(capitalization = Nls.Capitalization.Title) String text, @Nls(capitalization = Nls.Capitalization.Sentence) String description, Image icon) {
    super(text, description, icon);
  }

  protected ActionGroup(@Nonnull LocalizeValue text) {
    super(text);
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
    return myPopup;
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
    boolean oldPopup = myPopup;
    myPopup = popup;
    firePropertyChange(PROP_POPUP, oldPopup, myPopup);
  }

  public final void addPropertyChangeListener(@Nonnull PropertyChangeListener l) {
    myChangeSupport.addPropertyChangeListener(l);
  }

  public final void removePropertyChangeListener(@Nonnull PropertyChangeListener l) {
    myChangeSupport.removePropertyChangeListener(l);
  }

  protected final void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
    myChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
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

  final void setAsPrimary(@Nonnull AnAction action, boolean isPrimary) {
    if (isPrimary) {
      if (mySecondaryActions != null) {
        mySecondaryActions.remove(action);
      }
    }
    else {
      if (mySecondaryActions == null) {
        mySecondaryActions = new HashSet<>();
      }

      mySecondaryActions.add(action);
    }
  }

  public final boolean isPrimary(@Nonnull AnAction action) {
    return mySecondaryActions == null || !mySecondaryActions.contains(action);
  }

  protected final void replace(@Nonnull AnAction originalAction, @Nonnull AnAction newAction) {
    if (mySecondaryActions != null) {
      if (mySecondaryActions.contains(originalAction)) {
        mySecondaryActions.remove(originalAction);
        mySecondaryActions.add(newAction);
      }
    }
  }

  @Override
  public boolean isDumbAware() {
    if (myDumbAware != null) {
      return myDumbAware;
    }

    boolean dumbAware = super.isDumbAware();
    if (dumbAware) {
      myDumbAware = Boolean.TRUE;
    }
    else {
      if (myDumbAware == null) {
        Class<?> declaringClass = ReflectionUtil.getMethodDeclaringClass(getClass(), "update", AnActionEvent.class);
        myDumbAware = AnAction.class.equals(declaringClass) || ActionGroup.class.equals(declaringClass);
      }
    }

    return myDumbAware;
  }

  public boolean hideIfNoVisibleChildren() {
    return false;
  }

  public boolean disableIfNoVisibleChildren() {
    return true;
  }
}
