/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.impl.internal.hierarchy;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.extension.ExtensionPoint;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposer;
import consulo.language.editor.hierarchy.HierarchyKind;
import consulo.language.editor.hierarchy.HierarchyModel;
import consulo.language.editor.hierarchy.HierarchyProvider;
import consulo.language.editor.internal.hierarchy.HierarchyBrowseService;
import consulo.language.editor.internal.hierarchy.HierarchyBrowser;
import consulo.language.editor.internal.hierarchy.HierarchyBrowserFactory;
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
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.toolWindow.ToolWindow;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Owns everything about opening a hierarchy that no frontend needs to know: which provider serves a
 * language, how a model becomes a browser, and which tab of the hierarchy tool window it lands in.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
@Singleton
@ServiceImpl
public class HierarchyBrowseServiceImpl implements HierarchyBrowseService {
    private static final ExtensionPointCacheKey CACHE_KEY =
        ExtensionPointCacheKey.<HierarchyProvider, ByLanguageValue<List<HierarchyProvider>>>create(
            "HierarchyProvider",
            LanguageOneToMany.build(false)
        );

    private static final Logger LOG = Logger.getInstance(HierarchyBrowseServiceImpl.class);

    private final Project myProject;

    @Inject
    public HierarchyBrowseServiceImpl(Project project) {
        myProject = project;
    }

    @Override
    public boolean isSupported(HierarchyKind kind) {
        if (!HierarchyBrowserFactory.getInstance().isSupported()) {
            return false;
        }
        return myProject.getApplication().getExtensionPoint(HierarchyProvider.class).anyMatchSafe(provider -> kind.equals(provider.getKind()));
    }

    @Override
    @RequiredReadAction
    public boolean isAvailable(HierarchyKind kind, DataContext dataContext) {
        HierarchyProvider<?> provider = findProvider(kind, dataContext);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Using provider " + provider);
        }
        if (provider == null) {
            return false;
        }
        PsiElement target = findTarget(provider, dataContext);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Target: " + target);
        }
        return target != null;
    }

    @Override
    @RequiredUIAccess
    public void browse(HierarchyKind kind, DataContext dataContext) {
        PsiDocumentManager.getInstance(myProject).commitAllDocuments();

        HierarchyProvider<?> provider = findProvider(kind, dataContext);
        if (provider == null) {
            return;
        }
        PsiElement target = findTarget(provider, dataContext);
        if (target == null) {
            return;
        }

        doBrowse(provider, target, browser -> {
        });
    }

    @Override
    @RequiredUIAccess
    public void browse(HierarchyKind kind, PsiElement target, DataContext dataContext, Consumer<HierarchyBrowser> afterInit) {
        HierarchyProvider<?> provider = findProvider(kind, target, target.getContainingFile(), dataContext);
        if (provider == null) {
            return;
        }

        doBrowse(provider, target, afterInit);
    }

    @Override
    @RequiredUIAccess
    public <E extends PsiElement> void browse(HierarchyModel<E> model) {
        HierarchyBrowser browser = HierarchyBrowserFactory.getInstance().createBrowser(myProject, model);
        if (browser == null) {
            return;
        }

        showInToolWindow(browser, () -> {
        }, createdBrowser -> {
        });
    }

    @Override
    @RequiredUIAccess
    public @Nullable HierarchyBrowser createBrowser(HierarchyKind kind, DataContext dataContext) {
        HierarchyProvider<?> provider = findProvider(kind, dataContext);
        if (provider == null) {
            return null;
        }
        PsiElement target = findTarget(provider, dataContext);
        if (target == null) {
            return null;
        }

        HierarchyBrowser browser = doCreateBrowser(provider, target);
        if (browser == null) {
            return null;
        }

        browser.changeToDefaultView(false);
        return browser;
    }

    @RequiredUIAccess
    private <E extends PsiElement> void doBrowse(
        HierarchyProvider<E> provider,
        PsiElement target,
        Consumer<HierarchyBrowser> afterInit
    ) {
        @SuppressWarnings("unchecked")
        E typedTarget = (E)target;

        HierarchyBrowser browser = doCreateBrowser(provider, target);
        if (browser == null) {
            return;
        }

        showInToolWindow(
            browser,
            () -> {
                try {
                    provider.targetSelected(typedTarget);
                }
                catch (Exception e) {
                    LOG.error(e);
                }
            },
            afterInit
        );
    }

    @RequiredUIAccess
    private <E extends PsiElement> @Nullable HierarchyBrowser doCreateBrowser(HierarchyProvider<E> provider, PsiElement target) {
        @SuppressWarnings("unchecked")
        E typedTarget = (E)target;

        HierarchyModel<E> model;
        try {
            model = provider.createModel(myProject, typedTarget);
        }
        catch (Exception e) {
            LOG.error(e);
            return null;
        }

        return HierarchyBrowserFactory.getInstance().createBrowser(myProject, model);
    }

    @RequiredUIAccess
    private void showInToolWindow(HierarchyBrowser browser, Runnable afterShown, Consumer<HierarchyBrowser> afterInit) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(ToolWindowId.HIERARCHY);

        assert toolWindow != null;

        toolWindow.activate(() -> {
            ContentManager contentManager = toolWindow.getContentManager();
            Content selectedContent = contentManager.getSelectedContent();
            Content content;

            if (selectedContent != null && !selectedContent.isPinned()) {
                content = selectedContent;
                HierarchyBrowser previousBrowser = content.getUserData(HierarchyBrowser.KEY);
                if (previousBrowser != null) {
                    Disposer.dispose(previousBrowser);
                }
                content.setUIComponent(browser.getUIComponent());
            }
            else {
                content = ContentFactory.getInstance().createUIContent(browser.getUIComponent(), null, true);
                contentManager.addContent(content);
            }
            content.putUserData(HierarchyBrowser.KEY, browser);
            contentManager.setSelectedContent(content);
            browser.setContent(content);

            toolWindow.activate(() -> {
                browser.changeToDefaultView(true);
                afterShown.run();
            });

            afterInit.accept(browser);
        });
    }

    @RequiredReadAction
    private @Nullable HierarchyProvider<?> findProvider(HierarchyKind kind, DataContext dataContext) {
        return findProvider(kind, dataContext.getData(PsiElement.KEY), dataContext.getData(PsiFile.KEY), dataContext);
    }

    @RequiredReadAction
    private @Nullable HierarchyProvider<?> findProvider(
        HierarchyKind kind,
        @Nullable PsiElement psiElement,
        @Nullable PsiFile psiFile,
        DataContext dataContext
    ) {
        HierarchyProvider<?> provider = findBestHierarchyProvider(kind, psiElement, dataContext);
        if (provider == null) {
            return findBestHierarchyProvider(kind, psiFile, dataContext);
        }
        return provider;
    }

    @RequiredReadAction
    @SuppressWarnings("unchecked")
    private @Nullable HierarchyProvider<?> findBestHierarchyProvider(
        HierarchyKind kind,
        @Nullable PsiElement element,
        DataContext dataContext
    ) {
        if (element == null) {
            return null;
        }
        ExtensionPoint<HierarchyProvider> point = Application.get().getExtensionPoint(HierarchyProvider.class);
        ByLanguageValue<List<HierarchyProvider>> get = (ByLanguageValue<List<HierarchyProvider>>)point.getOrBuildCache(CACHE_KEY);

        List<HierarchyProvider> providers = get.requiredGet(element.getLanguage());
        HierarchyProvider<?> firstOfKind = null;
        for (HierarchyProvider provider : providers) {
            try {
                if (!kind.equals(provider.getKind())) {
                    continue;
                }
                if (firstOfKind == null) {
                    firstOfKind = provider;
                }
                if (provider.getTarget(dataContext) != null) {
                    return provider;
                }
            }
            catch (Exception e) {
                LOG.error(e);
            }
        }
        return firstOfKind;
    }

    @RequiredReadAction
    private static @Nullable PsiElement findTarget(HierarchyProvider<?> provider, DataContext dataContext) {
        try {
            return provider.getTarget(dataContext);
        }
        catch (Exception e) {
            LOG.error(e);
            return null;
        }
    }
}
