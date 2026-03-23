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
package consulo.externalSystem.impl.internal.service.project.manage;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.externalSystem.impl.internal.util.ExternalSystemUtil;
import consulo.externalSystem.model.DataNode;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.externalSystem.service.project.ProjectData;
import consulo.externalSystem.service.project.manage.ExternalProjectsManager;
import consulo.externalSystem.service.project.manage.ExternalSystemShortcutsManager;
import consulo.externalSystem.service.project.manage.ExternalSystemTaskActivator;
import consulo.externalSystem.view.ExternalProjectsView;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Vladislav.Soroka
 */
@Singleton
@ServiceImpl
public class ExternalProjectsManagerImpl implements ExternalProjectsManager {
    private final Project myProject;
    private final ExternalSystemShortcutsManager myShortcutsManager;
    private final ExternalSystemTaskActivator myTaskActivator;

    private final Map<String, ExternalProjectsView> myViews = new ConcurrentHashMap<>();
    /** systemId.id → (projectPath → DataNode<ProjectData>) */
    private final Map<String, Map<String, DataNode<ProjectData>>> myProjectDataCache = new ConcurrentHashMap<>();
    private final List<Runnable> myPostInitActivities = new ArrayList<>();
    private volatile boolean myInitialized;

    @Inject
    public ExternalProjectsManagerImpl(Project project) {
        myProject = project;
        myShortcutsManager = new ExternalSystemShortcutsManager(project);
        myTaskActivator = new ExternalSystemTaskActivator(project);
    }
   
    public static ExternalProjectsManagerImpl getInstance(Project project) {
        return (ExternalProjectsManagerImpl)ExternalProjectsManager.getInstance(project);
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public void runWhenInitialized(Runnable runnable) {
        if (myInitialized) {
            Application app = myProject.getApplication();
            app.invokeLater(runnable, myProject.getDisposed());
        }
        else {
            synchronized (myPostInitActivities) {
                if (myInitialized) {
                    Application app = myProject.getApplication();
                    app.invokeLater(runnable, myProject.getDisposed());
                }
                else {
                    myPostInitActivities.add(runnable);
                }
            }
        }
    }

    /**
     * Called from {@link consulo.externalSystem.impl.internal.service.ExternalSystemStartupActivity} after startup.
     */
    public void init() {
        List<Runnable> activities;
        synchronized (myPostInitActivities) {
            myInitialized = true;
            activities = new ArrayList<>(myPostInitActivities);
            myPostInitActivities.clear();
        }
        Application app = myProject.getApplication();
        for (Runnable activity : activities) {
            app.invokeLater(activity, myProject.getDisposed());
        }
    }

    @Override
   
    public ExternalSystemShortcutsManager getShortcutsManager() {
        return myShortcutsManager;
    }

    @Override
   
    public ExternalSystemTaskActivator getTaskActivator() {
        return myTaskActivator;
    }

    @Override
    public void registerView(ProjectSystemId systemId, ExternalProjectsView view) {
        myViews.put(systemId.getId(), view);
        if (myProjectDataCache.get(systemId.getId()) != null) {
            // Data already in cache (e.g. imported before tool window was opened) — just render it
            view.scheduleStructureUpdate();
        }
        else {
            // No cached data yet — trigger a background refresh to populate the tree
            ExternalSystemUtil.refreshProjects(myProject, systemId, true);
        }
    }

    @Override
    @Nullable
    public ExternalProjectsView getView(ProjectSystemId systemId) {
        return myViews.get(systemId.getId());
    }

    @Override
    public boolean isIgnored(DataNode<?> dataNode) {
        return dataNode.isIgnored();
    }

    @Override
    public void setIgnored(DataNode<?> dataNode, boolean ignored) {
        dataNode.setIgnored(ignored);
        // Refresh the view for this node's system
        for (ExternalProjectsView view : myViews.values()) {
            view.scheduleStructureUpdate();
        }
    }

    /**
     * Called from {@link ExternalProjectDataCacheServiceImpl} when project data is imported.
     */
    public void onProjectImported(DataNode<ProjectData> projectNode) {
        ProjectData data = projectNode.getData();
        String systemId = data.getOwner().getId();
        String projectPath = data.getLinkedExternalProjectPath();
        myProjectDataCache.computeIfAbsent(systemId, k -> new ConcurrentHashMap<>()).put(projectPath, projectNode);
        ExternalProjectsView view = myViews.get(systemId);
        if (view != null) {
            view.scheduleStructureUpdate();
        }
    }

    @Override
    public Collection<DataNode<ProjectData>> getProjectData(ProjectSystemId systemId) {
        Map<String, DataNode<ProjectData>> map = myProjectDataCache.get(systemId.getId());
        return map == null ? Collections.emptyList() : Collections.unmodifiableCollection(map.values());
    }
}
