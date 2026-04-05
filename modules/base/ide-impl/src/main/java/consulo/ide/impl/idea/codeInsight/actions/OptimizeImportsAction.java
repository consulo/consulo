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
package consulo.ide.impl.idea.codeInsight.actions;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.EditorSettingsExternalizable;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.refactoring.ImportOptimizer;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FormatChangedTextUtil;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.util.*;

@ActionImpl(id = "OptimizeImports")
public class OptimizeImportsAction extends AnAction {
    private static final String HELP_ID = "editing.manageImports";

    private static boolean myProcessVcsChangedFilesInTests;

    private final Application myApplication;

    @Inject
    public OptimizeImportsAction(Application application) {
        myApplication = application;
        List<ImportOptimizer> extensions = application.getExtensionPoint(ImportOptimizer.class).getExtensionList();

        updatePresentation(getTemplatePresentation(), extensions);
    }

    private void updatePresentation(Presentation presentation, List<ImportOptimizer> importOptimizers) {
        Set<LocalizeValue> actionNames = new LinkedHashSet<>();
        Set<LocalizeValue> actionDescriptions = new LinkedHashSet<>();
        for (ImportOptimizer importOptimizer : importOptimizers) {
            actionNames.add(importOptimizer.getActionName());
            actionDescriptions.add(importOptimizer.getActionDescription());
        }

        if (!actionNames.isEmpty() && !actionDescriptions.isEmpty()) {
            presentation.setTextValue(LocalizeValue.join(" | ", actionNames.toArray(LocalizeValue[]::new)));
            presentation.setDescriptionValue(LocalizeValue.join(" | ", actionDescriptions.toArray(LocalizeValue[]::new)));
        }
        else {
            presentation.setTextValue(ActionLocalize.notActionOptimizeimportsText());
            presentation.setDescriptionValue(ActionLocalize.notActionOptimizeimportsDescription());
        }
    }

    @RequiredUIAccess
    private void updatePresentationForFiles(Presentation presentation, boolean enabled, List<PsiFile> files) {
        presentation.setEnabled(enabled);

        List<ImportOptimizer> importOptimizers = new ArrayList<>(files.size());
        for (PsiFile file : files) {
            importOptimizers.addAll(ImportOptimizer.forFile(file));
        }

        updatePresentation(presentation, importOptimizers);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent event) {
        actionPerformedImpl(event.getDataContext());
    }

    @RequiredUIAccess
    public static void actionPerformedImpl(DataContext dataContext) {
        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            return;
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        Editor editor = BaseCodeInsightAction.getInjectedEditor(project, dataContext.getData(Editor.KEY));

        VirtualFile[] files = dataContext.getData(VirtualFile.KEY_OF_ARRAY);

        PsiFile file = null;
        PsiDirectory dir;

        if (editor != null) {
            file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (file == null) {
                return;
            }
            dir = file.getContainingDirectory();
        }
        else if (files != null && ReformatCodeAction.containsAtLeastOneFile(files)) {
            ReadonlyStatusHandler.OperationStatus operationStatus =
                ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(files);
            if (!operationStatus.hasReadonlyFiles()) {
                new OptimizeImportsProcessor(project, ReformatCodeAction.convertToPsiFiles(files, project), null).run();
            }
            return;
        }
        else {
            Project projectContext = dataContext.getData(PlatformDataKeys.PROJECT_CONTEXT);
            Module moduleContext = dataContext.getData(LangDataKeys.MODULE_CONTEXT);

            if (projectContext != null || moduleContext != null) {
                LocalizeValue text;
                boolean hasChanges;
                if (moduleContext != null) {
                    text = CodeInsightLocalize.processScopeModule(moduleContext.getName());
                    hasChanges = FormatChangedTextUtil.hasChanges(moduleContext);
                }
                else {
                    text = CodeInsightLocalize.processScopeProject(StringUtil.notNullize(projectContext.getPresentableUrl()));
                    hasChanges = FormatChangedTextUtil.hasChanges(projectContext);
                }
                Boolean isProcessVcsChangedText = isProcessVcsChangedText(project, text, hasChanges);
                if (isProcessVcsChangedText == null) {
                    return;
                }
                if (moduleContext != null) {
                    OptimizeImportsProcessor processor = new OptimizeImportsProcessor(project, moduleContext);
                    processor.setProcessChangedTextOnly(isProcessVcsChangedText);
                    processor.run();
                }
                else {
                    new OptimizeImportsProcessor(projectContext).run();
                }
                return;
            }

            PsiElement element = dataContext.getData(PsiElement.KEY);
            if (element == null) {
                return;
            }
            if (element instanceof PsiDirectoryContainer directoryContainer) {
                dir = directoryContainer.getDirectories()[0];
            }
            else if (element instanceof PsiDirectory directory) {
                dir = directory;
            }
            else {
                file = element.getContainingFile();
                if (file == null) {
                    return;
                }
                dir = file.getContainingDirectory();
            }
        }

        boolean processDirectory = false;
        boolean processOnlyVcsChangedFiles = false;
        if (!Application.get().isUnitTestMode() && file == null && dir != null) {
            LocalizeValue message = CodeInsightLocalize.processScopeDirectory(StringUtil.notNullize(dir.getName()));
            OptimizeImportsDialog dialog = new OptimizeImportsDialog(project, message, FormatChangedTextUtil.hasChanges(dir));
            dialog.show();
            if (!dialog.isOK()) {
                return;
            }
            processDirectory = true;
            processOnlyVcsChangedFiles = dialog.isProcessOnlyVcsChangedFiles();
        }

        if (processDirectory) {
            new OptimizeImportsProcessor(project, dir, true, processOnlyVcsChangedFiles).run();
        }
        else {
            OptimizeImportsProcessor optimizer = new OptimizeImportsProcessor(project, file);
            if (editor != null
                && EditorSettingsExternalizable.getInstance().getOptions().SHOW_NOTIFICATION_AFTER_OPTIMIZE_IMPORTS_ACTION) {
                optimizer.setCollectInfo(true);
                optimizer.setPostRunnable(() -> {
                    LayoutCodeInfoCollector collector = optimizer.getInfoCollector();
                    if (collector != null) {
                        LocalizeValue info = collector.getOptimizeImportsNotification();
                        if (!editor.isDisposed() && editor.getComponent().isShowing()) {
                            LocalizeValue message = info.orIfEmpty(CodeInsightLocalize.notificationTextUnusedImportsNotFound());
                            FileInEditorProcessor.showHint(editor, message, null);
                        }
                    }
                });
            }
            optimizer.run();
        }
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        if (!myApplication.getExtensionPoint(ImportOptimizer.class).hasAnyExtensions()) {
            presentation.setVisible(false);
            return;
        }

        DataContext dataContext = event.getDataContext();
        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            updatePresentationForFiles(presentation, false, Collections.emptyList());
            return;
        }

        VirtualFile[] files = dataContext.getData(VirtualFile.KEY_OF_ARRAY);
        List<PsiFile> psiFiles = new ArrayList<>();

        Editor editor = BaseCodeInsightAction.getInjectedEditor(project, dataContext.getData(Editor.KEY), false);
        if (editor != null) {
            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (file == null || !isOptimizeImportsAvailable(file)) {
                updatePresentationForFiles(presentation, false, Collections.emptyList());
                return;
            }
            else {
                psiFiles.add(file);
            }
        }
        else if (files != null && ReformatCodeAction.containsAtLeastOneFile(files)) {
            boolean anyHasOptimizeImports = false;
            for (VirtualFile virtualFile : files) {
                PsiFile file = PsiManager.getInstance(project).findFile(virtualFile);
                if (file == null) {
                    updatePresentationForFiles(presentation, false, Collections.emptyList());
                    return;
                }
                psiFiles.add(file);
                if (isOptimizeImportsAvailable(file)) {
                    anyHasOptimizeImports = true;
                }
            }
            if (!anyHasOptimizeImports) {
                updatePresentationForFiles(presentation, false, psiFiles);
                return;
            }
        }
        else if (files != null && files.length == 1) {
            // skip. Both directories and single files are supported.
        }
        else if (!dataContext.hasData(LangDataKeys.MODULE_CONTEXT)
            && !dataContext.hasData(PlatformDataKeys.PROJECT_CONTEXT)) {
            PsiElement element = dataContext.getData(PsiElement.KEY);
            if (element == null) {
                updatePresentationForFiles(presentation, false, Collections.emptyList());
                return;
            }

            if (!(element instanceof PsiDirectory)) {
                PsiFile file = element.getContainingFile();
                if (file == null || !isOptimizeImportsAvailable(file)) {
                    updatePresentationForFiles(presentation, false, Collections.emptyList());
                    return;
                }
            }
        }

        updatePresentationForFiles(presentation, true, psiFiles);
    }

    @RequiredUIAccess
    private static boolean isOptimizeImportsAvailable(PsiFile file) {
        return !ImportOptimizer.forFile(file).isEmpty();
    }

    @RequiredUIAccess
    private static Boolean isProcessVcsChangedText(Project project, LocalizeValue text, boolean hasChanges) {
        if (Application.get().isUnitTestMode()) {
            return myProcessVcsChangedFilesInTests;
        }

        OptimizeImportsDialog dialog = new OptimizeImportsDialog(project, text, hasChanges);
        if (!dialog.showAndGet()) {
            return null;
        }

        return dialog.isProcessOnlyVcsChangedFiles();
    }

    @TestOnly
    protected static void setProcessVcsChangedFilesInTests(boolean value) {
        myProcessVcsChangedFilesInTests = value;
    }

    private static class OptimizeImportsDialog extends DialogWrapper {
        private final boolean myContextHasChanges;

        private final LocalizeValue myText;
        private JCheckBox myOnlyVcsCheckBox;
        private final LastRunReformatCodeOptionsProvider myLastRunOptions;

        OptimizeImportsDialog(Project project, LocalizeValue text, boolean hasChanges) {
            super(project, false);
            myText = text;
            myContextHasChanges = hasChanges;
            myLastRunOptions = new LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance());
            setOKButtonText(CodeInsightLocalize.reformatCodeAcceptButtonText());
            setTitle(CodeInsightLocalize.processOptimizeImports());
            init();
        }

        public boolean isProcessOnlyVcsChangedFiles() {
            return myOnlyVcsCheckBox.isSelected();
        }

        @Override
        protected @Nullable JComponent createCenterPanel() {
            JPanel panel = new JPanel();
            BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
            panel.setLayout(layout);

            panel.add(new JLabel(myText.get()));
            myOnlyVcsCheckBox = new JCheckBox(CodeInsightLocalize.processScopeChangedFiles().get());
            boolean lastRunVcsChangedTextEnabled = myLastRunOptions.getLastTextRangeType() == TextRangeType.VCS_CHANGED_TEXT;

            myOnlyVcsCheckBox.setEnabled(myContextHasChanges);
            myOnlyVcsCheckBox.setSelected(myContextHasChanges && lastRunVcsChangedTextEnabled);
            myOnlyVcsCheckBox.setBorder(new EmptyBorder(0, 10, 0, 0));
            panel.add(myOnlyVcsCheckBox);
            return panel;
        }

        @Override
        protected @Nullable String getHelpId() {
            return HELP_ID;
        }
    }
}
