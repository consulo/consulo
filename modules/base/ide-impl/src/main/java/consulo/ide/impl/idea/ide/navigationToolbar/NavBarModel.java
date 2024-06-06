// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.ide.navigationToolbar;

import consulo.util.lang.StringUtil;
import consulo.util.lang.ObjectUtil;
import consulo.ide.navigationToolbar.NavBarModelExtension;
import consulo.util.lang.function.PairProcessor;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.application.ReadAction;
import consulo.application.impl.internal.LaterInvocator;
import consulo.application.ui.UISettings;
import consulo.application.util.function.CommonProcessors;
import consulo.application.util.function.Processor;
import consulo.dataContext.DataContext;
import consulo.language.psi.*;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.tree.TreeAnchorizer;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.*;

import static consulo.language.psi.PsiUtilCore.findFileSystemItem;

/**
 * @author Konstantin Bulenkov
 * @author Anna Kozlova
 */
public class NavBarModel {
  private List<Object> myModel = Collections.emptyList();
  private int mySelectedIndex;
  private final Project myProject;
  private final NavBarModelListener myNotificator;
  private final NavBarModelBuilder myBuilder;
  private boolean myChanged = true;
  private boolean updated = false;
  private boolean isFixedComponent = false;

  public NavBarModel(final Project project) {
    this(project, project.getMessageBus().syncPublisher(NavBarModelListener.class), NavBarModelBuilder.getInstance());
  }

  protected NavBarModel(Project project, NavBarModelListener notificator, NavBarModelBuilder builder) {
    myProject = project;
    myNotificator = notificator;
    myBuilder = builder;
  }

  public int getSelectedIndex() {
    return mySelectedIndex;
  }

  @Nullable
  public Object getSelectedValue() {
    return getElement(mySelectedIndex);
  }

  @Nullable
  public Object getElement(int index) {
    if (index != -1 && index < myModel.size()) {
      return get(index);
    }
    return null;
  }

  public int size() {
    return myModel.size();
  }

  public boolean isEmpty() {
    return myModel.isEmpty();
  }

  public int getIndexByModel(int index) {
    if (index < 0) return myModel.size() + index;
    if (index >= myModel.size() && myModel.size() > 0) return index % myModel.size();
    return index;
  }

  protected void updateModel(DataContext dataContext) {
    if (LaterInvocator.isInModalContext() || (updated && !isFixedComponent)) return;

    if (dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT) instanceof NavBarPanel) return;

    NavBarModelExtension ownerExtension = null;
    PsiElement psiElement = null;
    for (NavBarModelExtension extension : NavBarModelExtension.EP_NAME.getExtensionList()) {
      psiElement = extension.getLeafElement(dataContext);
      if (psiElement != null) {
        ownerExtension = extension;
        break;
      }
    }

    if (psiElement == null) {
      psiElement = dataContext.getData(PsiFile.KEY);
    }
    if (psiElement == null) {
      psiElement = dataContext.getData(PsiElement.KEY);
      if (psiElement == null) {
        psiElement = findFileSystemItem(dataContext.getData(Project.KEY), dataContext.getData(VirtualFile.KEY));
      }
    }

    if (ownerExtension == null) {
      psiElement = normalize(psiElement);
    }
    if (!myModel.isEmpty() && Objects.equals(get(myModel.size() - 1), psiElement) && !myChanged) return;

    if (psiElement != null && psiElement.isValid()) {
      updateModel(psiElement, ownerExtension);
    }
    else {
      if (UISettings.getInstance().getShowNavigationBar() && !myModel.isEmpty()) return;

      Object root = calculateRoot(dataContext);

      if (root != null) {
        setModel(Collections.singletonList(root));
      }
    }
    setChanged(false);

    updated = true;
  }

  private Object calculateRoot(DataContext dataContext) {
    // Narrow down the root element to the first interesting one
    Module root = dataContext.getData(Module.KEY);

    Project project = dataContext.getData(Project.KEY);
    if (project == null) return null;

    Object projectChild;
    Object projectGrandChild = null;

    CommonProcessors.FindFirstAndOnlyProcessor<Object> processor = new CommonProcessors.FindFirstAndOnlyProcessor<>();
    processChildren(project, processor);
    projectChild = processor.reset();
    if (projectChild != null) {
      processChildren(projectChild, processor);
      projectGrandChild = processor.reset();
    }
    return ObjectUtil.chooseNotNull(projectGrandChild, ObjectUtil.chooseNotNull(projectChild, project));
  }

  protected void updateModel(final PsiElement psiElement, @Nullable NavBarModelExtension ownerExtension) {

    final Set<VirtualFile> roots = new HashSet<>();
    final ProjectRootManager projectRootManager = ProjectRootManager.getInstance(myProject);
    final ProjectFileIndex projectFileIndex = projectRootManager.getFileIndex();

    for (VirtualFile root : projectRootManager.getContentRoots()) {
      VirtualFile parent = root.getParent();
      if (parent == null || !projectFileIndex.isInContent(parent)) {
        roots.add(root);
      }
    }

    for (final NavBarModelExtension modelExtension : NavBarModelExtension.EP_NAME.getExtensionList()) {
      for (VirtualFile root : modelExtension.additionalRoots(psiElement.getProject())) {
        VirtualFile parent = root.getParent();
        if (parent == null || !projectFileIndex.isInContent(parent)) {
          roots.add(root);
        }
      }
    }

    List<Object> updatedModel = ReadAction.compute(() -> isValid(psiElement) ? myBuilder.createModel(psiElement, roots, ownerExtension) : Collections.emptyList());

    setModel(ContainerUtil.reverse(updatedModel));
  }

  void revalidate() {
    final List<Object> objects = new ArrayList<>();
    boolean update = false;
    for (Object o : myModel) {
      if (isValid(TreeAnchorizer.getService().retrieveElement(o))) {
        objects.add(o);
      }
      else {
        update = true;
        break;
      }
    }
    if (update) {
      setModel(objects);
    }
  }

  protected void setModel(List<Object> model) {
    setModel(model, false);
  }

  protected void setModel(List<Object> model, boolean force) {
    if (!model.equals(TreeAnchorizer.retrieveList(myModel))) {
      myModel = TreeAnchorizer.anchorizeList(model);
      myNotificator.modelChanged();

      mySelectedIndex = myModel.size() - 1;
      myNotificator.selectionChanged();
    }
    else if (force) {
      myModel = TreeAnchorizer.anchorizeList(model);
      myNotificator.modelChanged();
    }
  }

  public void updateModel(final Object object) {
    if (object instanceof PsiElement) {
      updateModel((PsiElement)object, null);
    }
    else if (object instanceof Module) {
      List<Object> l = new ArrayList<>();
      l.add(myProject);
      l.add(object);
      setModel(l);
    }
  }

  protected boolean hasChildren(Object object) {
    return !processChildren(object, new CommonProcessors.FindFirstProcessor<>());
  }

  //to avoid the following situation: element was taken from NavBarPanel via data context and all left children
  // were truncated by traverseToRoot
  public void setChanged(boolean changed) {
    myChanged = changed;
  }

  static boolean isValid(final Object object) {
    if (object instanceof Project) {
      return !((Project)object).isDisposed();
    }
    if (object instanceof Module) {
      return !((Module)object).isDisposed();
    }
    if (object instanceof PsiElement) {
      return ReadAction.compute(() -> ((PsiElement)object).isValid()).booleanValue();
    }
    return object != null;
  }

  @Nullable
  public static PsiElement normalize(@Nullable PsiElement child) {
    if (child == null) return null;

    List<NavBarModelExtension> extensions = NavBarModelExtension.EP_NAME.getExtensionList();
    for (int i = extensions.size() - 1; i >= 0; i--) {
      NavBarModelExtension modelExtension = extensions.get(i);
      child = modelExtension.adjustElement(child);
      if (child == null) return null;
    }
    return child;
  }

  protected List<Object> getChildren(final Object object) {
    final List<Object> result = new ArrayList<>();
    PairProcessor<Object, NavBarModelExtension> processor = (o, ext) -> {
      ContainerUtil.addIfNotNull(result, o instanceof PsiElement && ext.normalizeChildren() ? normalize((PsiElement)o) : o);
      return true;
    };

    processChildrenWithExtensions(object, processor);

    result.sort(new SiblingsComparator());
    return result;
  }

  private boolean processChildren(Object object, @Nonnull Processor<Object> processor) {
    return processChildrenWithExtensions(object, (o, ext) -> processor.process(o));
  }

  private boolean processChildrenWithExtensions(Object object, @Nonnull PairProcessor<Object, NavBarModelExtension> pairProcessor) {
    if (!isValid(object)) return true;
    final Object rootElement = size() > 1 ? getElement(1) : null;
    if (rootElement != null && !isValid(rootElement)) return true;

    for (NavBarModelExtension modelExtension : NavBarModelExtension.EP_NAME.getExtensionList()) {
      if (!modelExtension.processChildren(object, rootElement, o -> pairProcessor.process(o, modelExtension))) return false;
    }
    return true;
  }

  public Object get(final int index) {
    return TreeAnchorizer.getService().retrieveElement(myModel.get(index));
  }

  public int indexOf(Object value) {
    for (int i = 0; i < myModel.size(); i++) {
      Object o = myModel.get(i);
      if (Objects.equals(TreeAnchorizer.getService().retrieveElement(o), value)) {
        return i;
      }
    }
    return -1;
  }

  public void setSelectedIndex(final int selectedIndex) {
    if (mySelectedIndex != selectedIndex) {
      mySelectedIndex = selectedIndex;
      myNotificator.selectionChanged();
    }
  }

  public void setFixedComponent(boolean fixedComponent) {
    isFixedComponent = fixedComponent;
  }

  private static final class SiblingsComparator implements Comparator<Object> {
    @Override
    public int compare(Object o1, Object o2) {
      int w1 = getWeight(o1);
      int w2 = getWeight(o2);
      if (w1 == 0) return w2 == 0 ? 0 : -1;
      if (w2 == 0) return 1;
      if (w1 != w2) return -w1 + w2;
      String s1 = NavBarPresentation.calcPresentableText(o1, false);
      String s2 = NavBarPresentation.calcPresentableText(o2, false);
      return StringUtil.naturalCompare(s1, s2);
    }

    private static int getWeight(Object object) {
      return object instanceof Module
             ? 5
             : object instanceof PsiDirectoryContainer ? 4 : object instanceof PsiDirectory ? 4 : object instanceof PsiFile ? 2 : object instanceof PsiNamedElement ? 3 : 0;
    }
  }
}
