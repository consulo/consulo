// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.review.impl.internal.toolwindow;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.review.impl.internal.ReviewUIUtil;
import consulo.review.toolwindow.ReviewTab;
import consulo.review.toolwindow.ReviewTabsComponentFactory;
import consulo.review.toolwindow.ReviewTabViewModel;
import consulo.review.toolwindow.ReviewToolwindowDataKeys;
import consulo.review.toolwindow.ReviewToolwindowProjectViewModel;
import consulo.review.toolwindow.ReviewToolwindowViewModel;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Manages review toolwindow tabs and their content.
 * If {@link ReviewToolwindowViewModel#getProjectVm()} in {@code reviewToolwindowViewModel} is null UI for acquiring this context is
 * shown (by {@link ReviewTabsComponentFactory#createEmptyTabContent}).
 * <p>
 * When {@link ReviewToolwindowViewModel#getProjectVm()} appears Review List will be shown
 * (by {@link ReviewTabsComponentFactory#createReviewListComponent}), and {@link ReviewToolwindowProjectViewModel#getTabs()} will be
 * managed, according to described in {@link ReviewTab} {@link ReviewTab#getId()} logic.
 * So, new tabs will be shown using {@link ReviewTabsComponentFactory#createTabComponent}.
 * <p>
 * Several controllers may share one tool window, and each only touches the contents it created.
 *
 * @see ReviewToolwindowDataKeys
 */
final class ReviewToolwindowTabsController<
    T extends ReviewTab,
    TVM extends ReviewTabViewModel,
    PVM extends ReviewToolwindowProjectViewModel<T, TVM>
    > {

    private static final Key<ReviewToolwindowTabsController<?, ?, ?>> OWNER_KEY = Key.create("consulo.review.toolwindow.owner");
    private static final Key<ReviewTab> REVIEW_TAB_KEY = Key.create("consulo.review.toolwindow.review.tab");
    private static final Key<ReviewTabViewModel> REVIEW_TAB_VM_KEY = Key.create("consulo.review.toolwindow.review.tab.vm");


    private final Project myProject;
    private final ToolWindow myToolwindow;
    private final ContentFactory myContentFactory;
    private final ContentManager myContentManager;
    private final ReviewToolwindowViewModel<PVM> myReviewToolwindowViewModel;
    private final ReviewTabsComponentFactory<TVM, PVM> myTabComponentFactory;
    private final LocalizeValue myTabTitle;

    private Disposable myProjectVmDisposable;

    @RequiredUIAccess
    ReviewToolwindowTabsController(
        Disposable parentDisposable,
        Project project,
        ToolWindow toolwindow,
        ContentFactory contentFactory,
        ReviewToolwindowViewModel<PVM> reviewToolwindowViewModel,
        ReviewTabsComponentFactory<TVM, PVM> tabComponentFactory,
        LocalizeValue tabTitle
    ) {
        myProject = project;
        myToolwindow = toolwindow;
        myContentFactory = contentFactory;
        myContentManager = toolwindow.getContentManager();
        myReviewToolwindowViewModel = reviewToolwindowViewModel;
        myTabComponentFactory = tabComponentFactory;
        myTabTitle = tabTitle;

        myProjectVmDisposable = Disposable.newDisposable();
        Disposer.register(parentDisposable, () -> Disposer.dispose(myProjectVmDisposable));

        myContentManager.addUiDataProvider(sink -> {
            Content selectedContent = myContentManager.getSelectedContent();
            if (selectedContent != null && selectedContent.getUserData(OWNER_KEY) == this) {
                sink.set(ReviewToolwindowDataKeys.REVIEW_TOOLWINDOW_PROJECT_VM, myReviewToolwindowViewModel.getProjectVm().get());
                sink.set(ReviewToolwindowDataKeys.REVIEW_TOOLWINDOW_VM, myReviewToolwindowViewModel);
            }
        });

        ReviewUIUtil.bind(parentDisposable, myReviewToolwindowViewModel.getProjectVm(), this::onProjectVmChanged);
    }

    @RequiredUIAccess
    private void onProjectVmChanged(@Nullable PVM vm) {
        Disposer.dispose(myProjectVmDisposable);
        removeOwnContents();

        myProjectVmDisposable = Disposable.newDisposable();
        Disposable scope = myProjectVmDisposable;
        if (vm == null) {
            Content loginContent = createDisposableContent(scope, (content, contentDisposable) -> {
                content.setUIComponent(myTabComponentFactory.createEmptyTabContent(contentDisposable));
                content.setCloseable(false);
            });
            myContentManager.addContent(loginContent);
            myContentManager.setSelectedContent(loginContent);
        }
        else {
            manageProjectTabs(scope, vm);
        }
    }

    @RequiredUIAccess
    private void manageProjectTabs(Disposable scope, PVM projectVm) {
        Content mainContent = createMainTabContent(scope, projectVm);
        myContentManager.addContent(mainContent);
        myContentManager.setSelectedContent(mainContent);
        refreshReviewListOnTabSelection(projectVm, myContentManager, mainContent);
        refreshListOnToolwindowShow(projectVm, myToolwindow, mainContent);

        // required for backwards sync contentManager -> VM
        ContentManagerListener syncListener = new ContentManagerListener() {
            @Override
            @SuppressWarnings("unchecked")
            public void contentRemoved(ContentManagerEvent event) {
                Content content = event.getContent();
                if (content.getUserData(OWNER_KEY) != ReviewToolwindowTabsController.this) {
                    return;
                }
                ReviewTab tab = content.getUserData(REVIEW_TAB_KEY);
                if (tab != null) {
                    projectVm.closeTab((T) tab);
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public void selectionChanged(ContentManagerEvent event) {
                Content content = event.getContent();
                if (content.getUserData(OWNER_KEY) != ReviewToolwindowTabsController.this) {
                    return;
                }
                if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
                    projectVm.selectTab((T) content.getUserData(REVIEW_TAB_KEY));
                }
            }
        };

        boolean[] listening = {false};
        Disposer.register(scope, () -> {
            if (listening[0]) {
                myContentManager.removeContentManagerListener(syncListener);
            }
        });

        ReviewUIUtil.bind(scope, projectVm.getTabs(), tabsState -> {
            myContentManager.removeContentManagerListener(syncListener);
            listening[0] = false;
            for (Content content : myContentManager.getContents()) {
                if (content != mainContent && content.getUserData(OWNER_KEY) == this) {
                    ReviewTab tab = content.getUserData(REVIEW_TAB_KEY);
                    if (tab == null || !tabsState.tabs().containsKey(tab)) {
                        myContentManager.removeContent(content, true);
                    }
                }
            }

            for (Map.Entry<T, TVM> entry : tabsState.tabs().entrySet()) {
                Content existing = findTabContent(entry.getKey());
                if (existing == null || existing.getUserData(REVIEW_TAB_VM_KEY) != entry.getValue()) {
                    closeExistingTabAndCreateNew(scope, entry.getKey(), projectVm, entry.getValue());
                }
            }

            T selectedTab = tabsState.selectedTab();
            Content tabContent = selectedTab == null ? null : findTabContent(selectedTab);
            Content contentToSelect = tabContent != null ? tabContent : mainContent;
            myContentManager.setSelectedContent(contentToSelect, true);
            myContentManager.addContentManagerListener(syncListener);
            listening[0] = true;
        });
    }

    private @Nullable Content findTabContent(T reviewTab) {
        for (Content content : myContentManager.getContents()) {
            if (content.getUserData(OWNER_KEY) == this && Objects.equals(content.getUserData(REVIEW_TAB_KEY), reviewTab)) {
                return content;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private void closeExistingTabAndCreateNew(Disposable scope, T tab, PVM projectVm, TVM tabVm) {
        Content existingContent = findTabContent(tab);
        if (existingContent != null) {
            myContentManager.removeContent(existingContent, true);
        }

        Content content = createTabContent(scope, tab, projectVm, tabVm);
        myContentManager.addContent(content);
    }

    @RequiredUIAccess
    private Content createMainTabContent(Disposable scope, PVM projectVm) {
        return createDisposableContent(scope, (content, contentDisposable) -> {
            content.setCloseable(false);
            content.setDisplayName(projectVm.getProjectName().get());

            content.setUIComponent(myTabComponentFactory.createReviewListComponent(contentDisposable, projectVm));
        });
    }

    @RequiredUIAccess
    private Content createTabContent(Disposable scope, T tab, PVM projectVm, TVM tabVm) {
        return createDisposableContent(scope, (content, contentDisposable) -> {
            content.setCloseable(true);
            content.setDisplayName(tabVm.getDisplayName().get());
            content.setDescription(projectVm.getProjectName().get() + ": " + tabVm.getDescription().get());

            content.setUIComponent(myTabComponentFactory.createTabComponent(contentDisposable, projectVm, tabVm));

            content.putUserData(REVIEW_TAB_KEY, tab);
            content.putUserData(REVIEW_TAB_VM_KEY, tabVm);
        });
    }

    @RequiredUIAccess
    private Content createDisposableContent(Disposable scope, BiConsumer<Content, Disposable> modifier) {
        Content content = myContentFactory.createUIContent(null, myTabTitle.get(), false);
        content.putUserData(OWNER_KEY, this);
        Disposable disposable = Disposable.newDisposable();
        content.setDisposer(disposable);
        Disposer.register(scope, disposable);
        modifier.accept(content, disposable);
        return content;
    }

    @RequiredUIAccess
    private void removeOwnContents() {
        for (Content content : myContentManager.getContents()) {
            if (content.getUserData(OWNER_KEY) == this) {
                myContentManager.removeContent(content, true);
            }
        }
    }

    private void refreshReviewListOnTabSelection(PVM projectVm, ContentManager contentManager, Content content) {
        ContentManagerListener listener = new ContentManagerListener() {
            @Override
            public void selectionChanged(ContentManagerEvent event) {
                if (event.getOperation() == ContentManagerEvent.ContentOperation.add && event.getContent() == content) {
                    // tab selected
                    projectVm.refresh();
                }
            }
        };
        contentManager.addContentManagerListener(listener);
        Disposer.register(content, () -> contentManager.removeContentManagerListener(listener));
    }

    private void refreshListOnToolwindowShow(PVM projectVm, ToolWindow toolwindow, Content content) {
        myProject.getMessageBus().connect(content).subscribe(ToolWindowManagerListener.class, new ToolWindowManagerListener() {
            @Override
            public void toolWindowShown(ToolWindow shownToolwindow) {
                if (shownToolwindow.getId().equals(toolwindow.getId())) {
                    Content selectedContent = shownToolwindow.getContentManager().getSelectedContent();
                    if (selectedContent == content) {
                        projectVm.refresh();
                    }
                }
            }
        });
    }
}
