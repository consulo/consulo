/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.hierarchy.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.hierarchy.HierarchyBrowserManager;
import consulo.language.editor.hierarchy.HierarchyBrowser;
import consulo.language.editor.hierarchy.HierarchyProvider;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageOneToMany;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.util.collection.ContainerUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.List;

/**
 * @author yole
 */
public abstract class BrowseHierarchyActionBase<T extends HierarchyProvider> extends AnAction {
  private static final ExtensionPointCacheKey CACHE_KEY =
          ExtensionPointCacheKey.<HierarchyProvider, ByLanguageValue<List<HierarchyProvider>>>create("HierarchyProvider", LanguageOneToMany.build(false));

  private static final Logger LOG = Logger.getInstance(BrowseHierarchyActionBase.class);
  private final Class<T> myHierarchyClass;

  protected BrowseHierarchyActionBase(@Nonnull Class<T> hierarchyClass) {
    myHierarchyClass = hierarchyClass;
  }

  @RequiredUIAccess
  @Override
  public final void actionPerformed(@Nonnull final AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Project project = e.getData(Project.KEY);
    if (project == null) return;

    PsiDocumentManager.getInstance(project).commitAllDocuments(); // prevents problems with smart pointers creation

    final HierarchyProvider provider = getProvider(e);
    if (provider == null) return;
    final PsiElement target = provider.getTarget(dataContext);
    if (target == null) return;
    createAndAddToPanel(project, provider, target);
  }

  public static HierarchyBrowser createAndAddToPanel(@Nonnull Project project, @Nonnull final HierarchyProvider provider, @Nonnull PsiElement target) {
    final HierarchyBrowser hierarchyBrowser = provider.createHierarchyBrowser(target);

    final Content content;

    final HierarchyBrowserManager hierarchyBrowserManager = HierarchyBrowserManager.getInstance(project);

    final ContentManager contentManager = hierarchyBrowserManager.getContentManager();
    final Content selectedContent = contentManager.getSelectedContent();
    if (selectedContent != null && !selectedContent.isPinned()) {
      content = selectedContent;
      final Component component = content.getComponent();
      if (component instanceof Disposable) {
        Disposer.dispose((Disposable)component);
      }
      content.setComponent(hierarchyBrowser.getComponent());
    }
    else {
      content = ContentFactory.getInstance().createContent(hierarchyBrowser.getComponent(), null, true);
      contentManager.addContent(content);
    }
    contentManager.setSelectedContent(content);
    hierarchyBrowser.setContent(content);

    final Runnable runnable = () -> provider.browserActivated(hierarchyBrowser);
    ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.HIERARCHY).activate(runnable);
    return hierarchyBrowser;
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull final AnActionEvent e) {
    Application application = Application.get();
    if (!application.getExtensionPoint(myHierarchyClass).hasAnyExtensions()) {
      e.getPresentation().setVisible(false);
    }
    else {
      final boolean enabled = isEnabled(e);
      if (ActionPlaces.isPopupPlace(e.getPlace())) {
        e.getPresentation().setVisible(enabled);
      }
      else {
        e.getPresentation().setVisible(true);
      }
      e.getPresentation().setEnabled(enabled);
    }
  }

  @RequiredReadAction
  private boolean isEnabled(final AnActionEvent e) {
    final HierarchyProvider provider = getProvider(e);
    if (LOG.isDebugEnabled()) {
      LOG.debug("Using provider " + provider);
    }
    if (provider == null) return false;
    PsiElement target = provider.getTarget(e.getDataContext());
    if (LOG.isDebugEnabled()) {
      LOG.debug("Target: " + target);
    }
    return target != null;
  }

  @Nullable
  @RequiredReadAction
  private HierarchyProvider getProvider(final AnActionEvent e) {
    return findProvider(myHierarchyClass, e.getData(PsiElement.KEY), e.getData(PsiFile.KEY), e.getDataContext());
  }

  @Nullable
  @RequiredReadAction
  public static <T extends HierarchyProvider> T findProvider(@Nonnull Class<T> extension, @Nullable PsiElement psiElement, @Nullable PsiFile psiFile, @Nonnull DataContext dataContext) {
    final T provider = findBestHierarchyProvider(extension, psiElement, dataContext);
    if (provider == null) {
      return findBestHierarchyProvider(extension, psiFile, dataContext);
    }
    return provider;
  }

  @Nullable
  @RequiredReadAction
  @SuppressWarnings("unchecked")
  public static <T extends HierarchyProvider> T findBestHierarchyProvider(final Class<T> extension, @Nullable PsiElement element, DataContext dataContext) {
    if (element == null) return null;
    ExtensionPoint<T> point = Application.get().getExtensionPoint(extension);
    ByLanguageValue<List<T>> get = (ByLanguageValue<List<T>>)point.getOrBuildCache(CACHE_KEY);

    List<T> providers = get.requiredGet(element.getLanguage());
    for (T provider : providers) {
      PsiElement target = provider.getTarget(dataContext);
      if (target != null) {
        return provider;
      }
    }
    return ContainerUtil.getFirstItem(providers);
  }
}
