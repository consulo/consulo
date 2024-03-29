// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.tree;

import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.Promise;

import jakarta.annotation.Nonnull;
import javax.swing.tree.TreePath;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public abstract class AbstractTreeWalker<N> {
  private enum State {
    STARTED,
    REQUESTED,
    PAUSED,
    FINISHED,
    FAILED
  }

  private final AtomicReference<State> state = new AtomicReference<>();
  private final AsyncPromise<TreePath> promise = new AsyncPromise<>();
  private final ArrayDeque<ArrayDeque<N>> stack = new ArrayDeque<>();
  private final Function<? super N, Object> converter;
  private final TreeVisitor visitor;
  private volatile TreePath current;

  /**
   * Creates a new tree walker with the specified tree visitor.
   *
   * @param visitor an object that controls visiting a tree structure
   */
  public AbstractTreeWalker(@Nonnull TreeVisitor visitor) {
    this(visitor, node -> node);
  }

  /**
   * Creates a new tree walker with the specified node converter,
   * which allows to generate a tree path expected by the given tree visitor.
   *
   * @param visitor   an object that controls visiting a tree structure
   * @param converter a node converter for the path components
   */
  public AbstractTreeWalker(@Nonnull TreeVisitor visitor, Function<? super N, Object> converter) {
    this.converter = converter;
    this.visitor = visitor;
  }

  /**
   * Returns a list of child nodes for the specified node.
   * This method is called by the walker only if the visitor
   * returned the {@link TreeVisitor.Action#CONTINUE CONTINUE} action.
   * The walker will be paused if it returns {@code null}.
   * To continue user should call the {@link #setChildren} method.
   *
   * @param node a node in a tree structure
   * @return children for the specified node or {@code null} if children will be set later
   */
  protected abstract Collection<N> getChildren(@Nonnull N node);

  /**
   * Sets the children, awaited by the walker, and continues to traverse a tree structure.
   *
   * @param children a list of child nodes for the node specified in the {@link #getChildren} method
   * @throws IllegalStateException if it is called in unexpected state
   */
  public void setChildren(Collection<? extends N> children) {
    boolean paused = state.compareAndSet(State.PAUSED, State.STARTED);
    if (!paused && !state.compareAndSet(State.REQUESTED, State.STARTED)) throw new IllegalStateException();
    stack.push(children == null ? new ArrayDeque<>() : new ArrayDeque<>(children));
    if (paused) processNextPath();
  }

  /**
   * @return a promise that will be resolved when visiting is finished
   */
  @Nonnull
  public Promise<TreePath> promise() {
    return promise;
  }

  /**
   * Stops visiting a tree structure by specifying a cause.
   */
  public void setError(@Nonnull Throwable error) {
    state.set(State.FAILED);
    promise.setError(error);
  }

  /**
   * Starts visiting a tree structure from the specified root node.
   *
   * @param node a tree root or {@code null} if nothing to traverse
   */
  public void start(N node) {
    start(null, node);
  }

  /**
   * Starts visiting a tree structure from the specified node.
   *
   * @param parent a parent tree path or {@code null} for a root node
   * @param node   a tree node or {@code null} if nothing to traverse
   */
  public void start(TreePath parent, N node) {
    TreePath result = null;
    if (node != null) {
      try {
        TreePath path = TreePathUtil.createTreePath(parent, converter.apply(node));
        switch (visitor.visit(path)) {
          case CONTINUE:
            update(null, State.REQUESTED);
            if (processChildren(path, node)) processNextPath();
            return;
          case INTERRUPT:
            result = path;
            break;
          case SKIP_CHILDREN:
            break;
          case SKIP_SIBLINGS:
            break;
        }
      }
      catch (Exception error) {
        setError(error);
      }
    }
    update(null, State.FINISHED);
    promise.setResult(result);
  }

  /**
   * @param path a path to the specified node
   * @param node a node to get children to process
   * @return {@code false} if the walker should be pause
   */
  private boolean processChildren(@Nonnull TreePath path, @Nonnull N node) {
    current = path;
    Collection<N> children = getChildren(node);
    if (children == null) return !state.compareAndSet(State.REQUESTED, State.PAUSED);
    update(State.REQUESTED, State.STARTED);
    stack.push(new ArrayDeque<>(children));
    return true;
  }

  private void processNextPath() {
    try {
      while (State.STARTED == state.get()) {
        ArrayDeque<N> siblings = stack.peek();
        if (siblings == null) {
          update(State.STARTED, State.FINISHED);
          current = null;
          promise.setResult(null);
          return; // nothing to process
        }
        N node = siblings.poll();
        if (node == null) {
          TreePath path = current;
          if (path == null) throw new IllegalStateException();
          if (siblings != stack.poll()) throw new IllegalStateException();
          current = path.getParentPath();
        }
        else {
          TreePath path = TreePathUtil.createTreePath(current, converter.apply(node));
          switch (visitor.visit(path)) {
            case CONTINUE:
              update(State.STARTED, State.REQUESTED);
              if (processChildren(path, node)) break;
              return; // stop processing and wait for setChildren
            case INTERRUPT:
              update(State.STARTED, State.FINISHED);
              current = null;
              stack.clear();
              promise.setResult(path);
              return; // path is found
            case SKIP_SIBLINGS:
              siblings.clear();
              break;
            case SKIP_CHILDREN:
              break;
          }
        }
      }
    }
    catch (Exception error) {
      setError(error);
    }
  }

  private void update(State expected, @Nonnull State replacement) {
    if (!state.compareAndSet(expected, replacement)) throw new IllegalStateException();
  }
}
