/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * User: anna
 * Date: 25-May-2007
 */
package consulo.execution.test.ui;

import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposer;
import consulo.execution.action.Location;
import consulo.execution.test.*;
import consulo.language.editor.QualifiedNameProviderUtil;
import consulo.language.psi.PsiElement;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.ExpandableItemsHandler;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.awt.EditSourceOnDoubleClickHandler;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class TestTreeView extends Tree implements DataProvider, CopyProvider {
  public static final Key<TestFrameworkRunningModel> MODEL_DATA_KEY = Key.create("testFrameworkModel.dataId");

  private TestFrameworkRunningModel myModel;

  protected abstract TreeCellRenderer getRenderer(TestConsoleProperties properties);

  public abstract AbstractTestProxy getSelectedTest(@Nonnull TreePath selectionPath);

  protected TestFrameworkRunningModel getTestFrameworkRunningModel() {
    return myModel;
  }

  @Nullable
  public AbstractTestProxy getSelectedTest() {
    TreePath[] paths = getSelectionPaths();
    if (paths != null && paths.length > 1) return null;
    final TreePath selectionPath = getSelectionPath();
    return selectionPath != null ? getSelectedTest(selectionPath) : null;
  }

  public void attachToModel(final TestFrameworkRunningModel model) {
    setModel(new DefaultTreeModel(new DefaultMutableTreeNode(model.getRoot())));
    getSelectionModel().setSelectionMode(model.getProperties().getSelectionMode());
    myModel = model;
    Disposer.register(myModel, myModel.getRoot());
    Disposer.register(myModel, () -> {
      setModel(null);
      myModel = null;
    });
    installHandlers();
    setCellRenderer(getRenderer(myModel.getProperties()));
  }

  @Override
  public Object getData(@Nonnull final Key<?> dataId) {
    if (CopyProvider.KEY == dataId) {
      return this;
    }

    if (PsiElement.KEY_OF_ARRAY == dataId) {
      TreePath[] paths = getSelectionPaths();
      if (paths != null && paths.length > 1) {
        final List<PsiElement> els = new ArrayList<>(paths.length);
        for (TreePath path : paths) {
          if (isPathSelected(path.getParentPath())) continue;
          AbstractTestProxy test = getSelectedTest(path);
          if (test != null) {
            final PsiElement psiElement = TestsUIUtil.getData(test, PsiElement.KEY, myModel);
            if (psiElement != null) {
              els.add(psiElement);
            }
          }
        }
        return els.isEmpty() ? null : els.toArray(new PsiElement[els.size()]);
      }
    }

    if (Location.DATA_KEYS == dataId) {
      TreePath[] paths = getSelectionPaths();
      if (paths != null && paths.length > 1) {
        final List<Location<?>> locations = new ArrayList<>(paths.length);
        for (TreePath path : paths) {
          if (isPathSelected(path.getParentPath())) continue;
          AbstractTestProxy test = getSelectedTest(path);
          if (test != null) {
            final Location<?> location = TestsUIUtil.getData(test, Location.DATA_KEY, myModel);
            if (location != null) {
              locations.add(location);
            }
          }
        }
        return locations.isEmpty() ? null : locations.toArray(new Location[locations.size()]);
      }
    }

    if (MODEL_DATA_KEY == dataId) {
      return myModel;
    }

    final TreePath selectionPath = getSelectionPath();
    if (selectionPath == null) return null;
    final AbstractTestProxy testProxy = getSelectedTest(selectionPath);
    if (testProxy == null) return null;
    return TestsUIUtil.getData(testProxy, dataId, myModel);
  }

  @Override
  public void performCopy(@Nonnull DataContext dataContext) {
    final PsiElement element = dataContext.getData(PsiElement.KEY);
    final String fqn;
    if (element != null) {
      fqn = QualifiedNameProviderUtil.elementToFqn(element, null);
    }
    else {
      AbstractTestProxy selectedTest = getSelectedTest();
      fqn = selectedTest instanceof TestProxyRoot ? ((TestProxyRoot)selectedTest).getRootLocation() : selectedTest != null ? selectedTest.getLocationUrl() : null;
    }
    CopyPasteManager.getInstance().setContents(new StringSelection(fqn));
  }

  @Override
  public boolean isCopyEnabled(@Nonnull DataContext dataContext) {
    AbstractTestProxy test = getSelectedTest();
    if (test instanceof TestProxyRoot) {
      return ((TestProxyRoot)test).getRootLocation() != null;
    }
    return test != null && test.getLocationUrl() != null;
  }

  @Override
  public boolean isCopyVisible(@Nonnull DataContext dataContext) {
    return true;
  }

  protected void installHandlers() {                                                
    EditSourceOnDoubleClickHandler.install(this);
    new TreeSpeedSearch(this, path -> {
      final AbstractTestProxy testProxy = getSelectedTest(path);
      if (testProxy == null) return null;
      return testProxy.getName();
    });
    TreeUtil.installActions(this);
    PopupHandler.installPopupHandler(this, IdeActions.GROUP_TESTTREE_POPUP, ActionPlaces.TESTTREE_VIEW_POPUP);
  }

  public boolean isExpandableHandlerVisibleForCurrentRow(int row) {
    final ExpandableItemsHandler<Integer> handler = getExpandableItemsHandler();
    final Collection<Integer> items = handler.getExpandedItems();
    return items.size() == 1 && row == items.iterator().next();
  }
}