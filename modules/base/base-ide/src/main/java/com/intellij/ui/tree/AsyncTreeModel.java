// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ui.tree;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.ui.LoadingNode;
import com.intellij.util.Consumer;
import com.intellij.util.concurrency.Command;
import com.intellij.util.concurrency.Invoker;
import com.intellij.util.concurrency.InvokerSupplier;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.SmartHashSet;
import com.intellij.util.ui.tree.AbstractTreeModel;
import com.intellij.util.ui.tree.TreeModelAdapter;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Obsolescent;
import org.jetbrains.concurrency.Promise;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.jetbrains.concurrency.Promises.rejectedPromise;

/**
 * @author Sergey.Malenkov
 */
public final class AsyncTreeModel extends AbstractTreeModel implements Identifiable, Searchable, Navigatable, TreeVisitor.Acceptor {
  private static final Logger LOG = Logger.getInstance(AsyncTreeModel.class);
  private final Command.Processor processor;
  private final Tree tree = new Tree();
  private final TreeModel model;
  private final boolean showLoadingNode;
  private final TreeModelListener listener = new TreeModelAdapter() {
    @Override
    protected void process(@Nonnull TreeModelEvent event, @Nonnull EventType type) {
      TreePath path = event.getTreePath();
      if (path == null) {
        // request a new root from model according to the specification
        processor.process(new CmdGetRoot("Reload root", null));
        return;
      }
      Object object = path.getLastPathComponent();
      if (object == null) {
        LOG.warn("unsupported path: " + path);
        return;
      }
      if (path.getParentPath() == null && type == EventType.StructureChanged) {
        // set a new root object according to the specification
        processor.process(new CmdGetRoot("Update root", object));
        return;
      }
      onValidThread(() -> {
        Node node = tree.map.get(object);
        if (node == null) {
          LOG.debug("ignore updating of nonexistent node: ", object);
        }
        else if (type == EventType.NodesChanged) {
          // the object is already updated, so we should not start additional command to update
          AsyncTreeModel.this.treeNodesChanged(event.getTreePath(), event.getChildIndices(), event.getChildren());
        }
        else if (node.isLoadingRequired()) {
          // update the object presentation only, if its children are not requested yet
          AsyncTreeModel.this.treeNodesChanged(event.getTreePath(), null, null);
        }
        else if (type == EventType.NodesInserted) {
          processor.process(new CmdGetChildren("Insert children", node, false));
        }
        else if (type == EventType.NodesRemoved) {
          processor.process(new CmdGetChildren("Remove children", node, false));
        }
        else {
          processor.process(new CmdGetChildren("Update children", node, true));
        }
      });
    }
  };

  /**
   * @deprecated use {@link #AsyncTreeModel(TreeModel, Disposable)} instead
   */
  @Deprecated
  public AsyncTreeModel(@Nonnull TreeModel model) {
    this(model, true);
  }

  /**
   * @deprecated use {@link #AsyncTreeModel(TreeModel, boolean, Disposable)} instead
   */
  @Deprecated
  public AsyncTreeModel(@Nonnull TreeModel model, boolean showLoadingNode) {
    if (model instanceof Disposable) {
      Disposer.register(this, (Disposable)model);
    }
    Invoker foreground = new Invoker.EDT(this);
    Invoker background = foreground;
    if (model instanceof InvokerSupplier) {
      InvokerSupplier supplier = (InvokerSupplier)model;
      background = supplier.getInvoker();
    }
    this.processor = new Command.Processor(foreground, background);
    this.model = model;
    this.model.addTreeModelListener(listener);
    this.showLoadingNode = showLoadingNode;
  }

  public AsyncTreeModel(@Nonnull TreeModel model, @Nonnull Disposable parent) {
    this(model, true, parent);
  }

  public AsyncTreeModel(@Nonnull TreeModel model, boolean showLoadingNode, @Nonnull Disposable parent) {
    this(model, showLoadingNode);
    Disposer.register(parent, this);
  }

  @Override
  public void dispose() {
    super.dispose();
    model.removeTreeModelListener(listener);
  }

  @Override
  public Object getUniqueID(@Nonnull TreePath path) {
    return model instanceof Identifiable ? ((Identifiable)model).getUniqueID(path) : null;
  }

  @Nonnull
  @Override
  public Promise<TreePath> getTreePath(Object object) {
    if (disposed) return rejectedPromise();
    return resolve(model instanceof Searchable ? ((Searchable)model).getTreePath(object) : null);
  }

  @Nonnull
  @Override
  public Promise<TreePath> nextTreePath(@Nonnull TreePath path, Object object) {
    if (disposed) return rejectedPromise();
    return resolve(model instanceof Navigatable ? ((Navigatable)model).nextTreePath(path, object) : null);
  }

  @Nonnull
  @Override
  public Promise<TreePath> prevTreePath(@Nonnull TreePath path, Object object) {
    if (disposed) return rejectedPromise();
    return resolve(model instanceof Navigatable ? ((Navigatable)model).prevTreePath(path, object) : null);
  }

  @Nonnull
  public Promise<TreePath> resolve(TreePath path) {
    AsyncPromise<TreePath> async = new AsyncPromise<>();
    onValidThread(() -> resolve(async, path));
    return async;
  }

  @Nonnull
  private Promise<TreePath> resolve(Promise<TreePath> promise) {
    if (promise == null && isValidThread()) {
      return rejectedPromise();
    }
    AsyncPromise<TreePath> async = new AsyncPromise<>();
    if (promise == null) {
      onValidThread(() -> async.setError("rejected"));
    }
    else {
      promise.onError(onValidThread(async::setError));
      promise.onSuccess(onValidThread(path -> resolve(async, path)));
    }
    return async;
  }

  private void resolve(@Nonnull AsyncPromise<TreePath> async, TreePath path) {
    LOG.debug("resolve path: ", path);
    if (path == null) {
      async.setError("path is null");
      return;
    }
    Object object = path.getLastPathComponent();
    if (object == null) {
      async.setError("path is wrong");
      return;
    }
    accept(new TreeVisitor.ByTreePath<>(path, o -> o)).onProcessed(result -> {
      if (result == null) {
        async.setError("path not found");
        return;
      }
      async.setResult(result);
    });
  }

  @Override
  public Object getRoot() {
    if (disposed || !isValidThread()) return null;
    promiseRootEntry();
    Node node = tree.root;
    return node == null ? null : node.object;
  }

  @Override
  public Object getChild(Object object, int index) {
    List<Node> children = getEntryChildren(object);
    return 0 <= index && index < children.size() ? children.get(index).object : null;
  }

  @Override
  public int getChildCount(Object object) {
    return getEntryChildren(object).size();
  }

  @Override
  public boolean isLeaf(Object object) {
    Node node = getEntry(object);
    return node == null || node.leaf;
  }

  @Override
  public void valueForPathChanged(@Nonnull TreePath path, Object value) {
    processor.background.runOrInvokeLater(() -> model.valueForPathChanged(path, value));
  }

  @Override
  public int getIndexOfChild(Object object, Object child) {
    if (child != null) {
      List<Node> children = getEntryChildren(object);
      for (int i = 0; i < children.size(); i++) {
        if (child.equals(children.get(i).object)) return i;
      }
    }
    return -1;
  }

  /**
   * Starts visiting the tree structure with loading all needed children.
   *
   * @param visitor an object that controls visiting a tree structure
   * @return a promise that will be resolved when visiting is finished
   */
  @Override
  @Nonnull
  public Promise<TreePath> accept(@Nonnull TreeVisitor visitor) {
    return accept(visitor, true);
  }

  /**
   * Starts visiting the tree structure.
   *
   * @param visitor      an object that controls visiting a tree structure
   * @param allowLoading load all needed children if {@code true}
   * @return a promise that will be resolved when visiting is finished
   */
  @Nonnull
  public Promise<TreePath> accept(@Nonnull TreeVisitor visitor, boolean allowLoading) {
    AbstractTreeWalker<Node> walker = new AbstractTreeWalker<Node>(visitor, node -> node.object) {
      @Override
      protected Collection<Node> getChildren(@Nonnull Node node) {
        if (node.leaf || !allowLoading) return node.getChildren();
        promiseChildren(node).onSuccess(parent -> setChildren(parent.getChildren())).onError(this::setError);
        return null;
      }
    };
    if (allowLoading) {
      // start visiting on the background thread to ensure that root node is already invalidated
      processor.background.invokeLater(() -> onValidThread(() -> promiseRootEntry().onSuccess(walker::start).onError(walker::setError)));
    }
    else {
      onValidThread(() -> walker.start(tree.root));
    }
    return walker.promise();
  }

  /**
   * @return {@code true} if this model is updating its structure
   */
  public boolean isProcessing() {
    if (processor.getTaskCount() > 0) return true;
    ObsolescentCommand command = tree.queue.get();
    return command != null && command.isPending();
  }

  private boolean isValidThread() {
    if (processor.foreground.isValidThread()) return true;
    LOG.warn(new IllegalStateException("AsyncTreeModel is used from unexpected thread: " + Thread.currentThread()));
    return false;
  }

  public void onValidThread(@Nonnull Runnable runnable) {
    processor.foreground.runOrInvokeLater(runnable);
  }

  @Nonnull
  private <T> java.util.function.Consumer<T> onValidThread(@Nonnull Consumer<? super T> consumer) {
    return value -> onValidThread(() -> consumer.consume(value));
  }

  @Nonnull
  private Promise<Node> promiseRootEntry() {
    if (disposed) return rejectedPromise();
    return tree.queue.promise(processor, () -> new CmdGetRoot("Load root", null));
  }

  @Nonnull
  private Promise<Node> promiseChildren(@Nonnull Node node) {
    if (disposed) return rejectedPromise();
    return node.queue.promise(processor, () -> {
      node.setLoading(!showLoadingNode ? null : new Node(new LoadingNode(), true));
      return new CmdGetChildren("Load children", node, false);
    });
  }

  private Node getEntry(Object object) {
    return disposed || object == null || !isValidThread() ? null : tree.map.get(object);
  }

  @Nonnull
  private List<Node> getEntryChildren(Object object) {
    Node node = getEntry(object);
    if (node == null) return emptyList();
    if (node.isLoadingRequired()) promiseChildren(node);
    return node.getChildren();
  }

  @Nonnull
  private TreeModelEvent createEvent(@Nonnull TreePath path, Map<Object, Integer> map) {
    if (map == null || map.isEmpty()) return new TreeModelEvent(this, path, null, null);
    int i = 0;
    int size = map.size();
    int[] indices = new int[size];
    Object[] children = new Object[size];
    for (Entry<Object, Integer> entry : map.entrySet()) {
      indices[i] = entry.getValue();
      children[i] = entry.getKey();
      i++;
    }
    return new TreeModelEvent(this, path, indices, children);
  }

  private void treeNodesChanged(@Nonnull Node node, Map<Object, Integer> map) {
    if (!listeners.isEmpty()) {
      for (TreePath path : node.paths) {
        listeners.treeNodesChanged(createEvent(path, map));
      }
    }
  }

  private void treeNodesInserted(@Nonnull Node node, Map<Object, Integer> map) {
    if (!listeners.isEmpty()) {
      for (TreePath path : node.paths) {
        listeners.treeNodesInserted(createEvent(path, map));
      }
    }
  }

  private void treeNodesRemoved(@Nonnull Node node, Map<Object, Integer> map) {
    if (!listeners.isEmpty()) {
      for (TreePath path : node.paths) {
        listeners.treeNodesRemoved(createEvent(path, map));
      }
    }
  }

  @Nonnull
  private static LinkedHashMap<Object, Integer> getIndices(@Nonnull List<Node> children, @Nullable ToIntFunction<? super Node> function) {
    LinkedHashMap<Object, Integer> map = new LinkedHashMap<>();
    for (int i = 0; i < children.size(); i++) {
      Node child = children.get(i);
      if (map.containsKey(child.object)) {
        LOG.warn("ignore duplicated " + (function == null ? "old" : "new") + " child at " + i);
      }
      else {
        map.put(child.object, function == null ? i : function.applyAsInt(child));
      }
    }
    return map;
  }

  private static int getIntersectionCount(@Nonnull Map<Object, Integer> indices, @Nonnull Iterable<Object> objects) {
    int count = 0;
    int last = -1;
    for (Object object : objects) {
      Integer index = indices.get(object);
      if (index != null && last < index.intValue()) {
        last = index;
        count++;
      }
    }
    return count;
  }

  @Nonnull
  private static List<Object> getIntersection(@Nonnull Map<Object, Integer> indices, @Nonnull Iterable<Object> objects) {
    List<Object> list = new ArrayList<>(indices.size());
    int last = -1;
    for (Object object : objects) {
      Integer index = indices.get(object);
      if (index != null && last < index.intValue()) {
        last = index;
        list.add(object);
      }
    }
    return list;
  }

  @Nonnull
  private static List<Object> getIntersection(@Nonnull Map<Object, Integer> removed, @Nonnull Map<Object, Integer> inserted) {
    if (removed.isEmpty() || inserted.isEmpty()) return emptyList();
    int countOne = getIntersectionCount(removed, inserted.keySet());
    int countTwo = getIntersectionCount(inserted, removed.keySet());
    if (countOne > countTwo) return getIntersection(removed, inserted.keySet());
    if (countTwo > 0) return getIntersection(inserted, removed.keySet());
    return emptyList();
  }

  private abstract static class ObsolescentCommand implements Obsolescent, Command<Node> {
    final AsyncPromise<Node> promise = new AsyncPromise<>();
    final String name;
    final Object object;
    volatile boolean started;

    ObsolescentCommand(@Nonnull String name, Object object) {
      this.name = name;
      this.object = object;
      LOG.debug("create command: ", this);
    }

    abstract Node getNode(Object object);

    abstract void setNode(Node node);

    boolean isPending() {
      return Promise.State.PENDING == promise.getState();
    }

    @Override
    public String toString() {
      return object == null ? name : name + ": " + object;
    }

    @Override
    public Node get() {
      started = true;
      if (isObsolete()) {
        LOG.debug("obsolete command: ", this);
        return null;
      }
      else {
        LOG.debug("background command: ", this);
        return getNode(object);
      }
    }

    @Override
    public void accept(Node node) {
      if (isObsolete()) {
        LOG.debug("obsolete command: ", this);
      }
      else {
        LOG.debug("foreground command: ", this);
        setNode(node);
      }
    }
  }

  private final class CmdGetRoot extends ObsolescentCommand {
    private CmdGetRoot(@Nonnull String name, Object object) {
      super(name, object);
      tree.queue.add(this, old -> old.started || old.object != object);
    }

    @Override
    public boolean isObsolete() {
      return disposed || this != tree.queue.get();
    }

    @Override
    Node getNode(Object object) {
      if (object == null) object = model.getRoot();
      if (object == null || isObsolete()) return null;
      return new Node(object, model.isLeaf(object));
    }

    @Override
    void setNode(Node loaded) {
      Node root = tree.root;
      if (root == null && loaded == null) {
        LOG.debug("no root");
        tree.queue.done(this, null);
        return;
      }

      if (root != null && loaded != null && root.object.equals(loaded.object)) {
        tree.fixEqualButNotSame(root, loaded.object);
        LOG.debug("same root: ", root.object);
        if (!root.isLoadingRequired()) processor.process(new CmdGetChildren("Update root children", root, true));
        tree.queue.done(this, root);
        return;
      }

      if (root != null) {
        root.removeMapping(null, tree);
      }
      if (!tree.map.isEmpty()) {
        tree.map.values().forEach(node -> {
          node.queue.close();
          LOG.warn("remove staled node: " + node.object);
        });
        tree.map.clear();
      }

      tree.root = loaded;
      if (loaded != null) {
        tree.map.put(loaded.object, loaded);
        TreePath path = new TreePath(loaded.object);
        loaded.insertPath(path);
        treeStructureChanged(path, null, null);
        LOG.debug("new root: ", loaded.object);
        tree.queue.done(this, loaded);
      }
      else {
        treeStructureChanged(null, null, null);
        LOG.debug("root removed");
        tree.queue.done(this, null);
      }
    }
  }

  private final class CmdGetChildren extends ObsolescentCommand {
    private final Node node;
    private volatile boolean deep;

    CmdGetChildren(@Nonnull String name, @Nonnull Node node, boolean deep) {
      super(name, node.object);
      this.node = node;
      if (deep) this.deep = true;
      node.queue.add(this, old -> {
        if (!deep && old.deep && old.isPending()) this.deep = true;
        return true;
      });
    }

    @Override
    public boolean isObsolete() {
      return disposed || this != node.queue.get();
    }

    @Override
    Node getNode(Object object) {
      Node loaded = new Node(object, model.isLeaf(object));
      if (loaded.leaf || isObsolete()) return loaded;

      if (model instanceof ChildrenProvider) {
        //noinspection unchecked
        ChildrenProvider<Object> provider = (ChildrenProvider)model;
        List<?> children = provider.getChildren(object);
        if (children == null) throw new ProcessCanceledException(); // cancel this command
        loaded.children = load(children.size(), index -> children.get(index));
      }
      else {
        loaded.children = load(model.getChildCount(object), index -> model.getChild(object, index));
      }
      return loaded;
    }

    @Nullable
    private List<Node> load(int count, @Nonnull IntFunction function) {
      if (count < 0) LOG.warn("illegal child count: " + count);
      if (count <= 0) return emptyList();

      SmartHashSet<Object> set = new SmartHashSet<>(count);
      List<Node> children = new ArrayList<>(count);
      for (int i = 0; i < count; i++) {
        if (isObsolete()) return null;
        Object child = function.apply(i);
        if (child == null) {
          LOG.warn("ignore null child at " + i);
        }
        else if (!set.add(child)) {
          LOG.warn("ignore duplicated child at " + i + ": " + child);
        }
        else {
          if (isObsolete()) return null;
          children.add(new Node(child, model.isLeaf(child)));
        }
      }
      return children;
    }

    @Override
    void setNode(Node loaded) {
      if (loaded == null || loaded.isLoadingRequired()) {
        LOG.debug("cancelled command: ", this);
        return;
      }
      if (node != tree.map.get(loaded.object)) {
        node.queue.close();
        LOG.warn("ignore removed node: " + node.object);
        return;
      }
      List<Node> oldChildren = node.getChildren();
      List<Node> newChildren = loaded.getChildren();
      if (oldChildren.isEmpty() && newChildren.isEmpty()) {
        node.setLeaf(loaded.leaf);
        treeNodesChanged(node, null);
        LOG.debug("no children: ", node.object);
        node.queue.done(this, node);
        return;
      }

      LinkedHashMap<Object, Integer> removed = getIndices(oldChildren, null);
      if (newChildren.isEmpty()) {
        oldChildren.forEach(child -> child.removeMapping(node, tree));
        node.setLeaf(loaded.leaf);
        treeNodesRemoved(node, removed);
        LOG.debug("children removed: ", node.object);
        node.queue.done(this, node);
        return;
      }

      // remove duplicated nodes during indices calculation
      ArrayList<Node> list = new ArrayList<>(newChildren.size());
      SmartHashSet<Object> reload = new SmartHashSet<>();
      LinkedHashMap<Object, Integer> inserted = getIndices(newChildren, child -> {
        Node found = tree.map.get(child.object);
        if (found == null) {
          tree.map.put(child.object, child);
          list.add(child);
        }
        else {
          tree.fixEqualButNotSame(found, child.object);
          list.add(found);
          if (found.leaf) {
            if (!child.leaf) {
              found.setLeaf(false); // mark existing leaf node as not a leaf
              reload.add(found.object); // and request to load its children
            }
          }
          else if (child.leaf || !found.isLoadingRequired() && (deep || !removed.containsKey(found.object))) {
            reload.add(found.object); // request to load children of existing node
          }
        }
        return list.size() - 1;
      });
      newChildren = list;

      if (oldChildren.isEmpty()) {
        newChildren.forEach(child -> child.insertMapping(node));
        node.setChildren(newChildren);
        treeNodesInserted(node, inserted);
        LOG.debug("children inserted: ", node.object);
        node.queue.done(this, node);
        return;
      }

      LinkedHashMap<Object, Integer> contained = new LinkedHashMap<>();
      for (Object object : getIntersection(removed, inserted)) {
        Integer oldIndex = removed.remove(object);
        if (oldIndex == null) {
          LOG.warn("intersection failed");
        }
        Integer newIndex = inserted.remove(object);
        if (newIndex == null) {
          LOG.warn("intersection failed");
        }
        else {
          contained.put(object, newIndex);
        }
      }

      for (Node child : newChildren) {
        if (!removed.containsKey(child.object) && inserted.containsKey(child.object)) {
          child.insertMapping(node);
        }
      }

      for (Node child : oldChildren) {
        if (removed.containsKey(child.object) && !inserted.containsKey(child.object)) {
          child.removeMapping(node, tree);
        }
      }

      node.setChildren(newChildren);
      if (!removed.isEmpty()) treeNodesRemoved(node, removed);
      if (!inserted.isEmpty()) treeNodesInserted(node, inserted);
      if (!contained.isEmpty()) treeNodesChanged(node, contained);
      if (removed.isEmpty() && inserted.isEmpty()) treeNodesChanged(node, null);
      LOG.debug("children changed: ", node.object);

      if (!reload.isEmpty()) {
        for (Node child : newChildren) {
          if (reload.contains(child.object)) {
            processor.process(new CmdGetChildren("Update children recursively", child, true));
          }
        }
      }
      node.queue.done(this, node);
    }
  }

  private static final class CommandQueue<T extends ObsolescentCommand> {
    private final Deque<T> deque = new ArrayDeque<>();
    private volatile boolean closed;

    T get() {
      synchronized (deque) {
        return deque.peekFirst();
      }
    }

    @Nonnull
    Promise<Node> promise(@Nonnull Command.Processor processor, @Nonnull Supplier<? extends T> supplier) {
      T command;
      synchronized (deque) {
        command = deque.peekFirst();
        if (command != null) return command.promise;
        command = supplier.get();
      }
      processor.process(command);
      return command.promise;
    }

    void add(@Nonnull T command, @Nonnull Predicate<? super T> predicate) {
      synchronized (deque) {
        if (closed) return;
        T old = deque.peekFirst();
        boolean add = old == null || predicate.test(old);
        if (add) deque.addFirst(command);
      }
    }

    void done(@Nonnull T command, Node node) {
      Iterable<AsyncPromise<Node>> promises;
      synchronized (deque) {
        if (closed) return;
        if (!deque.contains(command)) return;
        promises = getPromises(command);
        if (deque.isEmpty()) deque.addLast(command);
      }
      promises.forEach(promise -> promise.setResult(node));
    }

    void close() {
      Iterable<AsyncPromise<Node>> promises;
      synchronized (deque) {
        if (closed) return;
        closed = true;
        if (deque.isEmpty()) return;
        promises = getPromises(null);
      }
      promises.forEach(promise -> promise.setError("cancel loading"));
    }

    @Nonnull
    private Iterable<AsyncPromise<Node>> getPromises(T command) {
      ArrayList<AsyncPromise<Node>> list = new ArrayList<>();
      while (true) {
        T last = deque.pollLast();
        if (last == null) break;
        if (last.isPending()) list.add(last.promise);
        if (last.equals(command)) break;
      }
      return list;
    }
  }

  private static final class Tree {
    private final CommandQueue<CmdGetRoot> queue = new CommandQueue<>();
    private final Map<Object, Node> map = new HashMap<>();
    private volatile Node root;

    private void removeEmpty(@Nonnull Node child) {
      child.forEachChildExceptLoading(this::removeEmpty);
      if (child.paths.isEmpty()) {
        child.queue.close();
        Node node = map.remove(child.object);
        if (node != child) {
          LOG.warn("invalid node: " + child.object);
          if (node != null) map.put(node.object, node);
        }
      }
    }

    private void fixEqualButNotSame(@Nonnull Node node, @Nonnull Object object) {
      if (object == node.object) return;
      // always use new instance of user's object, because
      // some trees provide equal nodes with different behavior
      map.remove(node.object);
      node.updatePaths(node.object, object);
      node.object = object;
      map.put(object, node); // update key
    }
  }

  private static final class Node {
    private final CommandQueue<CmdGetChildren> queue = new CommandQueue<>();
    private final Set<TreePath> paths = new SmartHashSet<>();
    private volatile Object object;
    private volatile boolean leaf;
    @Nullable
    private volatile List<Node> children;
    private volatile Node loading;

    private Node(@Nonnull Object object, boolean leaf) {
      this.object = object;
      this.leaf = leaf;
    }

    private void setLeaf(boolean leaf) {
      this.leaf = leaf;
      this.children = leaf ? null : emptyList();
      this.loading = null;
    }

    private void setChildren(@Nonnull List<Node> children) {
      this.leaf = false;
      this.children = children;
      this.loading = null;
    }

    private void setLoading(Node loading) {
      this.leaf = false;
      this.children = loading != null ? singletonList(loading) : emptyList();
      this.loading = loading;
    }

    private boolean isLoadingRequired() {
      return !leaf && children == null;
    }

    @Nonnull
    private List<Node> getChildren() {
      List<Node> list = children;
      return list != null ? list : emptyList();
    }

    private void forEachChildExceptLoading(Consumer<? super Node> consumer) {
      for (Node node : getChildren()) {
        if (node != loading) consumer.consume(node);
      }
    }

    private void insertPath(@Nonnull TreePath path) {
      if (!paths.add(path)) {
        LOG.warn("node is already attached to " + path);
      }
      forEachChildExceptLoading(child -> child.insertPath(path.pathByAddingChild(child.object)));
    }

    private void insertMapping(Node parent) {
      if (parent == null) {
        insertPath(new TreePath(object));
      }
      else if (parent.loading == this) {
        LOG.warn("insert loading node unexpectedly");
      }
      else if (parent.paths.isEmpty()) {
        LOG.warn("insert to invalid parent");
      }
      else {
        parent.paths.forEach(path -> insertPath(path.pathByAddingChild(object)));
      }
    }

    private void removePath(@Nonnull TreePath path) {
      if (!paths.remove(path)) {
        LOG.warn("node is not attached to " + path);
      }
      forEachChildExceptLoading(child -> child.removePath(path.pathByAddingChild(child.object)));
    }

    private void removeMapping(Node parent, @Nonnull Tree tree) {
      if (parent == null) {
        removePath(new TreePath(object));
        tree.removeEmpty(this);
      }
      else if (parent.loading == this) {
        parent.loading = null;
      }
      else if (parent.paths.isEmpty()) {
        LOG.warn("remove from invalid parent");
      }
      else {
        parent.paths.forEach(path -> removePath(path.pathByAddingChild(object)));
        tree.removeEmpty(this);
      }
    }

    private void updatePaths(@Nonnull Object oldObject, @Nonnull Object newObject) {
      if (paths.stream().anyMatch(path -> contains(path, oldObject))) {
        // replace instance of user's object in all internal maps to avoid memory leaks
        List<TreePath> updated = ContainerUtil.map(paths, path -> update(path, oldObject, newObject));
        paths.clear();
        paths.addAll(updated);
        forEachChildExceptLoading(child -> child.updatePaths(oldObject, newObject));
      }
    }

    @Nonnull
    private static TreePath update(@Nonnull TreePath path, @Nonnull Object oldObject, @Nonnull Object newObject) {
      if (!contains(path, oldObject)) return path;
      LOG.debug("update path: ", path);
      Object[] objects = TreePathUtil.convertTreePathToArray(path);
      for (int i = 0; i < objects.length; i++) {
        if (oldObject == objects[i]) objects[i] = newObject;
      }
      return TreePathUtil.convertArrayToTreePath(objects);
    }

    private static boolean contains(@Nonnull TreePath path, @Nonnull Object object) {
      while (object != path.getLastPathComponent()) {
        path = path.getParentPath();
        if (path == null) return false;
      }
      return true;
    }
  }

  /**
   * @deprecated do not use
   */
  @Deprecated
  public void setRootImmediately(@Nonnull Object object) {
    Node node = new Node(object, false);
    node.insertPath(new TreePath(object));
    tree.root = node;
    tree.map.put(object, node);
  }
}
