// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.tree;

import consulo.application.util.Queryable;
import consulo.logging.Logger;
import consulo.navigation.NavigationItem;
import consulo.util.lang.Comparing;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

public abstract class TreeNode<T> extends PresentableNodeDescriptor<TreeNode<T>> implements NavigationItem, Queryable.Contributor, LeafState.Supplier {
  private static final Logger LOG = Logger.getInstance(TreeNode.class);
  private TreeNode myParent;
  private Object myValue;
  private boolean myNullValueSet;
  private final boolean myNodeWrapper;
  static final Object TREE_WRAPPER_VALUE = new Object();

  protected TreeNode(@Nonnull T value) {
    super(null);
    myNodeWrapper = setInternalValue(value);
  }

  @Nonnull
  public abstract Collection<? extends TreeNode> getChildren();

  protected boolean hasProblemFileBeneath() {
    return false;
  }

  @Override
  public PresentableNodeDescriptor getChildToHighlightAt(int index) {
    final Collection<? extends TreeNode> kids = getChildren();
    int i = 0;
    for (final TreeNode kid : kids) {
      if (i == index) return kid;
      i++;
    }

    return null;
  }

  @Override
  protected void postprocess(@Nonnull PresentationData presentation) {
    setForcedForeground(presentation);
  }

  protected void setForcedForeground(@Nonnull PresentationData presentation) {
  }

  @Override
  protected boolean shouldUpdateData() {
    return getEqualityObject() != null;
  }

  @Nonnull
  @Override
  public LeafState getLeafState() {
    if (isAlwaysShowPlus()) return LeafState.NEVER;
    if (isAlwaysLeaf()) return LeafState.ALWAYS;
    return LeafState.DEFAULT;
  }

  public boolean isAlwaysShowPlus() {
    return false;
  }

  public boolean isAlwaysLeaf() {
    return false;
  }

  public boolean isAlwaysExpand() {
    return false;
  }

  @Override
  @Nullable
  public final TreeNode<T> getElement() {
    return getEqualityObject() != null ? this : null;
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) return true;
    if (object == null || !object.getClass().equals(getClass())) return false;
    // we should not change this behaviour if value is set to null
    return object instanceof TreeNode && Comparing.equal(myValue, ((TreeNode)object).myValue);
  }

  @Override
  public int hashCode() {
    // we should not change hash code if value is set to null
    Object value = myValue;
    return value == null ? 0 : value.hashCode();
  }

  public final TreeNode getParent() {
    return myParent;
  }

  public final void setParent(TreeNode parent) {
    myParent = parent;
  }

  @Override
  public final NodeDescriptor getParentDescriptor() {
    return myParent;
  }

  public final T getValue() {
    Object value = getEqualityObject();
    return value == null ? null : (T)TreeAnchorizer.getService().retrieveElement(value);
  }

  public final void setValue(T value) {
    boolean debug = !myNodeWrapper && LOG.isDebugEnabled();
    int hash = !debug ? 0 : hashCode();
    myNullValueSet = value == null || setInternalValue(value);
    if (debug && hash != hashCode()) {
      LOG.warn("hash code changed: " + myValue);
    }
  }

  /**
   * Stores the anchor to new value if it is not {@code null}
   *
   * @param value a new value to set
   * @return {@code true} if the specified value is {@code null} and the anchor is not changed
   */
  private boolean setInternalValue(@Nonnull T value) {
    if (value == TREE_WRAPPER_VALUE) return true;
    myValue = TreeAnchorizer.getService().createAnchor(value);
    return false;
  }

  public final Object getEqualityObject() {
    return myNullValueSet ? null : myValue;
  }

  @Nullable
  @TestOnly
  public String toTestString(@Nullable Queryable.PrintInfo printInfo) {
    if (getValue() instanceof Queryable) {
      String text = Queryable.Util.print((Queryable)getValue(), printInfo, this);
      if (text != null) return text;
    }

    return getTestPresentation();
  }

  @Override
  public void apply(@Nonnull Map<String, String> info) {
  }

  /**
   * @deprecated use {@link #toTestString(Queryable.PrintInfo)} instead
   */
  @Deprecated
  @Nullable
  @NonNls
  @TestOnly
  public String getTestPresentation() {
    if (myName != null) {
      return myName;
    }
    if (getValue() != null) {
      return getValue().toString();
    }
    return null;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public void navigate(boolean requestFocus) {
  }

  @Override
  public boolean canNavigate() {
    return false;
  }

  @Override
  public boolean canNavigateToSource() {
    return false;
  }

  @Nullable
  protected final Object getParentValue() {
    TreeNode parent = getParent();
    return parent == null ? null : parent.getValue();
  }


  public boolean canRepresent(final Object element) {
    return Comparing.equal(getValue(), element);
  }

  /**
   * @deprecated use {@link #getPresentation()} instead
   */
  @Deprecated
  protected String getToolTip() {
    return getPresentation().getTooltip();
  }
}
