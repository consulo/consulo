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

package com.intellij.openapi.vcs.changes.ui;

import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.AbstractVcs;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsListener;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.ui.UIBundle;
import com.intellij.ui.content.*;
import com.intellij.util.Alarm;
import com.intellij.util.NotNullFunction;
import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.*;

/**
 * @author yole
 */
@Singleton
public class ChangesViewContentManager implements ChangesViewContentI {
  public static ChangesViewContentI getInstance(Project project) {
    return ServiceManager.getService(project, ChangesViewContentI.class);
  }

  @Deprecated
  @DeprecationInfo(value = "Use ToolWindowId#VCS", until = "2.0")
  public static final String TOOLWINDOW_ID = ToolWindowId.VCS;
  private static final Key<ChangesViewContentEP> myEPKey = Key.create("ChangesViewContentEP");

  private static final Logger LOG = Logger.getInstance(ChangesViewContentManager.class);

  private final ProjectLevelVcsManager myVcsManager;

  private final Alarm myVcsChangeAlarm;

  private final List<Content> myAddedContents = new ArrayList<>();
  @Nonnull
  private final Project myProject;

  private ToolWindow myToolWindow;
  private ContentManager myContentManager;

  @Inject
  public ChangesViewContentManager(@Nonnull Project project, final ProjectLevelVcsManager vcsManager) {
    myProject = project;
    myVcsManager = vcsManager;
    myVcsChangeAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, project);
    project.getMessageBus().connect().subscribe(ProjectLevelVcsManager.VCS_CONFIGURATION_CHANGED, new MyVcsListener());
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
    contentManager.addContentManagerListener(contentManagerListener);

    Disposer.register(myProject, () -> contentManager.removeContentManagerListener(contentManagerListener));

    return contents;
  }

  public void loadExtensionTabs() {
    final List<Content> contentList = new LinkedList<>();
    final ChangesViewContentEP[] contentEPs = ChangesViewContentEP.EP_NAME.getExtensions(myProject);
    for (ChangesViewContentEP ep : contentEPs) {
      final NotNullFunction<Project, Boolean> predicate = ep.newPredicateInstance(myProject);
      if (predicate == null || predicate.fun(myProject).equals(Boolean.TRUE)) {
        final Content content = ContentFactory.getInstance().createContent(new ContentStub(ep), ep.getTabName(), false);
        content.setCloseable(false);
        content.putUserData(myEPKey, ep);
        contentList.add(content);
      }
    }
    myAddedContents.addAll(0, contentList);
  }

  private void addExtensionTab(final ChangesViewContentEP ep) {
    final Content content = ContentFactory.getInstance().createContent(new ContentStub(ep), ep.getTabName(), false);
    content.setCloseable(false);
    content.putUserData(myEPKey, ep);
    addIntoCorrectPlace(content);
  }

  private void updateExtensionTabs() {
    final ChangesViewContentEP[] contentEPs = myProject.getExtensions(ChangesViewContentEP.EP_NAME);
    for (ChangesViewContentEP ep : contentEPs) {
      final NotNullFunction<Project, Boolean> predicate = ep.newPredicateInstance(myProject);
      if (predicate == null) continue;
      Content epContent = findEPContent(ep);
      final Boolean predicateResult = predicate.fun(myProject);
      if (predicateResult.equals(Boolean.TRUE) && epContent == null) {
        addExtensionTab(ep);
      }
      else if (predicateResult.equals(Boolean.FALSE) && epContent != null) {
        myContentManager.removeContent(epContent, true);
      }
    }
  }

  @Nullable
  private Content findEPContent(final ChangesViewContentEP ep) {
    if(myContentManager == null) {
      return null;
    }
    
    final Content[] contents = myContentManager.getContents();
    for (Content content : contents) {
      if (content.getUserData(myEPKey) == ep) {
        return content;
      }
    }
    return null;
  }

  @RequiredUIAccess
  private void updateToolWindowAvailability() {
    if (myToolWindow == null) {
      return;
    }
    AbstractVcs[] vcses = myVcsManager.getAllActiveVcss();

    if(vcses.length == 1) {
      AbstractVcs vcs = vcses[0];
      myToolWindow.setDisplayName(LocalizeValue.of(vcs.getDisplayName()));
      myToolWindow.setIcon(vcs.getIcon());
    }
    else {
      myToolWindow.setDisplayName(LocalizeValue.of(UIBundle.message("tool.window.name.version.control")));
      myToolWindow.setIcon(PlatformIconGroup.toolwindowsVcsSmallTab());
    }

    myToolWindow.setAvailable(vcses.length > 0, null);
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
  public void removeContent(final Content content) {
    if (myContentManager != null && (!myContentManager.isDisposed())) { // for unit tests
      myContentManager.removeContent(content, true);
    }
  }

  @Override
  public void setSelectedContent(final Content content) {
    myContentManager.setSelectedContent(content);
  }

  @Override
  @Nullable
  public <T> T getActiveComponent(final Class<T> aClass) {
    final Content content = myContentManager.getSelectedContent();
    if (content != null && aClass.isInstance(content.getComponent())) {
      //noinspection unchecked
      return (T)content.getComponent();
    }
    return null;
  }

  public boolean isContentSelected(@Nonnull String contentName) {
    Content selectedContent = myContentManager.getSelectedContent();
    if (selectedContent == null) return false;
    return Comparing.equal(contentName, selectedContent.getTabName());
  }

  @Override
  public void selectContent(@Nonnull String tabName) {
    selectContent(tabName, false);
  }

  public void selectContent(@Nonnull String tabName, boolean requestFocus) {
    for (Content content : myContentManager.getContents()) {
      if (content.getDisplayName().equals(tabName)) {
        myContentManager.setSelectedContent(content, requestFocus);
        break;
      }
    }
  }

  private class MyVcsListener implements VcsListener {
    @Override
    public void directoryMappingChanged() {
      myVcsChangeAlarm.cancelAllRequests();
      myVcsChangeAlarm.addRequest(() -> {
        if (myProject.isDisposed()) return;
        updateToolWindowAvailability();
        updateExtensionTabs();
      }, 100, ModalityState.NON_MODAL);
    }
  }

  private static class ContentStub extends JPanel {
    private final ChangesViewContentEP myEP;

    private ContentStub(final ChangesViewContentEP EP) {
      myEP = EP;
    }

    public ChangesViewContentEP getEP() {
      return myEP;
    }
  }

  private class MyContentManagerListener extends ContentManagerAdapter {
    @Override
    public void selectionChanged(final ContentManagerEvent event) {
      Content content = event.getContent();
      if (content.getComponent() instanceof ContentStub) {
        ChangesViewContentEP ep = ((ContentStub)content.getComponent()).getEP();
        final ChangesViewContentProvider provider = ep.getInstance(myProject);
        final JComponent contentComponent = provider.initContent();
        content.setComponent(contentComponent);
        content.setDisposer(() -> provider.disposeContent());
      }
    }
  }

  public static final String LOCAL_CHANGES = "Local Changes";
  public static final String REPOSITORY = "Repository";
  public static final String INCOMING = "Incoming";
  public static final String SHELF = "Shelf";
  private static final String[] ourPresetOrder = {LOCAL_CHANGES, REPOSITORY, INCOMING, SHELF};

  public static List<Content> doPresetOrdering(final List<Content> contents) {
    final List<Content> result = new ArrayList<>(contents.size());
    for (final String preset : ourPresetOrder) {
      for (Iterator<Content> iterator = contents.iterator(); iterator.hasNext(); ) {
        final Content current = iterator.next();
        if (preset.equals(current.getTabName())) {
          iterator.remove();
          result.add(current);
        }
      }
    }
    result.addAll(contents);
    return result;
  }

  private void addIntoCorrectPlace(final Content content) {
    final String name = content.getTabName();
    final Content[] contents = myContentManager.getContents();

    int idxOfBeingInserted = -1;
    for (int i = 0; i < ourPresetOrder.length; i++) {
      final String s = ourPresetOrder[i];
      if (s.equals(name)) {
        idxOfBeingInserted = i;
      }
    }
    if (idxOfBeingInserted == -1) {
      myContentManager.addContent(content);
      return;
    }

    final Set<String> existingNames = new HashSet<>();
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
