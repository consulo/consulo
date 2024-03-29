// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.tree;

import consulo.util.concurrent.Promise;

import jakarta.annotation.Nonnull;
import javax.swing.tree.TreePath;
import java.util.function.Function;

public interface TreeVisitor {
  /**
   * @param path a currently visited path
   * @return an action that controls visiting a tree
   */
  @Nonnull
  Action visit(@Nonnull TreePath path);

  enum Action {
    /**
     * Interrupt visiting a tree structure.
     */
    INTERRUPT,
    /**
     * Continue visiting the node children.
     */
    CONTINUE,
    /**
     * Continue without visiting the node children.
     */
    SKIP_CHILDREN,
    /**
     * Continue without visiting the node siblings.
     */
    SKIP_SIBLINGS,
  }


  /**
   * Represents a tree model that accepts a tree visitor and promises a result.
   */
  interface Acceptor {
    /**
     * @param visitor an object that controls visiting a tree structure
     * @return a promise that will be resolved when visiting is finished
     */
    @Nonnull
    Promise<TreePath> accept(@Nonnull TreeVisitor visitor);
  }


  abstract class ByComponent<C, T> implements TreeVisitor {
    private final Function<TreePath, T> converter;
    private final C component;

    public ByComponent(@Nonnull C component, @Nonnull Function<Object, ? extends T> converter) {
      this.converter = currentPath -> converter.apply(currentPath.getLastPathComponent());
      this.component = component;
    }

    public ByComponent(@Nonnull C component, @Nonnull Class<? extends T> type) {
      this(component, object -> type.isInstance(object) ? type.cast(object) : null);
    }

    @Nonnull
    @Override
    public Action visit(@Nonnull TreePath path) {
      return visit(converter.apply(path));
    }

    /**
     * @param component a last component of the current path
     * @return an action that controls visiting a tree
     */
    @Nonnull
    protected Action visit(T component) {
      if (component == null) return Action.SKIP_CHILDREN;
      if (matches(component, this.component)) return Action.INTERRUPT;
      if (contains(component, this.component)) return Action.CONTINUE;
      return Action.SKIP_CHILDREN;
    }

    /**
     * @param pathComponent a last component of the current path
     * @param thisComponent a seeking component
     * @return {@code true} if both components match each other
     */
    protected boolean matches(@Nonnull T pathComponent, @Nonnull C thisComponent) {
      return pathComponent.equals(thisComponent);
    }

    /**
     * @param pathComponent a last component of the current path
     * @param thisComponent a seeking component
     * @return {@code true} if the first component may contain the second one
     */
    protected abstract boolean contains(@Nonnull T pathComponent, @Nonnull C thisComponent);
  }


  class ByTreePath<T> implements TreeVisitor {
    private final Function<TreePath, T> converter;
    private final boolean ignoreRoot;
    private final TreePath path;
    private final int count;

    public ByTreePath(@Nonnull TreePath path, @Nonnull Function<Object, ? extends T> converter) {
      this(false, path, converter);
    }

    public ByTreePath(boolean ignoreRoot, @Nonnull TreePath path, @Nonnull Function<Object, ? extends T> converter) {
      this.converter = currentPath -> converter.apply(currentPath.getLastPathComponent());
      this.ignoreRoot = ignoreRoot;
      this.path = path;
      this.count = ignoreRoot ? path.getPathCount() + 1 : path.getPathCount();
    }

    @Nonnull
    @Override
    public Action visit(@Nonnull TreePath path) {
      return ignoreRoot && null == path.getParentPath() ? Action.CONTINUE : visit(path, converter.apply(path));
    }

    /**
     * @param path      a currently visited path
     * @param component a corresponding component
     * @return an action that controls visiting a tree
     */
    @Nonnull
    protected Action visit(@Nonnull TreePath path, T component) {
      if (component == null) return Action.SKIP_CHILDREN;
      int count = path.getPathCount();
      if (count < this.count) {
        TreePath parent = this.path.getParentPath();
        while (++count < this.count && parent != null) parent = parent.getParentPath();
        boolean found = parent != null && matches(component, parent.getLastPathComponent());
        return !found ? Action.SKIP_CHILDREN : Action.CONTINUE;
      }
      else {
        boolean found = count > this.count || matches(component, this.path.getLastPathComponent());
        return !found ? Action.SKIP_CHILDREN : visit(path, component, count - this.count);
      }
    }

    /**
     * @param path      a currently visited path
     * @param component a corresponding component
     * @param depth     a depth starting from the found node
     * @return an action that controls visiting a tree
     */
    @Nonnull
    @SuppressWarnings("unused")
    protected Action visit(@Nonnull TreePath path, @Nonnull T component, int depth) {
      return depth == 0 ? Action.INTERRUPT : Action.SKIP_CHILDREN;
    }

    /**
     * @param pathComponent a last component of the current path
     * @param thisComponent a component of the seeking path at the same level
     * @return {@code true} if both components match each other
     */
    protected boolean matches(@Nonnull T pathComponent, @Nonnull Object thisComponent) {
      return pathComponent.equals(thisComponent);
    }
  }
}
