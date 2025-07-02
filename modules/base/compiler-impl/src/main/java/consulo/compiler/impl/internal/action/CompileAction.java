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
package consulo.compiler.impl.internal.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.compiler.CompilerManager;
import consulo.compiler.action.CompileActionBase;
import consulo.compiler.artifact.Artifact;
import consulo.compiler.artifact.internal.ArtifactBySourceFileFinder;
import consulo.compiler.localize.CompilerLocalize;
import consulo.compiler.resourceCompiler.ResourceCompilerConfiguration;
import consulo.dataContext.DataContext;
import consulo.language.editor.LangDataKeys;
import consulo.language.psi.*;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static consulo.ui.ex.action.Presentation.NO_MNEMONIC;

@ActionImpl(id = "Compile")
public class CompileAction extends CompileActionBase {
    @Override
    @RequiredUIAccess
    protected void doAction(DataContext dataContext, Project project) {
        Module module = dataContext.getData(LangDataKeys.MODULE_CONTEXT);
        if (module != null) {
            CompilerManager.getInstance(project).compile(module, null);
        }
        else {
            VirtualFile[] files = getCompilableFiles(project, dataContext.getData(VirtualFile.KEY_OF_ARRAY));
            if (files.length > 0) {
                CompilerManager.getInstance(project).compile(files, null);
            }
        }
    }

    @Override
    @RequiredReadAction
    public void update(@Nonnull AnActionEvent event) {
        super.update(event);
        Presentation presentation = event.getPresentation();
        if (!presentation.isEnabled()) {
            return;
        }
        DataContext dataContext = event.getDataContext();

        presentation.setTextValue(ActionLocalize.actionCompileText().map(NO_MNEMONIC));
        presentation.setEnabled(true);
        presentation.setVisible(true);

        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }

        Module module = dataContext.getData(LangDataKeys.MODULE_CONTEXT);

        VirtualFile[] files = getCompilableFiles(project, dataContext.getData(VirtualFile.KEY_OF_ARRAY));
        if (module == null && files.length == 0) {
            presentation.setEnabled(false);
            presentation.setVisible(!ActionPlaces.isPopupPlace(event.getPlace()));
            return;
        }

        if (module != null) {
            presentation.setTextValue(CompilerLocalize.actionCompileModuleText(trimName(module.getName())));
        }
        else {
            PsiPackage aPackage = null;
            if (files.length == 1) {
                PsiDirectory directory = PsiManager.getInstance(project).findDirectory(files[0]);
                if (directory != null) {
                    aPackage = PsiPackageManager.getInstance(project).findAnyPackage(directory);
                }
            }
            else {
                PsiElement element = dataContext.getData(PsiElement.KEY);
                if (element instanceof PsiPackage psiPackage) {
                    aPackage = psiPackage;
                }
            }

            if (aPackage != null) {
                String name = aPackage.getQualifiedName();
                presentation.setTextValue(
                    StringUtil.isNotEmpty(name)
                        ? CompilerLocalize.actionCompile0Text(trimName(name))
                        : CompilerLocalize.actionCompileDefaultText()
                );
            }
            else if (files.length == 1) {
                VirtualFile file = files[0];
                FileType fileType = file.getFileType();
                if (CompilerManager.getInstance(project).isCompilableFileType(fileType) || isCompilableResourceFile(project, file)) {
                    presentation.setTextValue(CompilerLocalize.actionCompile0Text(trimName(file.getName())));
                }
                else {
                    presentation.setEnabled(false);
                    // the action should be invisible in popups for non-java files
                    presentation.setVisible(ActionPlaces.MAIN_MENU.equals(event.getPlace()));
                }
            }
            else {
                presentation.setTextValue(CompilerLocalize.actionCompileSelectedFilesText());
            }
        }
    }

    @RequiredReadAction
    private static VirtualFile[] getCompilableFiles(Project project, VirtualFile[] files) {
        if (files == null || files.length == 0) {
            return VirtualFile.EMPTY_ARRAY;
        }
        PsiManager psiManager = PsiManager.getInstance(project);
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
        CompilerManager compilerManager = CompilerManager.getInstance(project);
        List<VirtualFile> filesToCompile = new ArrayList<>();
        for (VirtualFile file : files) {
            if (!fileIndex.isInSourceContent(file)) {
                continue;
            }
            if (!file.isInLocalFileSystem()) {
                continue;
            }
            if (file.isDirectory()) {
                PsiDirectory directory = psiManager.findDirectory(file);
                if (directory == null || PsiPackageManager.getInstance(project).findAnyPackage(directory) == null) {
                    continue;
                }
            }
            else {
                FileType fileType = file.getFileType();
                if (!(compilerManager.isCompilableFileType(fileType) || isCompilableResourceFile(project, file))) {
                    continue;
                }
            }
            filesToCompile.add(file);
        }
        return VirtualFileUtil.toVirtualFileArray(filesToCompile);
    }

    private static String trimName(String name) {
        int length = name.length();
        return length > 23 ? '…' + name.substring(length - 20, length) : name;
    }

    private static boolean isCompilableResourceFile(Project project, VirtualFile file) {
        if (!ResourceCompilerConfiguration.getInstance(project).isResourceFile(file)) {
            return false;
        }
        Collection<? extends Artifact> artifacts = ArtifactBySourceFileFinder.getInstance(project).findArtifacts(file);
        return artifacts.isEmpty();
    }
}