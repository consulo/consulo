/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.application.ReadAction;
import consulo.compiler.util.ExportableUserDataHolderBase;
import consulo.module.Module;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;

import jakarta.annotation.Nonnull;

import java.util.*;

/**
 * @author Eugene Zhuravlev
 * @author 2003-01-20
 */
public class FileSetCompileScope extends ExportableUserDataHolderBase implements CompileScope {
    private final Set<VirtualFile> myRootFiles = new HashSet<>();
    private final Set<String> myDirectoryUrls = new HashSet<>();
    private Set<String> myUrls = null; // urls caching
    private final Module[] myAffectedModules;
    private final boolean myIncludeTestScope;

    public FileSetCompileScope(Collection<VirtualFile> files, Module[] modules) {
        this(files, modules, true);
    }

    public FileSetCompileScope(Collection<VirtualFile> files, Module[] modules, boolean includeTestScope) {
        myAffectedModules = modules;
        myIncludeTestScope = includeTestScope;
        ReadAction.run(() -> {
            for (VirtualFile file : files) {
                assert file != null;
                addFile(file);
            }
        });
    }

    @Override
    public boolean includeTestScope() {
        return myIncludeTestScope;
    }

    @Nonnull
    @Override
    public Module[] getAffectedModules() {
        return myAffectedModules;
    }

    public Collection<VirtualFile> getRootFiles() {
        return Collections.unmodifiableCollection(myRootFiles);
    }

    @Nonnull
    @Override
    public VirtualFile[] getFiles(FileType fileType) {
        List<VirtualFile> files = new ArrayList<>();
        for (Iterator<VirtualFile> it = myRootFiles.iterator(); it.hasNext(); ) {
            VirtualFile file = it.next();
            if (!file.isValid()) {
                it.remove();
                continue;
            }
            if (file.isDirectory()) {
                addRecursively(files, file, fileType);
            }
            else if (fileType == null || fileType.equals(file.getFileType())) {
                files.add(file);
            }
        }
        return VirtualFileUtil.toVirtualFileArray(files);
    }

    @Override
    public boolean belongs(String url) {
        //url = CompilerUtil.normalizePath(url, '/');
        if (getUrls().contains(url)) {
            return true;
        }
        for (String directoryUrl : myDirectoryUrls) {
            if (FileUtil.startsWith(url, directoryUrl)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> getUrls() {
        if (myUrls == null) {
            myUrls = new HashSet<>();
            for (VirtualFile file : myRootFiles) {
                String url = file.getUrl();
                myUrls.add(url);
            }
        }
        return myUrls;
    }

    private void addFile(VirtualFile file) {
        if (file.isDirectory()) {
            myDirectoryUrls.add(file.getUrl() + "/");
        }
        myRootFiles.add(file);
        myUrls = null;
    }

    private static void addRecursively(final Collection<VirtualFile> container, VirtualFile fromDirectory, final FileType fileType) {
        VirtualFileUtil.visitChildrenRecursively(fromDirectory, new VirtualFileVisitor(VirtualFileVisitor.SKIP_ROOT) {
            @Override
            public boolean visitFile(@Nonnull VirtualFile child) {
                if (!child.isDirectory() && (fileType == null || fileType.equals(child.getFileType()))) {
                    container.add(child);
                }
                return true;
            }
        });
    }
}
