/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.runAnything;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.UserHomeFileUtil;
import consulo.ide.internal.RunAnythingContextRecentDirectoryCache;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.internal.ModuleIconService;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * from kotlin
 */
public class RunAnythingContext {
    public static final class ProjectContext extends RunAnythingContext {
        private Project myProject;

        public ProjectContext(Project project) {
            super(IdeLocalize.runAnythingContextProject(), LocalizeValue.ofNullable(project.getBasePath()));
            myProject = project;
        }

        
        public Project getProject() {
            return myProject;
        }

        @Nullable
        @Override
        public String getPath() {
            return myProject.getBasePath();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ProjectContext that = (ProjectContext)o;
            return Objects.equals(myProject, that.myProject);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myProject);
        }
    }

    public static final class BrowseRecentDirectoryContext extends RunAnythingContext {
        public static final BrowseRecentDirectoryContext INSTANCE = new BrowseRecentDirectoryContext();

        public BrowseRecentDirectoryContext() {
            super(IdeLocalize.runAnythingContextBrowseDirectory(), LocalizeValue.empty(), PlatformIconGroup.nodesFolder());
        }
    }

    public static final class RecentDirectoryContext extends RunAnythingContext {
        
        private final String myPath;

        public RecentDirectoryContext(String path) {
            super(
                LocalizeValue.ofNullable(UserHomeFileUtil.getLocationRelativeToUserHome(path)),
                LocalizeValue.empty(),
                PlatformIconGroup.nodesFolder()
            );
            myPath = path;
        }

        
        public String getPath() {
            return myPath;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            RecentDirectoryContext that = (RecentDirectoryContext)o;
            return Objects.equals(myPath, that.myPath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myPath);
        }
    }

    public static final class ModuleContext extends RunAnythingContext {
        private final Module myModule;

        public ModuleContext(Module module) {
            super(
                LocalizeValue.ofNullable(module.getName()),
                LocalizeValue.ofNullable(calcDescription(module)),
                ModuleIconService.getInstance(module.getProject()).getIcon(module)
            );
            myModule = module;
        }

        @Nullable
        @Override
        public String getPath() {
            return myModule.getModuleDirPath();
        }

        
        private static String calcDescription(Module module) {
            String basePath = module.getProject().getBasePath();
            if (basePath != null) {
                String modulePath = module.getModuleDirPath();
                if (modulePath == null) {
                    VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
                    if (contentRoots.length == 1) {
                        modulePath = contentRoots[0].getPath();
                    }
                }

                if (modulePath != null) {
                    String relativePath = FileUtil.getRelativePath(basePath, modulePath, '/');
                    if (relativePath != null) {
                        return relativePath;
                    }
                }
            }
            return "undefined";
        }

        
        public Module getModule() {
            return myModule;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ModuleContext that = (ModuleContext)o;
            return Objects.equals(myModule, that.myModule);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myModule);
        }
    }

    
    protected final LocalizeValue label;
    
    protected final LocalizeValue description;
    protected final Image icon;

    private RunAnythingContext(LocalizeValue label) {
        this(label, LocalizeValue.empty(), null);
    }

    private RunAnythingContext(LocalizeValue label, LocalizeValue description) {
        this(label, description, null);
    }

    private RunAnythingContext(LocalizeValue label, LocalizeValue description, Image icon) {
        this.label = label;
        this.description = description;
        this.icon = icon;
    }

    public @Nullable String getPath() {
        return null;
    }

    
    public LocalizeValue getLabel() {
        return label;
    }

    
    public LocalizeValue getDescription() {
        return description;
    }

    public Image getIcon() {
        return icon;
    }

    @RequiredReadAction
    public static List<RunAnythingContext> allContexts(Project project) {
        List<RunAnythingContext> contexts = projectAndModulesContexts(project);
        contexts.add(RunAnythingContext.BrowseRecentDirectoryContext.INSTANCE);

        List<String> paths = RunAnythingContextRecentDirectoryCache.getInstance(project).getPaths();
        for (String path : paths) {
            contexts.add(new RunAnythingContext.RecentDirectoryContext(path));
        }
        return contexts;
    }

    @RequiredReadAction
    private static List<RunAnythingContext> projectAndModulesContexts(Project project) {
        List<RunAnythingContext> contexts = new ArrayList<>();
        contexts.add(new RunAnythingContext.ProjectContext(project));
        ModuleManager manager = ModuleManager.getInstance(project);
        Module[] modules = manager.getModules();
        if (modules.length == 1) {
            contexts.add(new RunAnythingContext.ModuleContext(modules[0]));
        }
        return contexts;
    }
}
