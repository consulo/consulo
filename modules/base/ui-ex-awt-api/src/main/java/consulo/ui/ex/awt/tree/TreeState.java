// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ui.ex.awt.tree;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Progressive;
import consulo.logging.Logger;
import consulo.navigation.NavigationItem;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.tree.NodeDescriptorProvidingKey;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.JBIterable;
import consulo.util.collection.SmartList;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.Promise;
import consulo.util.concurrent.Promises;
import consulo.util.dataholder.Key;
import consulo.util.jdom.JDOMUtil;
import consulo.util.lang.*;
import consulo.util.lang.ref.SoftReference;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * @see #createOn(JTree)
 * @see #createOn(JTree, DefaultMutableTreeNode)
 * @see #applyTo(JTree)
 * @see #applyTo(JTree, Object)
 */
public class TreeState implements JDOMExternalizable {
  private static final Logger LOG = Logger.getInstance(TreeState.class);

  public static final Key<WeakReference<ActionCallback>> CALLBACK = Key.create("Callback");
  private static final Key<Promise<Void>> EXPANDING = Key.create("TreeExpanding");

  private static final String EXPAND_TAG = "expand";
  private static final String SELECT_TAG = "select";
  private static final String PATH_TAG = "path";

  private enum Match {
    OBJECT,
    ID_TYPE
  }

  @Tag("item")
  public static class PathElement {
    @Attribute("name")
    public String id;
    @Attribute("type")
    public String type;
    @Attribute("user")
    public String userStr;

    Object userObject;
    final int index;

    @SuppressWarnings("unused")
    public PathElement() {
      this(null, null, -1, null);
    }

    public PathElement(String itemId, String itemType, int itemIndex, Object userObject) {
      id = itemId;
      type = itemType;

      index = itemIndex;
      userStr = userObject instanceof String ? (String)userObject : null;
      this.userObject = userObject;
    }

    @Override
    public String toString() {
      return id + ": " + type;
    }

    private boolean isMatchTo(Object object) {
      return getMatchTo(object) != null;
    }

    private Match getMatchTo(Object object) {
      Object userObject = TreeUtil.getUserObject(object);
      if (this.userObject != null && this.userObject.equals(userObject)) return Match.OBJECT;
      return Comparing.equal(id, calcId(userObject)) && Comparing.equal(type, calcType(userObject)) ? Match.ID_TYPE : null;
    }
  }

  private final List<List<PathElement>> myExpandedPaths;
  private final List<List<PathElement>> mySelectedPaths;
  private boolean myScrollToSelection;

  // xml deserialization
  @SuppressWarnings("unused")
  TreeState() {
    this(new SmartList<>(), new SmartList<>());
  }

  private TreeState(List<List<PathElement>> expandedPaths, List<List<PathElement>> selectedPaths) {
    myExpandedPaths = expandedPaths;
    mySelectedPaths = selectedPaths;
    myScrollToSelection = true;
  }

  public boolean isEmpty() {
    return myExpandedPaths.isEmpty() && mySelectedPaths.isEmpty();
  }

  @Override
  public void readExternal(Element element) throws InvalidDataException {
    readExternal(element, myExpandedPaths, EXPAND_TAG);
    readExternal(element, mySelectedPaths, SELECT_TAG);
  }

  private static void readExternal(Element root, List<? super List<PathElement>> list, String name) throws InvalidDataException {
    list.clear();
    for (Element element : root.getChildren(name)) {
      for (Element child : element.getChildren(PATH_TAG)) {
        PathElement[] path = XmlSerializer.deserialize(child, PathElement[].class);
        list.add(List.of(path));
      }
    }
  }

  @Nonnull
  public static TreeState createOn(@Nonnull JTree tree, @Nonnull DefaultMutableTreeNode treeNode) {
    return createOn(tree, new TreePath(treeNode.getPath()));
  }

  @Nonnull
  public static TreeState createOn(@Nonnull JTree tree, @Nonnull TreePath rootPath) {
    return new TreeState(createPaths(tree, TreeUtil.collectExpandedPaths(tree, rootPath)), createPaths(tree, TreeUtil.collectSelectedPaths(tree, rootPath)));
  }

  @Nonnull
  public static TreeState createOn(@Nonnull JTree tree) {
    return new TreeState(createPaths(tree, TreeUtil.collectExpandedPaths(tree)), new ArrayList<>());
  }

  @Nonnull
  public static TreeState createFrom(@Nullable Element element) {
    TreeState state = new TreeState(new ArrayList<>(), new ArrayList<>());
    try {
      if (element != null) {
        state.readExternal(element);
      }
    }
    catch (InvalidDataException e) {
      LOG.warn(e);
    }
    return state;
  }

  @Override
  public void writeExternal(Element element) throws WriteExternalException {
    writeExternal(element, myExpandedPaths, EXPAND_TAG);
    writeExternal(element, mySelectedPaths, SELECT_TAG);
  }

  private static void writeExternal(Element element, List<? extends List<PathElement>> list, String name) throws WriteExternalException {
    Element root = new Element(name);
    for (List<PathElement> path : list) {
      Element e = XmlSerializer.serialize(path.toArray());
      e.setName(PATH_TAG);
      root.addContent(e);
    }
    element.addContent(root);
  }

  @Nonnull
  private static List<List<PathElement>> createPaths(@Nonnull JTree tree, @Nonnull List<? extends TreePath> paths) {
    return JBIterable.from(paths).filter(o -> o.getPathCount() > 1 || tree.isRootVisible()).map(o -> createPath(tree.getModel(), o)).toList();
  }

  @Nonnull
  private static List<PathElement> createPath(@Nonnull TreeModel model, @Nonnull TreePath treePath) {
    Object prev = null;
    int count = treePath.getPathCount();
    PathElement[] result = new PathElement[count];
    for (int i = 0; i < count; i++) {
      Object cur = treePath.getPathComponent(i);
      Object userObject = TreeUtil.getUserObject(cur);
      int childIndex = prev == null ? 0 : model.getIndexOfChild(prev, cur);
      result[i] = new PathElement(calcId(userObject), calcType(userObject), childIndex, userObject);
      prev = cur;
    }
    return Arrays.asList(result);
  }

  @Nonnull
  private static String calcId(@Nullable Object userObject) {
    if (userObject == null) return "";
    Object value = userObject instanceof NodeDescriptorProvidingKey
                   ? ((NodeDescriptorProvidingKey)userObject).getKey()
                   : userObject instanceof TreeNode ? ((TreeNode)userObject).getValue() : userObject;
    if (value instanceof NavigationItem) {
      try {
        String name = ((NavigationItem)value).getName();
        return name != null ? name : StringUtil.notNullize(value.toString());
      }
      catch (Exception ignored) {
      }
    }
    return StringUtil.notNullize(userObject.toString());
  }

  @Nonnull
  private static String calcType(@Nullable Object userObject) {
    if (userObject == null) return "";
    String name = userObject.getClass().getName();
    return Integer.toHexString(StringHash.murmur(name, 31)) + ":" + StringUtil.getShortName(name);
  }

  public void applyTo(@Nonnull JTree tree) {
    applyTo(tree, tree.getModel().getRoot());
  }

  public void applyTo(@Nonnull JTree tree, @Nullable Object root) {
    LOG.debug(new IllegalStateException("restore paths"));
    if (visit(tree)) return; // AsyncTreeModel#accept
    if (root == null) return;
    TreeFacade facade = TreeFacade.getFacade(tree);
    ActionCallback callback = facade.getInitialized().doWhenDone(new TreeRunnable("TreeState.applyTo: on done facade init") {
      @Override
      public void perform() {
        facade.batch(indicator -> applyExpandedTo(facade, new TreePath(root), indicator));
      }
    });
    if (tree.getSelectionCount() == 0) {
      callback.doWhenDone(new TreeRunnable("TreeState.applyTo: on done") {
        @Override
        public void perform() {
          if (tree.getSelectionCount() == 0) {
            applySelectedTo(tree);
          }
        }
      });
    }
  }

  private void applyExpandedTo(@Nonnull TreeFacade tree, @Nonnull TreePath rootPath, @Nonnull ProgressIndicator indicator) {
    indicator.checkCanceled();
    if (rootPath.getPathCount() <= 0) return;

    for (List<PathElement> path : myExpandedPaths) {
      if (path.isEmpty()) continue;
      int index = rootPath.getPathCount() - 1;
      if (!path.get(index).isMatchTo(rootPath.getPathComponent(index))) continue;
      expandImpl(0, path, rootPath, tree, indicator);
    }
  }

  private void applySelectedTo(@Nonnull JTree tree) {
    List<TreePath> selection = new ArrayList<>();
    for (List<PathElement> path : mySelectedPaths) {
      TreeModel model = tree.getModel();
      TreePath treePath = new TreePath(model.getRoot());
      for (int i = 1; treePath != null && i < path.size(); i++) {
        treePath = findMatchedChild(model, treePath, path.get(i));
      }
      ContainerUtil.addIfNotNull(selection, treePath);
    }
    if (selection.isEmpty()) return;
    tree.setSelectionPaths(selection.toArray(TreeUtil.EMPTY_TREE_PATH));
    if (myScrollToSelection) {
      TreeUtil.showRowCentered(tree, tree.getRowForPath(selection.get(0)), true, true);
    }
  }


  @Nullable
  private static TreePath findMatchedChild(@Nonnull TreeModel model, @Nonnull TreePath treePath, @Nonnull PathElement pathElement) {
    Object parent = treePath.getLastPathComponent();
    int childCount = model.getChildCount(parent);
    if (childCount <= 0) return null;

    boolean idMatchedFound = false;
    Object idMatchedChild = null;
    for (int j = 0; j < childCount; j++) {
      Object child = model.getChild(parent, j);
      Match match = pathElement.getMatchTo(child);
      if (match == Match.OBJECT) {
        return treePath.pathByAddingChild(child);
      }
      if (match == Match.ID_TYPE && !idMatchedFound) {
        idMatchedChild = child;
        idMatchedFound = true;
      }
    }
    if (idMatchedFound) {
      return treePath.pathByAddingChild(idMatchedChild);
    }

    int index = Math.max(0, Math.min(pathElement.index, childCount - 1));
    Object child = model.getChild(parent, index);
    return treePath.pathByAddingChild(child);
  }

  private static void expandImpl(int positionInPath, List<? extends PathElement> path, TreePath treePath, TreeFacade tree, ProgressIndicator indicator) {
    tree.expand(treePath).doWhenDone(new TreeRunnable("TreeState.applyTo") {
      @Override
      public void perform() {
        indicator.checkCanceled();

        PathElement next = positionInPath == path.size() - 1 ? null : path.get(positionInPath + 1);
        if (next == null) return;

        Object parent = treePath.getLastPathComponent();
        TreeModel model = tree.tree.getModel();
        int childCount = model.getChildCount(parent);
        for (int j = 0; j < childCount; j++) {
          Object child = tree.tree.getModel().getChild(parent, j);
          if (next.isMatchTo(child)) {
            expandImpl(positionInPath + 1, path, treePath.pathByAddingChild(child), tree, indicator);
            break;
          }
        }
      }
    });
  }

  abstract static class TreeFacade {

    final JTree tree;

    TreeFacade(@Nonnull JTree tree) {
      this.tree = tree;
    }

    abstract ActionCallback getInitialized();

    abstract ActionCallback expand(TreePath treePath);

    abstract void batch(Progressive progressive);

    static TreeFacade getFacade(JTree tree) {
      AbstractTreeBuilder builder = AbstractTreeBuilder.getBuilderFor(tree);
      return builder != null ? new BuilderFacade(builder) : new JTreeFacade(tree);
    }
  }

  static class JTreeFacade extends TreeFacade {

    JTreeFacade(JTree tree) {
      super(tree);
    }

    @Override
    public ActionCallback expand(@Nonnull TreePath treePath) {
      tree.expandPath(treePath);
      return ActionCallback.DONE;
    }

    @Override
    public ActionCallback getInitialized() {
      WeakReference<ActionCallback> ref = UIUtil.getClientProperty(tree, CALLBACK);
      ActionCallback callback = SoftReference.dereference(ref);
      if (callback != null) return callback;
      return ActionCallback.DONE;
    }

    @Override
    public void batch(Progressive progressive) {
      progressive.run(new EmptyProgressIndicator());
    }
  }

  static class BuilderFacade extends TreeFacade {

    private final AbstractTreeBuilder myBuilder;

    BuilderFacade(AbstractTreeBuilder builder) {
      super(ObjectUtil.notNull(builder.getTree()));
      myBuilder = builder;
    }

    @Override
    public ActionCallback getInitialized() {
      return myBuilder.getReady(this);
    }

    @Override
    public void batch(Progressive progressive) {
      myBuilder.batch(progressive);
    }

    @Override
    public ActionCallback expand(TreePath treePath) {
      NodeDescriptor desc = TreeUtil.getLastUserObject(NodeDescriptor.class, treePath);
      if (desc == null) return ActionCallback.REJECTED;
      Object element = myBuilder.getTreeStructureElement(desc);
      ActionCallback result = new ActionCallback();
      myBuilder.expand(element, result.createSetDoneRunnable());

      return result;
    }
  }

  public void setScrollToSelection(boolean scrollToSelection) {
    myScrollToSelection = scrollToSelection;
  }

  @Override
  public String toString() {
    Element st = new Element("TreeState");
    String content;
    try {
      writeExternal(st);
      content = JDOMUtil.writeChildren(st, "\n");
    }
    catch (IOException e) {
      content = ExceptionUtil.getThrowableText(e);
    }
    return "TreeState(" + myScrollToSelection + ")\n" + content;
  }

  /**
   * @deprecated Temporary solution to resolve simultaneous expansions with async tree model.
   * Note that the specified consumer must resolve async promise at the end.
   */
  @Deprecated
  public static void expand(@Nonnull JTree tree, @Nonnull Consumer<? super AsyncPromise<Void>> consumer) {
    Promise<Void> expanding = UIUtil.getClientProperty(tree, EXPANDING);
    LOG.debug("EXPANDING: ", expanding);
    if (expanding == null) expanding = Promises.resolvedPromise();
    expanding.onProcessed(value -> {
      AsyncPromise<Void> promise = new AsyncPromise<>();
      UIUtil.putClientProperty(tree, EXPANDING, promise);
      consumer.accept(promise);
    });
  }

  private static boolean isSelectionNeeded(List<TreePath> list, @Nonnull JTree tree, AsyncPromise<Void> promise) {
    if (list != null && tree.isSelectionEmpty()) return true;
    if (promise != null) promise.setResult(null);
    return false;
  }

  private Promise<List<TreePath>> expand(@Nonnull JTree tree) {
    return TreeUtil.promiseExpand(tree, myExpandedPaths.stream().map(elements -> new Visitor(elements)));
  }

  private Promise<List<TreePath>> select(@Nonnull JTree tree) {
    return TreeUtil.promiseSelect(tree, mySelectedPaths.stream().map(elements -> new Visitor(elements)));
  }

  private boolean visit(@Nonnull JTree tree) {
    TreeModel model = tree.getModel();
    if (!(model instanceof TreeVisitor.Acceptor)) return false;

    expand(tree, promise -> expand(tree).onProcessed(expanded -> {
      if (isSelectionNeeded(expanded, tree, promise)) {
        select(tree).onProcessed(selected -> promise.setResult(null));
      }
    }));
    return true;
  }

  private static final class Visitor implements TreeVisitor {
    private final List<PathElement> elements;

    Visitor(List<PathElement> elements) {
      this.elements = elements;
    }

    @Nonnull
    @Override
    public Action visit(@Nonnull TreePath path) {
      int count = path.getPathCount();
      if (count > elements.size()) return Action.SKIP_CHILDREN;
      boolean matches = elements.get(count - 1).isMatchTo(path.getLastPathComponent());
      return !matches ? Action.SKIP_CHILDREN : count < elements.size() ? Action.CONTINUE : Action.INTERRUPT;
    }
  }
}

