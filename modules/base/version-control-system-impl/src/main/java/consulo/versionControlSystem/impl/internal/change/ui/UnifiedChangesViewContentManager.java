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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.Component;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.image.Image;
import consulo.ui.layout.VerticalLayout;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.PluginVcsMappingListener;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsMappingListener;
import consulo.versionControlSystem.VcsToolWindow;
import consulo.versionControlSystem.change.ChangesViewContentFactory;
import consulo.versionControlSystem.change.ChangesViewContentManagerListener;
import consulo.versionControlSystem.change.ChangesViewContentProvider;
import consulo.versionControlSystem.commit.CommitModeListener;
import consulo.versionControlSystem.commit.CommitModeManager;
import consulo.versionControlSystem.internal.ChangesViewContentI;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
@Singleton
@ServiceImpl
public class UnifiedChangesViewContentManager implements ChangesViewContentI, Disposable {
    private static final Logger LOG = Logger.getInstance(UnifiedChangesViewContentManager.class);

    private static final Key<ChangesViewContentFactory> FACTORY_KEY = Key.create("UnifiedChangesViewContentManager.FACTORY_KEY");

    private static final Map<String, Integer> TAB_WEIGHTS = Map.of(
        LOCAL_CHANGES, 10,
        REPOSITORY, 20,
        INCOMING, 30,
        SHELF, 40
    );

    private static final int OTHER_TAB_WEIGHT = 100;

    private final Project myProject;
    private final Application myApplication;
    private final ContentFactory myContentFactory;

    private final Map<String, ToolWindow> myToolWindows = new LinkedHashMap<>();
    private final List<Content> myAddedContents = new ArrayList<>();
    private final Map<Content, ChangesViewContentProvider> myProviders = new HashMap<>();
    private final Map<Content, Component> myProviderComponents = new HashMap<>();
    private final Set<Content> myFailedContents = new HashSet<>();
    private final Set<Content> myInternalRemovals = new HashSet<>();

    private @Nullable Content mySelectedAddedContent;

    private boolean myExtensionTabsLoaded;

    private @Nullable ToolWindowState myToolWindowState;

    @Inject
    public UnifiedChangesViewContentManager(Project project, Application application, ContentFactory contentFactory) {
        myProject = project;
        myApplication = application;
        myContentFactory = contentFactory;

        project.getMessageBus().connect(this).subscribe(VcsMappingListener.class, this::update);
        project.getMessageBus().connect(this).subscribe(PluginVcsMappingListener.class, this::update);
        project.getMessageBus().connect(this).subscribe(CommitModeListener.class, this::onCommitModeChanged);
        project.getMessageBus().connect(this).subscribe(ChangesViewContentManagerListener.class, this::update);
    }

    public static UnifiedChangesViewContentManager getInstanceImpl(Project project) {
        return (UnifiedChangesViewContentManager) ChangesViewContentI.getInstance(project);
    }

    @RequiredUIAccess
    public void attachToolWindow(ToolWindow toolWindow) {
        String id = toolWindow.getId();
        if (myToolWindows.containsKey(id)) {
            return;
        }

        myToolWindows.put(id, toolWindow);

        ContentManager contentManager = toolWindow.getContentManager();
        contentManager.addContentManagerListener(new ContentManagerListener() {
            @RequiredUIAccess
            @Override
            public void selectionChanged(ContentManagerEvent event) {
                Content content = event.getContent();
                if (content.isSelected()) {
                    initializeLazyContent(content);
                }
            }

            @Override
            public void contentAdded(ContentManagerEvent event) {
                updateLocalChangesTitle();
            }

            @Override
            public void contentRemoved(ContentManagerEvent event) {
                updateLocalChangesTitle();

                Content content = event.getContent();
                ChangesViewContentFactory factory = content.getUserData(FACTORY_KEY);
                if (factory != null && factory.isCloseable() && !myInternalRemovals.contains(content)) {
                    try {
                        factory.contentClosedByUser();
                    }
                    catch (RuntimeException e) {
                        LOG.error(e);
                    }
                }
            }
        }, this);

        if (VcsToolWindow.ID.equals(id)) {
            loadExtensionTabs();

            ToolWindowState state = myToolWindowState;
            if (state != null) {
                applyToolWindowState(toolWindow, state);
            }
        }

        List<Content> contents = new ArrayList<>();
        for (Content content : myAddedContents) {
            if (resolveToolWindowId(content).equals(id)) {
                contents.add(content);
            }
        }
        myAddedContents.removeAll(contents);

        for (Content content : contents) {
            addIntoCorrectPlace(contentManager, content);
        }

        Content toSelect = mySelectedAddedContent;
        if (toSelect != null && contents.contains(toSelect)) {
            contentManager.setSelectedContent(toSelect);
            mySelectedAddedContent = null;
        }
        else {
            selectFirstContent(contentManager);
        }

        update();
    }

    public boolean isToolWindowAvailable() {
        ToolWindowState state = myToolWindowState;
        return state != null && state.available();
    }

    public boolean isCommitToolWindowAvailable() {
        return isToolWindowAvailable() && CommitModeManager.getInstance(myProject).isCommitToolWindowEnabled();
    }

    @RequiredUIAccess
    private void updateCommitToolWindowAvailability() {
        ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(VcsToolWindow.COMMIT_ID);
        if (toolWindow != null) {
            toolWindow.setAvailable(isCommitToolWindowAvailable(), null);
        }
    }

    private void loadExtensionTabs() {
        if (myExtensionTabsLoaded) {
            return;
        }
        myExtensionTabsLoaded = true;

        myProject.getExtensionPoint(ChangesViewContentFactory.class).forEach(factory -> {
            if (factory.isAvailable()) {
                myAddedContents.add(createExtensionContent(factory));
            }
        });
    }

    @RequiredUIAccess
    private void updateExtensionTabs() {
        myProject.getExtensionPoint(ChangesViewContentFactory.class).forEach(factory -> {
            Content content = findContentOf(factory);
            boolean available = factory.isAvailable();
            if (available && content == null) {
                addContent(createExtensionContent(factory));
            }
            else if (!available && content != null) {
                removeContent(content, true);
            }
        });
    }

    private Content createExtensionContent(ChangesViewContentFactory factory) {
        Content content = myContentFactory.createUIContent(VerticalLayout.create(), factory.getTabName().get(), false);
        content.setCloseable(factory.isCloseable());
        content.putUserData(FACTORY_KEY, factory);
        content.putUserData(CONTENT_ID_KEY, factory.getId());
        if (factory.isInCommitToolWindow()) {
            content.putUserData(IN_COMMIT_TOOL_WINDOW_KEY, Boolean.TRUE);
        }
        return content;
    }

    private @Nullable Content findContentOf(ChangesViewContentFactory factory) {
        List<Content> contents = findContents(content -> content.getUserData(FACTORY_KEY) == factory);
        return contents.isEmpty() ? null : contents.get(0);
    }

    @RequiredUIAccess
    private void initializeLazyContent(Content content) {
        ChangesViewContentFactory factory = content.getUserData(FACTORY_KEY);
        if (factory == null || myProviders.containsKey(content) || myFailedContents.contains(content)) {
            return;
        }

        ChangesViewContentProvider provider;
        Component component;
        try {
            provider = factory.create();
            component = provider.initContent();
        }
        catch (RuntimeException e) {
            myFailedContents.add(content);
            LOG.error("Cannot create the '" + factory.getId() + "' tab", e);
            return;
        }

        myProviders.put(content, provider);
        myProviderComponents.put(content, component);

        content.setUIComponent(component);
        content.setDisposer(() -> {
            myProviders.remove(content);
            myProviderComponents.remove(content);
            provider.disposeContent();
        });
    }

    private String resolveToolWindowId(Content content) {
        if (isInCommitToolWindow(content) && CommitModeManager.getInstance(myProject).isCommitToolWindowEnabled()) {
            return VcsToolWindow.COMMIT_ID;
        }
        return VcsToolWindow.ID;
    }

    private static boolean isInCommitToolWindow(Content content) {
        if (Boolean.TRUE.equals(content.getUserData(IN_COMMIT_TOOL_WINDOW_KEY))) {
            return true;
        }

        String contentId = ChangesViewContentI.getContentId(content);
        return LOCAL_CHANGES.equals(contentId) || SHELF.equals(contentId);
    }

    private @Nullable ContentManager resolveContentManager(Content content) {
        ToolWindow toolWindow = myToolWindows.get(resolveToolWindowId(content));
        return toolWindow == null ? null : toolWindow.getContentManager();
    }

    @RequiredUIAccess
    private void onCommitModeChanged() {
        updateCommitToolWindowAvailability();

        List<Content> remapped = findContents(content -> resolveContentManager(content) != content.getManager());
        for (Content content : remapped) {
            try {
                removeContent(content, false);
            }
            catch (RuntimeException e) {
                LOG.error(e);
            }
        }
        for (Content content : remapped) {
            try {
                addContent(content);
            }
            catch (RuntimeException e) {
                LOG.error(e);
            }
        }

        for (ToolWindow toolWindow : myToolWindows.values()) {
            selectFirstContent(toolWindow.getContentManager());
        }
    }

    @Override
    public void addContent(Content content) {
        ContentManager contentManager = resolveContentManager(content);
        if (contentManager == null) {
            myAddedContents.add(content);
        }
        else {
            addIntoCorrectPlace(contentManager, content);
        }
    }

    @Override
    public void removeContent(Content content) {
        removeContent(content, true);
    }

    private void removeContent(Content content, boolean dispose) {
        if (dispose) {
            myFailedContents.remove(content);
        }

        ContentManager contentManager = content.getManager();
        if (contentManager == null || contentManager.isDisposed()) {
            myAddedContents.remove(content);
            if (mySelectedAddedContent == content) {
                mySelectedAddedContent = null;
            }
        }
        else {
            myInternalRemovals.add(content);
            try {
                contentManager.removeContent(content, dispose);
            }
            finally {
                myInternalRemovals.remove(content);
            }
        }
    }

    @Override
    public void setSelectedContent(Content content) {
        setSelectedContent(content, false);
    }

    private void setSelectedContent(Content content, boolean requestFocus) {
        ContentManager contentManager = content.getManager();
        if (contentManager != null) {
            contentManager.setSelectedContent(content, requestFocus);
            mySelectedAddedContent = null;
        }
        else if (myAddedContents.contains(content)) {
            mySelectedAddedContent = content;
        }
    }

    @Override
    public <T> @Nullable T getActiveComponent(Class<T> aClass) {
        for (ToolWindow toolWindow : myToolWindows.values()) {
            Content content = toolWindow.getContentManager().getSelectedContent();
            if (content == null) {
                continue;
            }

            Component component = myProviderComponents.get(content);
            if (aClass.isInstance(component)) {
                return aClass.cast(component);
            }

            Object awtComponent = content.getComponent();
            if (aClass.isInstance(awtComponent)) {
                return aClass.cast(awtComponent);
            }
        }
        return null;
    }

    @Override
    public boolean isContentSelected(String contentId) {
        for (ToolWindow toolWindow : myToolWindows.values()) {
            Content selected = toolWindow.getContentManager().getSelectedContent();
            if (selected != null && Objects.equals(contentId, ChangesViewContentI.getContentId(selected))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void selectContent(String contentId, boolean requestFocus) {
        List<Content> contents = findContents(content -> Objects.equals(contentId, ChangesViewContentI.getContentId(content)));
        if (!contents.isEmpty()) {
            setSelectedContent(contents.get(0), requestFocus);
        }
    }

    public String getToolWindowIdFor(String contentId) {
        List<Content> contents = findContents(content -> Objects.equals(contentId, ChangesViewContentI.getContentId(content)));
        return contents.isEmpty() ? VcsToolWindow.ID : resolveToolWindowId(contents.get(0));
    }

    @Override
    public @Nullable ToolWindow getToolWindowFor(String contentId) {
        return ToolWindowManager.getInstance(myProject).getToolWindow(getToolWindowIdFor(contentId));
    }

    private List<Content> findContents(Predicate<Content> predicate) {
        List<Content> result = new ArrayList<>();
        for (ToolWindow toolWindow : myToolWindows.values()) {
            for (Content content : toolWindow.getContentManager().getContents()) {
                if (predicate.test(content)) {
                    result.add(content);
                }
            }
        }
        for (Content content : myAddedContents) {
            if (predicate.test(content)) {
                result.add(content);
            }
        }
        return result;
    }

    public void update() {
        myApplication.invokeLater(() -> {
            updateToolWindowAvailability();
            if (myToolWindows.containsKey(VcsToolWindow.ID)) {
                updateExtensionTabs();
            }
        }, ModalityState.nonModal(), myProject.getDisposed());
    }

    @RequiredUIAccess
    private void updateToolWindowAvailability() {
        AbstractVcs[] vcses = ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss();

        ToolWindowState state;
        if (vcses.length == 1) {
            state = new ToolWindowState(vcses[0].getIcon(), vcses[0].getDisplayName(), true);
        }
        else {
            state = new ToolWindowState(PlatformIconGroup.toolwindowsToolwindowchanges(),
                UILocalize.toolWindowNameVersionControl(),
                vcses.length > 0);
        }
        myToolWindowState = state;

        ToolWindow toolWindow = myToolWindows.get(VcsToolWindow.ID);
        if (toolWindow != null) {
            applyToolWindowState(toolWindow, state);
        }

        updateCommitToolWindowAvailability();
    }

    private static void applyToolWindowState(ToolWindow toolWindow, ToolWindowState state) {
        toolWindow.setIcon(state.icon());
        toolWindow.setDisplayName(state.displayName());
        toolWindow.setAvailable(state.available(), null);
    }

    private static void selectFirstContent(ContentManager contentManager) {
        Content first = contentManager.getContentCount() > 0 ? contentManager.getContent(0) : null;
        if (first != null && contentManager.getSelectedContent() != first) {
            contentManager.setSelectedContent(first);
        }
    }

    private void updateLocalChangesTitle() {
        for (ToolWindow toolWindow : myToolWindows.values()) {
            ContentManager contentManager = toolWindow.getContentManager();
            for (Content content : contentManager.getContents()) {
                if (LOCAL_CHANGES.equals(ChangesViewContentI.getContentId(content))) {
                    content.setDisplayName(localChangesTitle(toolWindow, contentManager));
                }
            }
        }
    }

    private static String localChangesTitle(ToolWindow toolWindow, ContentManager contentManager) {
        if (contentManager.getContentCount() == 1) {
            return "";
        }
        if (VcsToolWindow.COMMIT_ID.equals(toolWindow.getId())) {
            return VcsLocalize.changesViewTabCommit().get();
        }
        return VcsLocalize.changesViewTabLocalChanges().get();
    }

    private static int weightOf(Content content) {
        String contentId = ChangesViewContentI.getContentId(content);
        Integer weight = contentId == null ? null : TAB_WEIGHTS.get(contentId);
        if (weight != null) {
            return weight;
        }
        ChangesViewContentFactory factory = content.getUserData(FACTORY_KEY);
        return factory != null ? factory.getOrderWeight() : OTHER_TAB_WEIGHT;
    }

    private static void addIntoCorrectPlace(ContentManager contentManager, Content content) {
        int weight = weightOf(content);
        Content[] contents = contentManager.getContents();

        int index = contents.length;
        for (int i = 0; i < contents.length; i++) {
            if (weightOf(contents[i]) > weight) {
                index = i;
                break;
            }
        }
        contentManager.addContent(content, index);
    }

    @Override
    public void dispose() {
        myAddedContents.clear();
        mySelectedAddedContent = null;
    }

    private record ToolWindowState(@Nullable Image icon, LocalizeValue displayName, boolean available) {
    }
}
