/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.util;

import consulo.application.ApplicationManager;
import consulo.application.impl.internal.IdeaModalityState;
import consulo.application.progress.ProgressIndicator;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.function.Processor;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.projectView.BaseProjectTreeBuilder;
import consulo.ide.impl.idea.ide.projectView.impl.AbstractProjectTreeStructure;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectAbstractTreeStructureBase;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectTreeBuilder;
import consulo.ide.impl.idea.ide.util.gotoByName.*;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.ui.TreeChooser;
import consulo.language.editor.ui.TreeClassInheritorsProvider;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.search.FindSymbolParameters;
import consulo.language.psi.util.SymbolPresentationUtil;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;
import consulo.project.content.scope.ProjectScopes;
import consulo.ui.ModalityState;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.NodeRenderer;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.tree.AlphaComparator;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;

public abstract class AbstractTreeClassChooserDialog<T extends PsiNamedElement> extends DialogWrapper implements TreeChooser<T> {
  private Tree myTree;
  private T mySelectedClass = null;
  @Nonnull
  private final Project myProject;
  private BaseProjectTreeBuilder myBuilder;
  private TabbedPaneWrapper myTabbedPane;
  private ChooseByNamePanel myGotoByNamePanel;
  private final ProjectAwareSearchScope myScope;
  @Nonnull
  private final Predicate<T> myClassFilter;
  private final Class<T> myElementClass;
  @Nullable
  private final T myBaseClass;
  private T myInitialClass;
  private final boolean myIsShowMembers;
  private final boolean myIsShowLibraryContents;

  public AbstractTreeClassChooserDialog(String title, Project project, final Class<T> elementClass) {
    this(title, project, elementClass, null);
  }

  public AbstractTreeClassChooserDialog(String title, Project project, final Class<T> elementClass, @Nullable T initialClass) {
    this(title, project, ProjectScopes.getProjectScope(project), elementClass, null, initialClass);
  }

  public AbstractTreeClassChooserDialog(
    String title,
    @Nonnull Project project,
    ProjectAwareSearchScope scope,
    @Nonnull Class<T> elementClass,
    @Nullable Predicate<T> classFilter,
    @Nullable T initialClass
  ) {
    this(title, project, scope, elementClass, classFilter, null, initialClass, false, true);
  }

  public AbstractTreeClassChooserDialog(
    String title,
    @Nonnull Project project,
    ProjectAwareSearchScope scope,
    @Nonnull Class<T> elementClass,
    @Nullable Predicate<T> classFilter,
    @Nullable T baseClass,
    @Nullable T initialClass,
    boolean isShowMembers,
    boolean isShowLibraryContents
  ) {
    super(project, true);
    myScope = scope;
    myElementClass = elementClass;
    myClassFilter = classFilter == null ? allFilter() : classFilter;
    myBaseClass = baseClass;
    myInitialClass = initialClass;
    myIsShowMembers = isShowMembers;
    myIsShowLibraryContents = isShowLibraryContents;
    setTitle(title);
    myProject = project;
    init();
    if (initialClass != null) {
      select(initialClass);
    }

    handleSelectionChanged();
  }

  private Predicate<T> allFilter() {
    return element -> true;
  }

  @Override
  protected JComponent createCenterPanel() {
    final DefaultTreeModel model = new DefaultTreeModel(new DefaultMutableTreeNode());
    myTree = new Tree(model);

    ProjectAbstractTreeStructureBase treeStructure = new AbstractProjectTreeStructure(myProject) {
      @Override
      public boolean isFlattenPackages() {
        return false;
      }

      @Override
      public boolean isShowMembers() {
        return myIsShowMembers;
      }

      @Override
      public boolean isHideEmptyMiddlePackages() {
        return true;
      }

      @Override
      public boolean isAbbreviatePackageNames() {
        return false;
      }

      @Override
      public boolean isShowLibraryContents() {
        return myIsShowLibraryContents;
      }

      @Override
      public boolean isShowModules() {
        return false;
      }
    };
    myBuilder = new ProjectTreeBuilder(myProject, myTree, model, AlphaComparator.INSTANCE, treeStructure);

    myTree.setRootVisible(false);
    myTree.setShowsRootHandles(true);
    myTree.expandRow(0);
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    myTree.setCellRenderer(new NodeRenderer());
    UIUtil.setLineStyleAngled(myTree);

    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTree);
    scrollPane.setPreferredSize(new Dimension(500, 300));

    myTree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (KeyEvent.VK_ENTER == e.getKeyCode()) {
          doOKAction();
        }
      }
    });

    new DoubleClickListener() {
      @Override
      protected boolean onDoubleClick(MouseEvent event) {
        TreePath path = myTree.getPathForLocation(event.getX(), event.getY());
        if (path != null && myTree.isPathSelected(path)) {
          doOKAction();
          return true;
        }
        return false;
      }
    }.installOn(myTree);

    myTree.addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        handleSelectionChanged();
      }
    });

    new TreeSpeedSearch(myTree);

    myTabbedPane = new TabbedPaneWrapper(getDisposable());

    final JPanel dummyPanel = new JPanel(new BorderLayout());
    String name = null;
/*
    if (myInitialClass != null) {
      name = myInitialClass.getName();
    }
*/
    myGotoByNamePanel = new ChooseByNamePanel(myProject, createChooseByNameModel(), name, myScope.isSearchInLibraries(), getContext()) {

      @Override
      protected void showTextFieldPanel() {
      }

      @Override
      protected void close(boolean isOk) {
        super.close(isOk);

        if (isOk) {
          doOKAction();
        }
        else {
          doCancelAction();
        }
      }

      @Nonnull
      @Override
      protected Set<Object> filter(@Nonnull Set<Object> elements) {
        return doFilter(elements);
      }

      @Override
      protected void initUI(ChooseByNamePopupComponent.Callback callback, ModalityState modalityState, boolean allowMultipleSelection) {
        super.initUI(callback, modalityState, allowMultipleSelection);
        dummyPanel.add(myGotoByNamePanel.getPanel(), BorderLayout.CENTER);
        IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(IdeFocusTraversalPolicy.getPreferredFocusedComponent(myGotoByNamePanel.getPanel()));
      }

      @Override
      protected void showList() {
        super.showList();
        if (myInitialClass != null && myList.getModel().getSize() > 0) {
          myList.setSelectedValue(myInitialClass, true);
          myInitialClass = null;
        }
      }

      @Override
      protected void chosenElementMightChange() {
        handleSelectionChanged();
      }
    };

    Disposer.register(myDisposable, myGotoByNamePanel);

    myTabbedPane.addTab(IdeLocalize.tabChooserSearchByName().get(), dummyPanel);
    myTabbedPane.addTab(IdeLocalize.tabChooserProject().get(), scrollPane);

    myGotoByNamePanel.invoke(new MyCallback(), getModalityState(), false);

    myTabbedPane.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        handleSelectionChanged();
      }
    });

    return myTabbedPane.getComponent();
  }

  private Set<Object> doFilter(Set<Object> elements) {
    Set<Object> result = new LinkedHashSet<Object>();
    for (Object o : elements) {
      if (myElementClass.isInstance(o) && getFilter().test((T)o)) {
        result.add(o);
      }
    }
    return result;
  }

  protected ChooseByNameModel createChooseByNameModel() {
    if (myBaseClass == null) {
      return new MyGotoClassModel<T>(myProject, this);
    }
    else {
      TreeClassInheritorsProvider<T> inheritorsProvider = getInheritorsProvider(myBaseClass);
      if (inheritorsProvider != null) {
        return new SubclassGotoClassModel<T>(myProject, this, inheritorsProvider);
      }
      else {
        throw new IllegalStateException("inheritors provider is null");
      }
    }
  }

  /**
   * Makes sense only in case of not null base class.
   *
   * @param baseClass
   * @return
   */
  @Nullable
  protected TreeClassInheritorsProvider<T> getInheritorsProvider(@Nonnull T baseClass) {
    return null;
  }

  private void handleSelectionChanged() {
    T selection = calcSelectedClass();
    setOKActionEnabled(selection != null);
  }

  @Override
  protected void doOKAction() {
    mySelectedClass = calcSelectedClass();
    if (mySelectedClass == null) return;
    if (!myClassFilter.test(mySelectedClass)) {
      Messages.showErrorDialog(myTabbedPane.getComponent(), SymbolPresentationUtil.getSymbolPresentableText(mySelectedClass) + " is not acceptable");
      return;
    }
    super.doOKAction();
  }

  @Override
  public T getSelected() {
    return mySelectedClass;
  }

  @Override
  public void select(@Nonnull final T aClass) {
    selectElementInTree(aClass);
  }

  @Override
  public void showDialog() {
    show();
  }

  @Override
  public void showPopup() {
    //todo leak via not shown dialog?
    ChooseByNamePopup popup = ChooseByNamePopup.createPopup(myProject, createChooseByNameModel(), getContext());
    popup.invoke(new ChooseByNamePopupComponent.Callback() {
      @Override
      public void elementChosen(Object element) {
        mySelectedClass = (T)element;
        ((Navigatable)element).navigate(true);
      }
    }, getModalityState(), true);
  }

  private T getContext() {
    return myBaseClass != null ? myBaseClass : myInitialClass != null ? myInitialClass : null;
  }


  protected void selectElementInTree(@Nonnull final PsiElement element) {
    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        if (myBuilder == null) return;
        final VirtualFile vFile = PsiUtilBase.getVirtualFile(element);
        myBuilder.select(element, vFile, false);
      }
    }, getModalityState());
  }

  private IdeaModalityState getModalityState() {
    return IdeaModalityState.stateForComponent(getRootPane());
  }


  @Nullable
  protected T calcSelectedClass() {
    if (getTabbedPane().getSelectedIndex() == 0) {
      return (T)getGotoByNamePanel().getChosenElement();
    }
    else {
      TreePath path = getTree().getSelectionPath();
      if (path == null) return null;
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
      return getSelectedFromTreeUserObject(node);
    }
  }

  protected abstract T getSelectedFromTreeUserObject(DefaultMutableTreeNode node);

  @Override
  public void dispose() {
    if (myBuilder != null) {
      Disposer.dispose(myBuilder);
      myBuilder = null;
    }
    super.dispose();
  }

  @Override
  protected String getDimensionServiceKey() {
    return "#consulo.ide.impl.idea.ide.util.TreeClassChooserDialog";
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myGotoByNamePanel.getPreferredFocusedComponent();
  }

  @Nonnull
  protected Project getProject() {
    return myProject;
  }

  protected ProjectAwareSearchScope getScope() {
    return myScope;
  }

  @Nonnull
  protected Predicate<T> getFilter() {
    return myClassFilter;
  }

  protected T getBaseClass() {
    return myBaseClass;
  }

  protected T getInitialClass() {
    return myInitialClass;
  }

  protected TabbedPaneWrapper getTabbedPane() {
    return myTabbedPane;
  }

  protected Tree getTree() {
    return myTree;
  }

  protected ChooseByNamePanel getGotoByNamePanel() {
    return myGotoByNamePanel;
  }

  protected static class MyGotoClassModel<T extends PsiNamedElement> extends GotoClassModel2 {
    private final AbstractTreeClassChooserDialog<T> myTreeClassChooserDialog;

    AbstractTreeClassChooserDialog<T> getTreeClassChooserDialog() {
      return myTreeClassChooserDialog;
    }

    public MyGotoClassModel(@Nonnull Project project, AbstractTreeClassChooserDialog<T> treeClassChooserDialog) {
      super(project);
      myTreeClassChooserDialog = treeClassChooserDialog;
    }

    @Nonnull
    @Override
    public Object[] getElementsByName(String name, FindSymbolParameters parameters, @Nonnull ProgressIndicator canceled) {
      String patternName = parameters.getLocalPatternName();
      Collection<T> classes = myTreeClassChooserDialog.getClassesByName(name, parameters.isSearchInLibraries(), patternName, myTreeClassChooserDialog.getScope());
      if (classes.size() == 0) return ArrayUtil.EMPTY_OBJECT_ARRAY;
      if (classes.size() == 1) {
        return isAccepted(ContainerUtil.getFirstItem(classes)) ? ArrayUtil.toObjectArray(classes) : ArrayUtil.EMPTY_OBJECT_ARRAY;
      }

      Set<String> qNames = ContainerUtil.newHashSet();
      List<T> list = new ArrayList<>(classes.size());
      for (T aClass : classes) {
        if (qNames.add(getFullName(aClass)) && isAccepted(aClass)) {
          list.add(aClass);
        }
      }
      return ArrayUtil.toObjectArray(list);
    }

    @Override
    @Nullable
    public String getPromptText() {
      return null;
    }

    protected boolean isAccepted(T aClass) {
      return myTreeClassChooserDialog.getFilter().test(aClass);
    }
  }

  @Nonnull
  protected abstract Collection<T> getClassesByName(final String name, final boolean searchInLibraries, final String pattern, final ProjectAwareSearchScope searchScope);

  private static class SubclassGotoClassModel<T extends PsiNamedElement> extends MyGotoClassModel<T> {
    private final TreeClassInheritorsProvider<T> myInheritorsProvider;

    private boolean myFastMode = true;

    public SubclassGotoClassModel(@Nonnull final Project project, @Nonnull final AbstractTreeClassChooserDialog<T> treeClassChooserDialog, @Nonnull TreeClassInheritorsProvider<T> inheritorsProvider) {
      super(project, treeClassChooserDialog);
      myInheritorsProvider = inheritorsProvider;
      assert myInheritorsProvider.getBaseClass() != null;
    }

    @Nonnull
    @Override
    public String[] getNames(boolean checkBoxState) {
      if (!myFastMode) {
        return myInheritorsProvider.getNames();
      }
      final List<String> names = new ArrayList<String>();

      myFastMode = myInheritorsProvider.searchForInheritorsOfBaseClass().forEach(new Processor<T>() {
        private int count;

        @Override
        public boolean process(T aClass) {
          if (count++ > 1000) {
            return false;
          }
          if ((getTreeClassChooserDialog().getFilter().test(aClass)) && aClass.getName() != null) {
            names.add(aClass.getName());
          }
          return true;
        }
      });
      if (!myFastMode) {
        return getNames(checkBoxState);
      }
      if ((getTreeClassChooserDialog().getFilter().test(myInheritorsProvider.getBaseClass())) && myInheritorsProvider.getBaseClass().getName() != null) {
        names.add(myInheritorsProvider.getBaseClass().getName());
      }
      return ArrayUtil.toStringArray(names);
    }


    @Override
    protected boolean isAccepted(T aClass) {
      if (myFastMode) {
        return getTreeClassChooserDialog().getFilter().test(aClass);
      }
      else {
        return (aClass == getTreeClassChooserDialog().getBaseClass() || myInheritorsProvider.isInheritorOfBaseClass(aClass)) && getTreeClassChooserDialog().getFilter().test(aClass);
      }
    }
  }

  private class MyCallback extends ChooseByNamePopupComponent.Callback {
    @Override
    public void elementChosen(Object element) {
      mySelectedClass = (T)element;
      close(OK_EXIT_CODE);
    }
  }
}
