// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ui.tree;

import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.logging.Logger;
import consulo.ui.ex.awt.tree.TreeVisitor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class AbstractTreeNodeVisitor<T> implements TreeVisitor {
  protected static final Logger LOG = Logger.getInstance(AbstractTreeNodeVisitor.class);
  private final Supplier<? extends T> supplier;
  private final Predicate<? super TreePath> predicate;

  /**
   * @param supplier  that provides an element to search in a tree
   * @param predicate that controls visiting children of found node:
   *                  {@code null} to interrupt visiting,
   *                  {@code true} to continue visiting with children,
   *                  {@code false} to continue visiting without children
   */
  public AbstractTreeNodeVisitor(@Nonnull Supplier<? extends T> supplier, @Nullable Predicate<? super TreePath> predicate) {
    this.supplier = supplier;
    this.predicate = predicate;
  }

  /**
   * @return an element to search in a tree or {@code null} if it is obsolete
   */
  @Nullable
  public final T getElement() {
    return supplier.get();
  }

  @Nonnull
  @Override
  public Action visit(@Nonnull TreePath path) {
    if (LOG.isTraceEnabled()) LOG.debug("process ", path);
    T element = getElement();
    if (element == null) return Action.SKIP_SIBLINGS;
    Object component = path.getLastPathComponent();
    if (component instanceof AbstractTreeNode) {
      return visit(path, (AbstractTreeNode)component, element);
    }
    if (component instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)component;
      Object object = node.getUserObject();
      if (object instanceof AbstractTreeNode) {
        return visit(path, (AbstractTreeNode)object, element);
      }
      else if (object instanceof String) {
        LOG.debug("ignore children: ", object);
      }
      else {
        LOG.warn(object == null ? "no object" : "unexpected object " + object.getClass());
      }
    }
    else if (component instanceof String) {
      LOG.debug("ignore children: ", component);
    }
    else {
      LOG.warn(component == null ? "no component" : "unexpected component " + component.getClass());
    }
    return Action.SKIP_CHILDREN;
  }

  /**
   * @param path    a currently visited path
   * @param node    a node of a tree structure
   * @param element an element to find
   * @return an action that controls visiting a tree
   */
  @Nonnull
  protected Action visit(@Nonnull TreePath path, @Nonnull AbstractTreeNode node, @Nonnull T element) {
    if (matches(node, element)) {
      LOG.debug("found ", path);
      if (predicate == null) return Action.INTERRUPT;
      if (predicate.test(path)) return Action.CONTINUE;
    }
    else if (contains(node, element)) {
      LOG.debug("visit ", path);
      return Action.CONTINUE;
    }
    return Action.SKIP_CHILDREN;
  }

  /**
   * @param node    a node of a tree structure
   * @param element an element to find
   * @return {@code true} if the specified node represents the given element
   */
  protected boolean matches(@Nonnull AbstractTreeNode node, @Nonnull T element) {
    return node.canRepresent(element);
  }

  /**
   * @param node    a node of a tree structure
   * @param element an element to find
   * @return {@code true} if the specified node is an ancestor of the given element
   */
  protected boolean contains(@Nonnull AbstractTreeNode node, @Nonnull T element) {
    T content = getContent(node);
    return content != null && isAncestor(content, element);
  }

  /**
   * @param node a node of a tree structure
   * @return a content of the specified tree or {@code null}
   */
  protected T getContent(@Nonnull AbstractTreeNode node) {
    return null;
  }

  /**
   * @param content a content of a tree node
   * @param element an element to find
   * @return {@code true} if the specified content is an ancestor of the given element
   */
  protected boolean isAncestor(@Nonnull T content, @Nonnull T element) {
    return false;
  }
}
