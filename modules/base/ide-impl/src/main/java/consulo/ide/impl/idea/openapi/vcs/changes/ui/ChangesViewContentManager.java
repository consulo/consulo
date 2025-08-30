/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.vcs.changes.ui;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.lang.Comparing;
import consulo.util.lang.Trinity;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsMappingListener;
import consulo.versionControlSystem.VcsToolWindow;
import consulo.versionControlSystem.change.ChangesViewContentFactory;
import consulo.versionControlSystem.change.ChangesViewContentProvider;
import consulo.versionControlSystem.internal.ChangesViewContentI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.util.*;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public class ChangesViewContentManager implements ChangesViewContentI, Disposable {
    public static ChangesViewContentI getInstance(Project project) {
        return ChangesViewContentI.getInstance(project);
    }

    @Deprecated
    @DeprecationInfo(value = "Use VcsToolWindow#ID")
    public static final String TOOLWINDOW_ID = VcsToolWindow.ID;

    private static final Key<ChangesViewContentFactory> ourEpKey = Key.create(ChangesViewContentFactory.class);

    private final List<Content> myAddedContents = new ArrayList<>();
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final ContentFactory myContentFactory;

    private ToolWindow myToolWindow;

    private ContentManager myContentManager;

    private Trinity<Image, LocalizeValue, Boolean> myAlreadyLoadedState;

    @RequiredUIAccess
    @Inject
    public ChangesViewContentManager(@Nonnull Project project, @Nonnull ContentFactory contentFactory) {
        myProject = project;
        myContentFactory = contentFactory;
        project.getMessageBus().connect().subscribe(VcsMappingListener.class, this::update);
    }

    @Nonnull
    public List<Content> setContentManager(@Nonnull ToolWindow toolWindow, @Nonnull ContentManager contentManager) {
        if (myToolWindow != null) {
            throw new IllegalArgumentException();
        }

        myToolWindow = toolWindow;
        ArrayList<Content> contents = new ArrayList<>(myAddedContents);
        myAddedContents.clear();
        myContentManager = contentManager;

        MyContentManagerListener contentManagerListener = new MyContentManagerListener();
        contentManager.addContentManagerListener(contentManagerListener, this);

        return contents;
    }

    public void loadExtensionTabs() {
        List<Content> contentList = new LinkedList<>();
        List<ChangesViewContentFactory> contentEPs = myProject.getExtensionList(ChangesViewContentFactory.class);
        for (ChangesViewContentFactory factory : contentEPs) {
            if (factory.isAvailable()) {
                Content content = myContentFactory.createContent(new ContentStub(factory), factory.getTabName().getValue(), false);
                content.setCloseable(false);
                content.putUserData(ourEpKey, factory);
                contentList.add(content);
            }
        }
        myAddedContents.addAll(0, contentList);
    }

    private void addExtensionTab(ChangesViewContentFactory factory) {
        Content content = myContentFactory.createContent(new ContentStub(factory), factory.getTabName().getValue(), false);
        content.setCloseable(false);
        content.putUserData(ourEpKey, factory);
        addIntoCorrectPlace(content);
    }

    private void updateExtensionTabs() {
        List<ChangesViewContentFactory> contentFactoryList = myProject.getExtensionList(ChangesViewContentFactory.class);
        for (ChangesViewContentFactory factory : contentFactoryList) {
            Content epContent = findContent(factory);
            boolean predicateResult = factory.isAvailable();
            if (predicateResult && epContent == null) {
                addExtensionTab(factory);
            }
            else if (!predicateResult && epContent != null) {
                myContentManager.removeContent(epContent, true);
            }
        }
    }

    @Nullable
    private Content findContent(ChangesViewContentFactory factory) {
        if (myContentManager == null) {
            for (Content content : myAddedContents) {
                if (content instanceof ContentStub && ((ContentStub) content).getChangesViewContentFactory() == factory) {
                    return content;
                }
            }
            return null;
        }

        Content[] contents = myContentManager.getContents();
        for (Content content : contents) {
            if (content.getUserData(ourEpKey) == factory) {
                return content;
            }
        }
        return null;
    }

    @RequiredUIAccess
    private void updateToolWindowAvailability() {
        AbstractVcs[] vcses = ProjectLevelVcsManager.getInstance(myProject).getAllActiveVcss();

        LocalizeValue displayName;
        Image image;
        boolean avaliable = vcses.length > 0;

        if (vcses.length == 1) {
            AbstractVcs vcs = vcses[0];
            displayName = vcs.getDisplayName();
            image = vcs.getIcon();
        }
        else {
            displayName = UILocalize.toolWindowNameVersionControl();
            image = PlatformIconGroup.toolwindowsToolwindowchanges();
        }

        if (myToolWindow != null) {
            myToolWindow.setIcon(image);
            myToolWindow.setDisplayName(displayName);
            myToolWindow.setAvailable(avaliable, null);
        }
        else {
            myAlreadyLoadedState = Trinity.create(image, displayName, avaliable);
        }
    }

    @Nullable
    public Trinity<Image, LocalizeValue, Boolean> getAlreadyLoadedState() {
        return myAlreadyLoadedState;
    }

    public void setAlreadyLoadedState(Trinity<Image, LocalizeValue, Boolean> alreadyLoadedState) {
        myAlreadyLoadedState = alreadyLoadedState;
    }

    @Override
    public void addContent(Content content) {
        if (myContentManager == null) {
            myAddedContents.add(content);
        }
        else {
            addIntoCorrectPlace(content);
        }
    }

    @Override
    public void removeContent(Content content) {
        if (myContentManager != null && (!myContentManager.isDisposed())) { // for unit tests
            myContentManager.removeContent(content, true);
        }
    }

    @Override
    public void setSelectedContent(Content content) {
        myContentManager.setSelectedContent(content);
    }

    @Override
    @Nullable
    public <T> T getActiveComponent(Class<T> aClass) {
        if (myContentManager == null) {
            return null;
        }

        Content content = myContentManager.getSelectedContent();
        if (content != null && aClass.isInstance(content.getComponent())) {
            //noinspection unchecked
            return (T) content.getComponent();
        }
        return null;
    }

    @Override
    public boolean isContentSelected(@Nonnull String contentName) {
        Content selectedContent = myContentManager.getSelectedContent();
        if (selectedContent == null) {
            return false;
        }
        return Comparing.equal(contentName, selectedContent.getTabName());
    }

    @Override
    public void selectContent(@Nonnull String tabName, boolean requestFocus) {
        for (Content content : myContentManager.getContents()) {
            if (content.getDisplayName().equals(tabName)) {
                myContentManager.setSelectedContent(content, requestFocus);
                break;
            }
        }
    }

    public void update() {
        Application.get().invokeLater(() -> {
            updateToolWindowAvailability();
            if (myToolWindow != null) {
                updateExtensionTabs();
            }
        });
    }

    @Override
    public void dispose() {
    }

    private static class ContentStub extends JPanel {
        private final ChangesViewContentFactory myChangesViewContentFactory;

        private ContentStub(ChangesViewContentFactory contentFactory) {
            myChangesViewContentFactory = contentFactory;
        }

        public ChangesViewContentFactory getChangesViewContentFactory() {
            return myChangesViewContentFactory;
        }
    }

    private class MyContentManagerListener implements ContentManagerListener {
        @RequiredUIAccess
        @Override
        public void selectionChanged(ContentManagerEvent event) {
            Content content = event.getContent();
            if (content.getComponent() instanceof ContentStub) {
                ChangesViewContentFactory contentFactory = ((ContentStub) content.getComponent()).getChangesViewContentFactory();
                ChangesViewContentProvider provider = contentFactory.create();
                JComponent contentComponent = provider.initContent();
                content.setComponent(contentComponent);
                content.setDisposer(provider::disposeContent);
            }
        }
    }

    public static final String LOCAL_CHANGES = "Local Changes";
    public static final String REPOSITORY = "Repository";
    public static final String INCOMING = "Incoming";
    public static final String SHELF = "Shelf";
    private static final String[] ourPresetOrder = {LOCAL_CHANGES, REPOSITORY, INCOMING, SHELF};

    public static List<Content> doPresetOrdering(List<Content> contents) {
        List<Content> result = new ArrayList<>(contents.size());
        for (String preset : ourPresetOrder) {
            for (Iterator<Content> iterator = contents.iterator(); iterator.hasNext(); ) {
                Content current = iterator.next();
                if (preset.equals(current.getTabName())) {
                    iterator.remove();
                    result.add(current);
                }
            }
        }
        result.addAll(contents);
        return result;
    }

    private void addIntoCorrectPlace(Content content) {
        if (myContentManager == null) {
            myAddedContents.add(content);
            return;
        }

        String name = content.getTabName();
        Content[] contents = myContentManager.getContents();

        int idxOfBeingInserted = -1;
        for (int i = 0; i < ourPresetOrder.length; i++) {
            String s = ourPresetOrder[i];
            if (s.equals(name)) {
                idxOfBeingInserted = i;
            }
        }
        if (idxOfBeingInserted == -1) {
            myContentManager.addContent(content);
            return;
        }

        Set<String> existingNames = new HashSet<>();
        for (Content existingContent : contents) {
            existingNames.add(existingContent.getTabName());
        }

        int place = idxOfBeingInserted;
        for (int i = 0; i < idxOfBeingInserted; i++) {
            if (!existingNames.contains(ourPresetOrder[i])) {
                --place;
            }

        }
        myContentManager.addContent(content, place);
    }
}
