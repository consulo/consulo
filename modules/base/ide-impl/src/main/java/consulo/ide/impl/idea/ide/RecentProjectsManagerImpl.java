/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.registry.Registry;
import consulo.component.messagebus.MessageBusConnection;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.ide.impl.idea.openapi.wm.impl.SystemDock;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.module.extension.ModuleExtension;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ProjectGroup;
import consulo.project.ProjectManager;
import consulo.project.ProjectOpenContext;
import consulo.project.event.ProjectManagerListener;
import consulo.project.impl.internal.ProjectImplUtil;
import consulo.project.impl.internal.store.ProjectStoreImpl;
import consulo.project.internal.RecentProjectsManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeFrameState;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.util.collection.SmartList;
import consulo.util.io.FileUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.File;
import java.util.*;

/**
 * @author yole
 * @author Konstantin Bulenkov
 */
@Singleton
@ServiceImpl
@State(name = "RecentProjectsManager", storages = {@Storage(value = "recentProjects.xml", roamingType = RoamingType.DISABLED)})
public class RecentProjectsManagerImpl extends RecentProjectsManager implements PersistentStateComponent<RecentProjectsManagerImpl.State> {
    private static final int MAX_PROJECTS_IN_MAIN_MENU = 6;

    public static boolean isFileSystemPath(String path) {
        return path.indexOf('/') != -1 || path.indexOf('\\') != -1;
    }

    public static class State {
        public List<String> recentPaths = new SmartList<>();
        public List<String> openPaths = new SmartList<>();
        public Map<String, String> names = new LinkedHashMap<>();
        public List<ProjectGroup> groups = new SmartList<>();
        public String lastPath;

        public Map<String, RecentProjectMetaInfo> additionalInfo = new LinkedHashMap<>();
        public Map<String, IdeFrameState> frameStates = new LinkedHashMap<>();

        public String lastProjectLocation;

        void validateRecentProjects() {
            //noinspection StatementWithEmptyBody
            while (recentPaths.remove(null)) ;
            Collection<String> displayNames = names.values();
            //noinspection StatementWithEmptyBody
            while (displayNames.remove("")) ;

            while (recentPaths.size() > Registry.intValue("ide.max.recent.projects")) {
                int index = recentPaths.size() - 1;
                names.remove(recentPaths.get(index));
                recentPaths.remove(index);
            }
        }
    }

    private final Object myStateLock = new Object();
    private State myState = new State();

    private Set<String> myDuplicatesCache = null;
    private boolean isDuplicatesCacheUpdating = false;

    @Inject
    public RecentProjectsManagerImpl(@Nonnull Application application) {
        MessageBusConnection connection = application.getMessageBus().connect();
        connection.subscribe(AppLifecycleListener.class, new MyAppLifecycleListener());
        connection.subscribe(ProjectManagerListener.class, new MyProjectListener());
    }

    @Override
    public State getState() {
        synchronized (myStateLock) {
            myState.validateRecentProjects();
            return myState;
        }
    }

    @Override
    public void loadState(final State state) {
        removeDuplicates(state);
        if (state.lastPath != null && !new File(state.lastPath).exists()) {
            state.lastPath = null;
        }
        if (state.lastPath != null) {
            File lastFile = new File(state.lastPath);
            if (lastFile.isDirectory() && !new File(lastFile, Project.DIRECTORY_STORE_FOLDER).exists()) {
                state.lastPath = null;
            }
        }
        synchronized (myStateLock) {
            myState = state;
        }
    }

    protected void removeDuplicates(State state) {
        for (String path : new ArrayList<>(state.recentPaths)) {
            if (path.endsWith(File.separator)) {
                state.recentPaths.remove(path);
                state.additionalInfo.remove(path);
                state.openPaths.remove(path);
            }
        }
    }

    private static void removePathFrom(List<String> items, String path) {
        for (Iterator<String> iterator = items.iterator(); iterator.hasNext(); ) {
            final String next = iterator.next();
            if (Platform.current().fs().isCaseSensitive() ? path.equals(next) : path.equalsIgnoreCase(next)) {
                iterator.remove();
            }
        }
    }

    @Override
    public void removePath(@Nullable String path) {
        if (path == null) {
            return;
        }

        synchronized (myStateLock) {
            removePathFrom(myState.recentPaths, path);
            myState.names.remove(path);
            for (ProjectGroup group : myState.groups) {
                group.removeProject(path);
            }
        }
    }

    @Override
    public boolean hasPath(String path) {
        final State state = getState();
        return state != null && state.recentPaths.contains(path);
    }

    /**
     * @return a path pointing to a directory where the last project was created or null if not available
     */
    @Override
    @Nullable
    public String getLastProjectCreationLocation() {
        return myState.lastProjectLocation;
    }

    @Override
    public void setLastProjectCreationLocation(@Nullable String lastProjectLocation) {
        myState.lastProjectLocation = StringUtil.nullize(lastProjectLocation, true);
    }

    @Override
    public String getLastProjectPath() {
        return myState.lastPath;
    }

    @Override
    public void updateLastProjectPath() {
        final Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
        synchronized (myStateLock) {
            myState.openPaths.clear();
            if (openProjects.length == 0) {
                myState.lastPath = null;
            }
            else {
                myState.lastPath = getProjectPath(openProjects[openProjects.length - 1]);
                for (Project openProject : openProjects) {
                    String path = getProjectPath(openProject);
                    if (path != null) {
                        myState.openPaths.add(path);
                        myState.names.put(path, getProjectDisplayName(openProject));
                    }
                }
            }
        }
    }

    @Nonnull
    protected String getProjectDisplayName(@Nonnull Project project) {
        return "";
    }

    private Set<String> getDuplicateProjectNames(final Set<String> openedPaths, final Set<String> recentPaths) {
        if (myDuplicatesCache != null) {
            return myDuplicatesCache;
        }

        if (!isDuplicatesCacheUpdating) {
            isDuplicatesCacheUpdating = true; //assuming that this check happens only on EDT. So, no synchronised block or double-checked locking needed
            Application.get().executeOnPooledThread((Runnable) () -> {
                Set<String> names = new HashSet<>();
                final HashSet<String> duplicates = new HashSet<>();
                for (String path : ContainerUtil.union(openedPaths, recentPaths)) {
                    if (!names.add(RecentProjectsManagerImpl.this.getProjectName(path))) {
                        duplicates.add(path);
                    }
                }
                myDuplicatesCache = duplicates;
                isDuplicatesCacheUpdating = false;
            });
        }
        return new HashSet<>();
    }

    @Override
    public AnAction[] getRecentProjectsActions(boolean forMainMenu) {
        return getRecentProjectsActions(forMainMenu, false);
    }

    @Override
    public AnAction[] getRecentProjectsActions(boolean forMainMenu, boolean useGroups) {
        final Set<String> paths;
        synchronized (myStateLock) {
            myState.validateRecentProjects();
            paths = new LinkedHashSet<>(myState.recentPaths);
        }

        Set<String> openedPaths = new HashSet<>();
        for (Project openProject : ProjectManager.getInstance().getOpenProjects()) {
            consulo.util.collection.ContainerUtil.addIfNotNull(openedPaths, getProjectPath(openProject));
        }

        paths.remove(null);
        paths.removeAll(openedPaths);

        List<AnAction> actions = new SmartList<>();
        Set<String> duplicates = getDuplicateProjectNames(openedPaths, paths);
        if (useGroups) {
            final List<ProjectGroup> groups = new ArrayList<>(new ArrayList<>(myState.groups));
            final List<String> projectPaths = new ArrayList<>(paths);
            Collections.sort(groups, new Comparator<>() {
                @Override
                public int compare(ProjectGroup o1, ProjectGroup o2) {
                    int ind1 = getGroupIndex(o1);
                    int ind2 = getGroupIndex(o2);
                    return ind1 == ind2 ? StringUtil.naturalCompare(o1.getName(), o2.getName()) : ind1 - ind2;
                }

                private int getGroupIndex(ProjectGroup group) {
                    int index = -1;
                    for (String path : group.getProjects()) {
                        final int i = projectPaths.indexOf(path);
                        if (index >= 0 && index > i) {
                            index = i;
                        }
                    }
                    return index;
                }
            });

            for (ProjectGroup group : groups) {
                paths.removeAll(group.getProjects());
            }

            for (ProjectGroup group : groups) {
                final List<AnAction> children = new ArrayList<>();
                for (String path : group.getProjects()) {
                    final AnAction action = createOpenAction(path, duplicates);
                    if (action != null) {
                        children.add(action);

                        if (forMainMenu && children.size() >= MAX_PROJECTS_IN_MAIN_MENU) {
                            break;
                        }
                    }
                }
                actions.add(new ProjectGroupActionGroup(group, children));
                if (group.isExpanded()) {
                    for (AnAction child : children) {
                        actions.add(child);
                    }
                }
            }
        }

        for (final String path : paths) {
            final AnAction action = createOpenAction(path, duplicates);
            if (action != null) {
                actions.add(action);
            }
        }

        if (actions.isEmpty()) {
            return AnAction.EMPTY_ARRAY;
        }

        return actions.toArray(new AnAction[actions.size()]);
    }

    @Nullable
    private AnAction createOpenAction(String path, Set<String> duplicates) {
        String projectName = getProjectName(path);
        String displayName;
        RecentProjectMetaInfo metaInfo;
        IdeFrameState state;

        synchronized (myStateLock) {
            displayName = myState.names.get(path);
            state = myState.frameStates.get(path);
            metaInfo = myState.additionalInfo.get(path);
        }

        if (StringUtil.isEmptyOrSpaces(displayName)) {
            displayName = duplicates.contains(path) ? path : projectName;
        }

        List<String> extensions = List.of();
        if (metaInfo != null) {
            extensions = new ArrayList<>(metaInfo.extensions);
        }

        // It's better don't to remove non-existent projects. Sometimes projects stored
        // on USB-sticks or flash-cards, and it will be nice to have them in the list
        // when USB device or SD-card is mounted
        //if (new File(path).exists()) {
        return new ReopenProjectAction(path, projectName, displayName, extensions, state);
        //}
        //return null;
    }

    @RequiredReadAction
    private void markPathRecent(String path, @Nullable Project project) {
        synchronized (myStateLock) {
            if (path.endsWith(File.separator)) {
                path = path.substring(0, path.length() - File.separator.length());
            }
            myState.lastPath = path;
            ProjectGroup group = getProjectGroup(path);
            removePath(path);
            myState.recentPaths.add(0, path);
            if (group != null) {
                List<String> projects = group.getProjects();
                projects.add(0, path);
                group.save(projects);
            }
            myState.additionalInfo.remove(path);
            if (project != null) {
                myState.additionalInfo.put(path, RecentProjectMetaInfo.create(project));
            }
        }
    }

    @Nullable
    private ProjectGroup getProjectGroup(String path) {
        if (path == null) {
            return null;
        }
        for (ProjectGroup group : myState.groups) {
            if (group.getProjects().contains(path)) {
                return group;
            }
        }
        return null;
    }

    @Nullable
    private String getProjectPath(@Nonnull Project project) {
        final VirtualFile baseDirVFile = project.getBaseDir();
        return baseDirVFile != null ? FileUtil.toSystemDependentName(baseDirVFile.getPath()) : null;
    }

    public static boolean isValidProjectPath(String projectPath) {
        final File file = new File(projectPath);
        return file.exists() && (!file.isDirectory() || new File(file, Project.DIRECTORY_STORE_FOLDER).exists());
    }

    private class MyProjectListener implements ProjectManagerListener {
        @Override
        @RequiredReadAction
        public void projectOpened(@Nonnull final Project project, @Nonnull UIAccess uiAccess) {
            String path = getProjectPath(project);
            if (path != null) {
                markPathRecent(path, project);
            }
            SystemDock.getInstance().updateMenu();

            project.getMessageBus().connect().subscribe(ModuleRootListener.class, new ModuleRootListener() {
                @Override
                @RequiredReadAction
                public void rootsChanged(ModuleRootEvent event) {
                    updateProjectModuleExtensions(project);
                }
            });
        }

        @Override
        public void projectClosing(@Nonnull Project project) {
            synchronized (myStateLock) {
                String projectPath = getProjectPath(project);

                myState.names.put(projectPath, getProjectDisplayName(project));

                IdeFrame frame = WindowManager.getInstance().getIdeFrame(project);
                if (frame != null) {
                    IdeFrameState frameState = frame.getFrameState();
                    if (!IdeFrameState.EMPTY.equals(frameState)) {
                        myState.frameStates.put(projectPath, frameState);
                    }
                }
            }
        }

        @Override
        @RequiredReadAction
        public void projectClosed(@Nonnull final Project project, @Nonnull UIAccess uiAccess) {
            Project[] openProjects = ProjectManager.getInstance().getOpenProjects();
            if (openProjects.length > 0) {
                String path = getProjectPath(openProjects[openProjects.length - 1]);
                if (path != null) {
                    markPathRecent(path, null);
                }
            }
            SystemDock.getInstance().updateMenu();
        }
    }

    @Nonnull
    public String getProjectName(@Nonnull String path) {
        return ProjectStoreImpl.readProjectName(new File(path));
    }

    @Override
    @RequiredReadAction
    public void updateProjectModuleExtensions(@Nonnull Project project) {
        String projectPath = getProjectPath(project);
        if (projectPath == null) {
            return;
        }

        synchronized (myStateLock) {
            myState.additionalInfo.put(projectPath, RecentProjectMetaInfo.create(project));
        }
    }

    public boolean willReopenProjectOnStart() {
        return GeneralSettings.getInstance().isReopenLastProject() && getLastProjectPath() != null;
    }

    @RequiredUIAccess
    public void doReopenLastProject() {
        GeneralSettings generalSettings = GeneralSettings.getInstance();
        if (generalSettings.isReopenLastProject()) {
            Set<String> openPaths;
            Map<String, IdeFrameState> states = new HashMap<>();
            boolean forceNewFrame = true;
            synchronized (myStateLock) {
                openPaths = new LinkedHashSet<>(myState.openPaths);
                if (openPaths.isEmpty()) {
                    openPaths = ContainerUtil.createMaybeSingletonSet(myState.lastPath);
                    forceNewFrame = false;
                }

                for (String openPath : openPaths) {
                    IdeFrameState state = myState.frameStates.get(openPath);
                    if (state != null) {
                        states.put(openPath, state);
                    }
                }
            }

            for (String openPath : openPaths) {
                if (isValidProjectPath(openPath)) {
                    ProjectOpenContext context = new ProjectOpenContext();
                    IdeFrameState state = states.get(openPath);
                    if (state != null) {
                        context.putUserData(IdeFrameState.KEY, state);
                    }

                    ProjectImplUtil.openAsync(openPath, null, forceNewFrame, UIAccess.current(), context);
                }
            }
        }
    }

    @Override
    public List<ProjectGroup> getGroups() {
        return Collections.unmodifiableList(myState.groups);
    }

    @Override
    public void addGroup(ProjectGroup group) {
        if (!myState.groups.contains(group)) {
            myState.groups.add(group);
        }
    }

    @Override
    public void removeGroup(ProjectGroup group) {
        myState.groups.remove(group);
    }

    private class MyAppLifecycleListener implements AppLifecycleListener {
        @Override
        public void projectFrameClosed() {
            updateLastProjectPath();
        }

        @Override
        public void projectOpenFailed() {
            updateLastProjectPath();
        }

        @Override
        public void appClosing() {
            updateLastProjectPath();
        }
    }

    public static class RecentProjectMetaInfo {
        public Set<String> extensions = new HashSet<>();

        @RequiredReadAction
        public static RecentProjectMetaInfo create(@Nonnull Project project) {
            RecentProjectMetaInfo info = new RecentProjectMetaInfo();

            ModuleManager moduleManager = ModuleManager.getInstance(project);
            for (Module module : moduleManager.getModules()) {
                VirtualFile moduleDir = module.getModuleDir();
                if (Comparing.equal(project.getBaseDir(), moduleDir)) {
                    ModuleRootManager manager = ModuleRootManager.getInstance(module);

                    List<ModuleExtension> extensions = manager.getExtensions();
                    for (ModuleExtension extension : extensions) {
                        ModuleExtensionProvider provider = ModuleExtensionProvider.findProvider(extension.getId());
                        assert provider != null;

                        if (provider.getParentId() == null) {
                            info.extensions.add(provider.getId());
                        }
                    }
                }
            }
            return info;
        }
    }
}
