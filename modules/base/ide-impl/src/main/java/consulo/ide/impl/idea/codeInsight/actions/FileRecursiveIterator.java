// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.actions;

import consulo.application.ReadAction;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectRootManager;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.internal.CompactVirtualFileSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

class FileRecursiveIterator {
    
    private final Project myProject;
    
    private final Collection<? extends VirtualFile> myRoots;

    FileRecursiveIterator(Project project, List<? extends PsiFile> roots) {
        this(project, ContainerUtil.<PsiFile, VirtualFile>map(roots, PsiFile::getVirtualFile));
    }

    FileRecursiveIterator(Module module) {
        this(
            module.getProject(),
            ContainerUtil.<PsiDirectory, VirtualFile>map(collectModuleDirectories(module), PsiDirectory::getVirtualFile)
        );
    }

    FileRecursiveIterator(Project project) {
        this(project, ContainerUtil.<PsiDirectory, VirtualFile>map(collectProjectDirectories(project), PsiDirectory::getVirtualFile));
    }

    FileRecursiveIterator(PsiDirectory directory) {
        this(directory.getProject(), Collections.singletonList(directory.getVirtualFile()));
    }

    FileRecursiveIterator(Project project, Collection<? extends VirtualFile> roots) {
        myProject = project;
        myRoots = roots;
    }

    
    static List<PsiDirectory> collectProjectDirectories(Project project) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        List<PsiDirectory> directories = new ArrayList<>(modules.length * 3);
        for (Module module : modules) {
            directories.addAll(collectModuleDirectories(module));
        }

        return directories;
    }

    boolean processAll(Predicate<? super PsiFile> processor) {
        CompactVirtualFileSet visited = new CompactVirtualFileSet();
        for (VirtualFile root : myRoots) {
            if (!ProjectRootManager.getInstance(myProject).getFileIndex().iterateContentUnderDirectory(
                root,
                fileOrDir -> {
                    if (fileOrDir.isDirectory() || !visited.add(fileOrDir)) {
                        return true;
                    }
                    PsiFile psiFile =
                        ReadAction.compute(() -> myProject.isDisposed() ? null : PsiManager.getInstance(myProject).findFile(fileOrDir));
                    return psiFile == null || processor.test(psiFile);
                }
            )) {
                return false;
            }
        }
        return true;
    }

    
    static List<PsiDirectory> collectModuleDirectories(Module module) {
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
        return ReadAction.compute(() -> ContainerUtil.mapNotNull(
            contentRoots,
            root -> PsiManager.getInstance(module.getProject()).findDirectory(root)
        ));
    }
}
