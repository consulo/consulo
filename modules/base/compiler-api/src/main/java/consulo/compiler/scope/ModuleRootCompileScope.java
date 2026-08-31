/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.scope;

import consulo.annotation.access.RequiredReadAction;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.FileIndex;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class is similar to {@link ModuleCompileScope} with one difference: it doesn't support source roots.
 * Compilation works from module root.
 *
 * @author VISTALL
 * @since 2014-08-17
 */
public class ModuleRootCompileScope extends FileIndexCompileScope {
    private final Project myProject;
    private final Set<Module> myScopeModules;
    private final Module[] myModules;

    @RequiredReadAction
    public ModuleRootCompileScope(Module module, boolean includeDependentModules) {
        myProject = module.getProject();
        myScopeModules = new HashSet<>();
        if (includeDependentModules) {
            buildScopeModulesSet(module);
        }
        else {
            myScopeModules.add(module);
        }
        myModules = ModuleManager.getInstance(myProject).getModules();
    }

    @RequiredReadAction
    public ModuleRootCompileScope(Project project, Module[] modules, boolean includeDependentModules) {
        myProject = project;
        myScopeModules = new HashSet<>();
        for (Module module : modules) {
            if (module == null) {
                continue; // prevent NPE
            }
            if (includeDependentModules) {
                buildScopeModulesSet(module);
            }
            else {
                myScopeModules.add(module);
            }
        }
        myModules = ModuleManager.getInstance(myProject).getModules();
    }

    private void buildScopeModulesSet(Module module) {
        myScopeModules.add(module);
        Module[] dependencies = ModuleRootManager.getInstance(module).getDependencies();
        for (Module dependency : dependencies) {
            if (!myScopeModules.contains(dependency)) { // may be in case of module circular dependencies
                buildScopeModulesSet(dependency);
            }
        }
    }

    @Override
    
    public Module[] getAffectedModules() {
        return myScopeModules.toArray(new Module[myScopeModules.size()]);
    }

    @Override
    protected FileIndex[] getFileIndices() {
        FileIndex[] indices = new FileIndex[myScopeModules.size()];
        int idx = 0;
        for (Module module : myScopeModules) {
            indices[idx++] = ModuleRootManager.getInstance(module).getFileIndex();
        }
        return indices;
    }

    @Override
    public Collection<Path> getFiles(FileType fileType) {
        List<Path> files = new ArrayList<>();
        FileIndex[] fileIndices = getFileIndices();
        for (FileIndex fileIndex : fileIndices) {
            fileIndex.iterateContent(new ModuleRootCompilerContentIterator(fileType, files));
        }
        return files;
    }

    @Override
    public boolean belongs(Path path) {
        if (myScopeModules.isEmpty()) {
            return false; // optimization
        }
        String filePath = FileUtil.toSystemIndependentName(path.toString());
        Module candidateModule = null;
        int maxRootLength = 0;
        ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
        for (Module module : myModules) {
            String[] contentRootUrls = getModuleContentUrls(module);
            for (String contentRootUrl : contentRootUrls) {
                String contentRootPath = VirtualFileUtil.urlToPath(contentRootUrl);
                if (contentRootPath.length() < maxRootLength) {
                    continue;
                }
                if (!isPathUnderRoot(filePath, contentRootPath)) {
                    continue;
                }
                if (contentRootPath.length() == maxRootLength) {
                    if (candidateModule == null) {
                        candidateModule = module;
                    }
                    else if (!candidateModule.equals(module)) {
                        // the same content root exists in several modules
                        VirtualFile contentRootFile = LocalFileSystem.getInstance().findFileByPath(contentRootPath);
                        candidateModule = contentRootFile != null ? projectFileIndex.getModuleForFile(contentRootFile) : null;
                    }
                }
                else {
                    maxRootLength = contentRootPath.length();
                    candidateModule = module;
                }
            }
        }

        if (candidateModule != null && myScopeModules.contains(candidateModule)) {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(candidateModule);
            String[] excludeRootUrls = moduleRootManager.getContentFolderUrls(ContentFolderTypeProvider.onlyExcluded());
            for (String excludeRootUrl : excludeRootUrls) {
                if (isPathUnderRoot(filePath, VirtualFileUtil.urlToPath(excludeRootUrl))) {
                    return false;
                }
            }
            for (String sourceRootUrl : getModuleContentUrls(candidateModule)) {
                if (isPathUnderRoot(filePath, VirtualFileUtil.urlToPath(sourceRootUrl))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isPathUnderRoot(String path, String root) {
        return (path.length() > root.length()) && path.charAt(root.length()) == '/' && FileUtil.startsWith(path, root);
    }

    private String[] getModuleContentUrls(Module module) {
        return new String[]{module.getModuleDirUrl()};
    }
}
