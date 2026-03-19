/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.psi.path;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiFileSystemItem;
import consulo.language.psi.PsiManager;
import consulo.module.Module;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;

/**
 * @author peter
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class FileReferenceHelper {
    public static final ExtensionPointName<FileReferenceHelper> EP_NAME = ExtensionPointName.create(FileReferenceHelper.class);

    
    public String trimUrl(String url) {
        return url;
    }

    // FIXME use consulo.language.editor.inspection.PsiReferenceLocalQuickFixProvider
    //
    //public List<? extends LocalQuickFix> registerFixes(FileReference reference) {
    //  return Collections.emptyList();
    //}

    @Nullable
    @RequiredReadAction
    public PsiFileSystemItem getPsiFileSystemItem(Project project, VirtualFile file) {
        PsiManager psiManager = PsiManager.getInstance(project);
        return getPsiFileSystemItem(psiManager, file);
    }

    @RequiredReadAction
    public static PsiFileSystemItem getPsiFileSystemItem(PsiManager psiManager, VirtualFile file) {
        return file.isDirectory() ? psiManager.findDirectory(file) : psiManager.findFile(file);
    }

    public @Nullable PsiFileSystemItem findRoot(Project project, VirtualFile file) {
        return null;
    }

    
    public Collection<PsiFileSystemItem> getRoots(Module module) {
        return Collections.emptyList();
    }

    
    public abstract Collection<PsiFileSystemItem> getContexts(Project project, VirtualFile file);

    public abstract boolean isMine(Project project, VirtualFile file);

    public boolean isFallback() {
        return false;
    }
}
