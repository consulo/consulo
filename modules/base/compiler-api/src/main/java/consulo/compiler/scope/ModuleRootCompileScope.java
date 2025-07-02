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

import consulo.application.ApplicationManager;
import consulo.content.ContentFolderTypeProvider;
import consulo.content.FileIndex;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 17.08.14
 * <p>
 * This class is looks like {@link ModuleCompileScope} with one different it dont supported source roots, compilation works from module root
 */
public class ModuleRootCompileScope extends FileIndexCompileScope {
    private final Project myProject;
    private final Set<Module> myScopeModules;
    private final Module[] myModules;

    public ModuleRootCompileScope(final Module module, boolean includeDependentModules) {
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

    public ModuleRootCompileScope(Project project, final Module[] modules, boolean includeDependentModules) {
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
        final Module[] dependencies = ModuleRootManager.getInstance(module).getDependencies();
        for (Module dependency : dependencies) {
            if (!myScopeModules.contains(dependency)) { // may be in case of module circular dependencies
                buildScopeModulesSet(dependency);
            }
        }
    }

    @Override
    @Nonnull
    public Module[] getAffectedModules() {
        return myScopeModules.toArray(new Module[myScopeModules.size()]);
    }

    @Override
    protected FileIndex[] getFileIndices() {
        final FileIndex[] indices = new FileIndex[myScopeModules.size()];
        int idx = 0;
        for (final Module module : myScopeModules) {
            indices[idx++] = ModuleRootManager.getInstance(module).getFileIndex();
        }
        return indices;
    }

    @Override
    @Nonnull
    public VirtualFile[] getFiles(final FileType fileType) {
        final List<VirtualFile> files = new ArrayList<>();
        final FileIndex[] fileIndices = getFileIndices();
        for (final FileIndex fileIndex : fileIndices) {
            fileIndex.iterateContent(new ModuleRootCompilerContentIterator(fileType, files));
        }
        return VirtualFileUtil.toVirtualFileArray(files);
    }

    @Override
    public boolean belongs(final String url) {
        if (myScopeModules.isEmpty()) {
            return false; // optimization
        }
        Module candidateModule = null;
        int maxUrlLength = 0;
        final ProjectFileIndex projectFileIndex = ProjectRootManager.getInstance(myProject).getFileIndex();
        for (final Module module : myModules) {
            final String[] contentRootUrls = getModuleContentUrls(module);
            for (final String contentRootUrl : contentRootUrls) {
                if (contentRootUrl.length() < maxUrlLength) {
                    continue;
                }
                if (!isUrlUnderRoot(url, contentRootUrl)) {
                    continue;
                }
                if (contentRootUrl.length() == maxUrlLength) {
                    if (candidateModule == null) {
                        candidateModule = module;
                    }
                    else {
                        // the same content root exists in several modules
                        if (!candidateModule.equals(module)) {
                            candidateModule = ApplicationManager.getApplication().runReadAction(new Supplier<Module>() {
                                @Override
                                public Module get() {
                                    final VirtualFile contentRootFile = VirtualFileManager.getInstance().findFileByUrl(contentRootUrl);
                                    if (contentRootFile != null) {
                                        return projectFileIndex.getModuleForFile(contentRootFile);
                                    }
                                    return null;
                                }
                            });
                        }
                    }
                }
                else {
                    maxUrlLength = contentRootUrl.length();
                    candidateModule = module;
                }
            }
        }

        if (candidateModule != null && myScopeModules.contains(candidateModule)) {
            ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(candidateModule);
            String[] excludeRootUrls = moduleRootManager.getContentFolderUrls(ContentFolderTypeProvider.onlyExcluded());
            for (String excludeRootUrl : excludeRootUrls) {
                if (isUrlUnderRoot(url, excludeRootUrl)) {
                    return false;
                }
            }
            for (String sourceRootUrl : getModuleContentUrls(candidateModule)) {
                if (isUrlUnderRoot(url, sourceRootUrl)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isUrlUnderRoot(final String url, final String root) {
        return (url.length() > root.length()) && url.charAt(root.length()) == '/' && FileUtil.startsWith(url, root);
    }

    private String[] getModuleContentUrls(final Module module) {
        return new String[]{module.getModuleDirUrl()};
    }
}
