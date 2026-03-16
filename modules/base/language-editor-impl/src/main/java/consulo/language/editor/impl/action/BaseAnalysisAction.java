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
package consulo.language.editor.impl.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.HelpManager;
import consulo.application.concurrent.coroutine.OptionalReadLock;
import consulo.dataContext.DataContext;
import consulo.document.FileDocumentManager;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.editor.scope.localize.AnalysisScopeLocalize;
import consulo.language.editor.ui.awt.scope.BaseAnalysisActionDialog;
import consulo.language.editor.ui.scope.AnalysisUIOptions;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.util.ModuleUtilCore;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.layout.VerticalLayout;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.archive.ArchiveFileType;
import consulo.virtualFileSystem.archive.ArchiveVfsUtil;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.util.HashSet;
import java.util.Set;

public abstract class BaseAnalysisAction extends AnAction {
    private final LocalizeValue myTitle;
    private final LocalizeValue myAnalysisNoon;
    private static final Logger LOG = Logger.getInstance(BaseAnalysisAction.class);

    protected BaseAnalysisAction(
        LocalizeValue text,
        LocalizeValue description,
        LocalizeValue title,
        LocalizeValue analysisNoon
    ) {
        super(text, description);
        myTitle = title;
        myAnalysisNoon = analysisNoon;
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent event) {
        return OptionalReadLock.apply(o -> {
            Presentation presentation = event.getPresentation();
            DataContext dataContext = event.getDataContext();
            Project project = event.getData(Project.KEY);
            boolean dumbMode = project == null || DumbService.getInstance(project).isDumb();
            presentation.setEnabled(!dumbMode && getInspectionScope(dataContext) != null);
            return null;
        }, () -> event.getPresentation().setEnabled(false)).toCoroutine();
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        DataContext dataContext = e.getDataContext();
        final Project project = e.getRequiredData(Project.KEY);
        final Module module = e.getData(Module.KEY);
        AnalysisScope scope = getInspectionScope(dataContext);
        LOG.assertTrue(scope != null);
        final boolean rememberScope = e.getPlace().equals(ActionPlaces.MAIN_MENU);
        AnalysisUIOptions uiOptions = AnalysisUIOptions.getInstance(project);
        PsiElement element = dataContext.getData(PsiElement.KEY);
        BaseAnalysisActionDialog dlg = new BaseAnalysisActionDialog(
            AnalysisScopeLocalize.specifyAnalysisScope(myTitle).get(),
            AnalysisScopeLocalize.analysisScopeTitle(myAnalysisNoon).get(),
            project,
            scope,
            module != null ? ModuleUtilCore.getModuleNameInReadAction(module) : null,
            rememberScope,
            AnalysisUIOptions.getInstance(project),
            element
        ) {
            @Override
            @RequiredUIAccess
            protected void extendMainLayout(VerticalLayout layout, Project project) {
                BaseAnalysisAction.this.extendMainLayout(this, layout, project);
            }

            @Override
            @RequiredUIAccess
            protected void doHelpAction() {
                HelpManager.getInstance().invokeHelp(getHelpTopic());
            }

            @Override
            protected Action[] createActions() {
                return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
            }
        };
        dlg.show();
        if (!dlg.isOK()) {
            canceled();
            return;
        }
        int oldScopeType = uiOptions.SCOPE_TYPE;
        scope = dlg.getScope(scope);
        if (!rememberScope) {
            uiOptions.SCOPE_TYPE = oldScopeType;
        }
        uiOptions.ANALYZE_TEST_SOURCES = dlg.isInspectTestSources();
        FileDocumentManager.getInstance().saveAllDocuments(UIAccess.current());

        analyze(project, scope);
    }

    protected String getHelpTopic() {
        return "reference.dialogs.analyzeDependencies.scope";
    }

    protected void canceled() {
    }

    protected abstract void analyze(Project project, AnalysisScope scope);

    @Nullable
    @RequiredReadAction
    private AnalysisScope getInspectionScope(DataContext dataContext) {
        if (!dataContext.hasData(Project.KEY)) {
            return null;
        }

        AnalysisScope scope = getInspectionScopeImpl(dataContext);

        return scope != null && scope.getScopeType() != AnalysisScope.INVALID ? scope : null;
    }

    @Nullable
    @RequiredReadAction
    private AnalysisScope getInspectionScopeImpl(DataContext dataContext) {
        //Possible scopes: file, directory, package, project, module.
        Project projectContext = dataContext.getData(PlatformDataKeys.PROJECT_CONTEXT);
        if (projectContext != null) {
            return new AnalysisScope(projectContext);
        }

        AnalysisScope analysisScope = dataContext.getData(AnalysisScope.KEY);
        if (analysisScope != null) {
            return analysisScope;
        }

        PsiFile psiFile = dataContext.getData(PsiFile.KEY);
        if (psiFile != null && psiFile.getManager().isInProject(psiFile)) {
            VirtualFile file = psiFile.getVirtualFile();
            if (file != null && file.isValid() && file.getFileType() instanceof ArchiveFileType && acceptNonProjectDirectories()) {
                VirtualFile jarRoot = ArchiveVfsUtil.getArchiveRootForLocalFile(file);
                if (jarRoot != null) {
                    PsiDirectory psiDirectory = psiFile.getManager().findDirectory(jarRoot);
                    if (psiDirectory != null) {
                        return new AnalysisScope(psiDirectory);
                    }
                }
            }
            return new AnalysisScope(psiFile);
        }

        VirtualFile[] virtualFiles = dataContext.getData(VirtualFile.KEY_OF_ARRAY);
        Project project = dataContext.getData(Project.KEY);
        if (virtualFiles != null && project != null) { //analyze on selection
            ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
            if (virtualFiles.length == 1) {
                PsiDirectory psiDirectory = PsiManager.getInstance(project).findDirectory(virtualFiles[0]);
                if (psiDirectory != null && (acceptNonProjectDirectories() || psiDirectory.getManager().isInProject(psiDirectory))) {
                    return new AnalysisScope(psiDirectory);
                }
            }
            Set<VirtualFile> files = new HashSet<>();
            for (VirtualFile vFile : virtualFiles) {
                if (fileIndex.isInContent(vFile)) {
                    files.add(vFile);
                }
            }
            return new AnalysisScope(project, files);
        }

        Module moduleContext = dataContext.getData(LangDataKeys.MODULE_CONTEXT);
        if (moduleContext != null) {
            return new AnalysisScope(moduleContext);
        }

        Module[] modulesArray = dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
        if (modulesArray != null) {
            return new AnalysisScope(modulesArray);
        }
        return project == null ? null : new AnalysisScope(project);
    }

    protected boolean acceptNonProjectDirectories() {
        return false;
    }

    @RequiredUIAccess
    protected void extendMainLayout(BaseAnalysisActionDialog dialog, VerticalLayout layout, Project project) {
    }
}
