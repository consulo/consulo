// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.dataContext.DataManager;
import consulo.ide.impl.idea.ide.PsiCopyPasteManager;
import consulo.ide.impl.idea.ide.projectView.BaseProjectTreeBuilder;
import consulo.project.ui.view.tree.PsiDirectoryNode;
import consulo.ide.impl.idea.ide.ui.customization.CustomizationUtil;
import consulo.ui.ex.action.ActionPlaces;
import consulo.dataContext.DataContext;
import consulo.ui.ex.action.IdeActions;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.TextAttributes;
import consulo.project.Project;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.awt.tree.*;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.application.util.registry.Registry;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ide.impl.idea.ui.stripe.ErrorStripe;
import consulo.ide.impl.idea.ui.stripe.ErrorStripePainter;
import consulo.ide.impl.idea.ui.stripe.TreeUpdater;
import consulo.ui.ex.awt.EditSourceOnDoubleClickHandler;
import consulo.ui.ex.OpenSourceUtil;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.update.Activatable;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.disposer.Disposer;
import consulo.ui.color.ColorValue;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.StringTokenizer;

public abstract class AbstractProjectViewPSIPane extends AbstractProjectViewPane {
  private AsyncProjectViewSupport myAsyncSupport;
  private JScrollPane myComponent;

  protected AbstractProjectViewPSIPane(@Nonnull Project project) {
    super(project);
  }

  @Nonnull
  @Override
  public JComponent createComponent() {
    if (myComponent != null) {
      if (myTree != null) {
        myTree.updateUI();
      }
      return myComponent;
    }

    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(null);
    DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
    myTree = createTree(treeModel);
    enableDnD();
    myComponent = ScrollPaneFactory.createScrollPane(myTree);
    if (Registry.is("error.stripe.enabled")) {
      ErrorStripePainter painter = new ErrorStripePainter(true);
      Disposer.register(this, new TreeUpdater<ErrorStripePainter>(painter, myComponent, myTree) {
        @Override
        protected void update(ErrorStripePainter painter, int index, Object object) {
          if (object instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)object;
            object = node.getUserObject();
          }
          super.update(painter, index, getStripe(object, myTree.isExpanded(index)));
        }
      });
    }
    myTreeStructure = createStructure();

    BaseProjectTreeBuilder treeBuilder = createBuilder(treeModel);
    if (treeBuilder != null) {
      installComparator(treeBuilder);
      setTreeBuilder(treeBuilder);
    }
    else {
      myAsyncSupport = new AsyncProjectViewSupport(this, myProject, myTree, myTreeStructure, createComparator());
    }

    initTree();

    Disposer.register(this, new UiNotifyConnector(myTree, new Activatable() {
      private boolean showing;

      @Override
      public void showNotify() {
        if (!showing) {
          showing = true;
          restoreExpandedPaths();
        }
      }

      @Override
      public void hideNotify() {
        if (showing) {
          showing = false;
          saveExpandedPaths();
        }
      }
    }));
    return myComponent;
  }

  @Override
  protected void installComparator(AbstractTreeBuilder builder, @Nonnull Comparator<? super NodeDescriptor> comparator) {
    if (myAsyncSupport != null) myAsyncSupport.setComparator(comparator);
    super.installComparator(builder, comparator);
  }

  @Override
  public final void dispose() {
    myAsyncSupport = null;
    myComponent = null;
    super.dispose();
  }

  private void initTree() {
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);
    UIUtil.setLineStyleAngled(myTree);
    myTree.setRootVisible(false);
    myTree.setShowsRootHandles(true);
    myTree.expandPath(new TreePath(myTree.getModel().getRoot()));

    EditSourceOnDoubleClickHandler.install(myTree);

    ToolTipManager.sharedInstance().registerComponent(myTree);
    TreeUtil.installActions(myTree);

    new MySpeedSearch(myTree);

    myTree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (KeyEvent.VK_ENTER == e.getKeyCode()) {
          TreePath path = getSelectedPath();
          if (path != null && !myTree.getModel().isLeaf(path.getLastPathComponent())) {
            return;
          }

          DataContext dataContext = DataManager.getInstance().getDataContext(myTree);
          OpenSourceUtil.openSourcesFrom(dataContext, ScreenReader.isActive());
        }
        else if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
          if (e.isConsumed()) return;
          PsiCopyPasteManager copyPasteManager = PsiCopyPasteManager.getInstance();
          boolean[] isCopied = new boolean[1];
          if (copyPasteManager.getElements(isCopied) != null && !isCopied[0]) {
            copyPasteManager.clear();
            e.consume();
          }
        }
      }
    });
    CustomizationUtil.installPopupHandler(myTree, IdeActions.GROUP_PROJECT_VIEW_POPUP, ActionPlaces.PROJECT_VIEW_POPUP);
  }

  @Nonnull
  @Override
  public final ActionCallback updateFromRoot(boolean restoreExpandedPaths) {
    Runnable afterUpdate;
    final ActionCallback cb = new ActionCallback();
    AbstractTreeBuilder builder = getTreeBuilder();
    if (restoreExpandedPaths && builder != null) {
      final ArrayList<Object> pathsToExpand = new ArrayList<>();
      final ArrayList<Object> selectionPaths = new ArrayList<>();
      TreeBuilderUtil.storePaths(builder, (DefaultMutableTreeNode)myTree.getModel().getRoot(), pathsToExpand, selectionPaths, true);
      afterUpdate = () -> {
        if (myTree != null && !builder.isDisposed()) {
          myTree.setSelectionPaths(new TreePath[0]);
          TreeBuilderUtil.restorePaths(builder, pathsToExpand, selectionPaths, true);
        }
        cb.setDone();
      };
    }
    else {
      afterUpdate = cb.createSetDoneRunnable();
    }
    if (builder != null) {
      builder.addSubtreeToUpdate(builder.getRootNode(), afterUpdate);
    }
    else if (myAsyncSupport != null) {
      myAsyncSupport.updateAll(afterUpdate);
    }
    return cb;
  }

  @Override
  public void select(Object element, VirtualFile file, boolean requestFocus) {
    selectCB(element, file, requestFocus);
  }

  @Override
  @Nonnull
  public AsyncResult<Void> selectCB(Object element, VirtualFile file, boolean requestFocus) {
    if (file != null) {
      AbstractTreeBuilder builder = getTreeBuilder();
      if (builder instanceof BaseProjectTreeBuilder) {
        beforeSelect().doWhenDone(() -> UIUtil.invokeLaterIfNeeded(() -> {
          if (!builder.isDisposed()) {
            ((BaseProjectTreeBuilder)builder).selectAsync(element, file, requestFocus);
          }
        }));
      }
      else if (myAsyncSupport != null) {
        myAsyncSupport.select(myTree, element, file);
      }
    }
    return AsyncResult.resolved();
  }

  @Nonnull
  public ActionCallback beforeSelect() {
    // actually, getInitialized().doWhenDone() should be called by builder internally
    // this will be done in 2017
    AbstractTreeBuilder builder = getTreeBuilder();
    if (builder == null) return AsyncResult.resolved();
    return builder.getInitialized();
  }

  protected BaseProjectTreeBuilder createBuilder(@Nonnull DefaultTreeModel treeModel) {
    return new ProjectTreeBuilder(myProject, myTree, treeModel, null, (ProjectAbstractTreeStructureBase)myTreeStructure) {
      @Override
      protected AbstractTreeUpdater createUpdater() {
        return createTreeUpdater(this);
      }
    };
  }

  @Nonnull
  public abstract ProjectAbstractTreeStructureBase createStructure();

  @Nonnull
  protected abstract ProjectViewTree createTree(@Nonnull DefaultTreeModel treeModel);

  @Nonnull
  protected abstract AbstractTreeUpdater createTreeUpdater(@Nonnull AbstractTreeBuilder treeBuilder);

  /**
   * @param object   an object that represents a node in the project tree
   * @param expanded {@code true} if the corresponding node is expanded,
   *                 {@code false} if it is collapsed
   * @return a non-null value if the corresponding node should be , or {@code null}
   */
  protected ErrorStripe getStripe(Object object, boolean expanded) {
    if (expanded && object instanceof PsiDirectoryNode) return null;
    if (object instanceof PresentableNodeDescriptor) {
      PresentableNodeDescriptor node = (PresentableNodeDescriptor)object;
      TextAttributesKey key = node.getPresentation().getTextAttributesKey();
      TextAttributes attributes = key == null ? null : EditorColorsManager.getInstance().getSchemeForCurrentUITheme().getAttributes(key);
      ColorValue color = attributes == null ? null : attributes.getErrorStripeColor();
      if (color != null) return ErrorStripe.create(color, 1);
    }
    return null;
  }

  protected static final class MySpeedSearch extends TreeSpeedSearch {
    MySpeedSearch(JTree tree) {
      super(tree);
    }

    @Override
    protected boolean isMatchingElement(Object element, String pattern) {
      Object userObject = ((DefaultMutableTreeNode)((TreePath)element).getLastPathComponent()).getUserObject();
      if (userObject instanceof PsiDirectoryNode) {
        String str = getElementText(element);
        if (str == null) return false;
        str = str.toLowerCase();
        if (pattern.indexOf('.') >= 0) {
          return compare(str, pattern);
        }
        StringTokenizer tokenizer = new StringTokenizer(str, ".");
        while (tokenizer.hasMoreTokens()) {
          String token = tokenizer.nextToken();
          if (compare(token, pattern)) {
            return true;
          }
        }
        return false;
      }
      else {
        return super.isMatchingElement(element, pattern);
      }
    }
  }

  @Override
  AsyncProjectViewSupport getAsyncSupport() {
    return myAsyncSupport;
  }
}
