/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.content.scope.SearchScope;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.find.impl.FindInProjectUtil;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.language.codeStyle.FormattingModelBuilder;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.*;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import consulo.util.lang.function.Predicates;
import consulo.virtualFileSystem.ReadonlyStatusHandler;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;

public class ReformatCodeAction extends AnAction implements DumbAware {
    private static final Logger LOG = Logger.getInstance(ReformatCodeAction.class);

    private static final String HELP_ID = "editing.codeReformatting";
    protected static ReformatFilesOptions myTestOptions;

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            return;
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        Editor editor = dataContext.getData(Editor.KEY);
        VirtualFile[] files = dataContext.getData(VirtualFile.KEY_OF_ARRAY);

        PsiFile file = null;
        PsiDirectory dir = null;
        boolean hasSelection = false;

        if (editor != null) {
            file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (file == null) {
                return;
            }
            dir = file.getContainingDirectory();
            hasSelection = editor.getSelectionModel().hasSelection();
        }
        else if (containsAtLeastOneFile(files)) {
            ReadonlyStatusHandler.OperationStatus operationStatus =
                ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(files);
            if (!operationStatus.hasReadonlyFiles()) {
                ReformatFilesOptions selectedFlags = getReformatFilesOptions(project, files);
                if (selectedFlags == null) {
                    return;
                }

                boolean processOnlyChangedText = selectedFlags.getTextRangeType() == TextRangeType.VCS_CHANGED_TEXT;
                boolean shouldOptimizeImports = selectedFlags.isOptimizeImports() && !DumbService.getInstance(project).isDumb();

                AbstractLayoutCodeProcessor processor =
                    new ReformatCodeProcessor(project, convertToPsiFiles(files, project), null, processOnlyChangedText);
                if (shouldOptimizeImports) {
                    processor = new OptimizeImportsProcessor(processor);
                }
                if (selectedFlags.isRearrangeCode()) {
                    processor = new RearrangeCodeProcessor(processor);
                }

                processor.run();
            }
            return;
        }
        else {
            if (dataContext.getData(PlatformDataKeys.PROJECT_CONTEXT) != null || dataContext.getData(LangDataKeys.MODULE_CONTEXT) != null) {
                Module moduleContext = dataContext.getData(LangDataKeys.MODULE_CONTEXT);
                ReformatFilesOptions selectedFlags = getLayoutProjectOptions(project, moduleContext);
                if (selectedFlags != null) {
                    reformatModule(project, moduleContext, selectedFlags);
                }
                return;
            }
            else {
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
        }

        if (file == null && dir != null) {
            DirectoryFormattingOptions options = getDirectoryFormattingOptions(project, dir);
            if (options != null) {
                reformatDirectory(project, dir, options);
            }
            return;
        }

        if (file == null || editor == null) {
            return;
        }

        LastRunReformatCodeOptionsProvider provider = new LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance());
        ReformatCodeRunOptions currentRunOptions = provider.getLastRunOptions(file);

        TextRangeType processingScope = currentRunOptions.getTextRangeType();
        if (hasSelection) {
            processingScope = TextRangeType.SELECTED_TEXT;
        }
        else if (processingScope == TextRangeType.VCS_CHANGED_TEXT) {
            if (FormatChangedTextUtil.getInstance().isChangeNotTrackedForFile(project, file)) {
                processingScope = TextRangeType.WHOLE_FILE;
            }
        }
        else {
            processingScope = TextRangeType.WHOLE_FILE;
        }

        currentRunOptions.setProcessingScope(processingScope);
        new FileInEditorProcessor(file, editor, currentRunOptions).processCode();
    }

    @Nullable
    @Override
    protected Image getTemplateIcon() {
        return PlatformIconGroup.actionsReformatcode();
    }

    @Nullable
    @RequiredUIAccess
    private static DirectoryFormattingOptions getDirectoryFormattingOptions(
        @Nonnull Project project,
        @Nonnull PsiDirectory dir
    ) {
        LayoutDirectoryDialog dialog = new LayoutDirectoryDialog(
            project,
            CodeInsightLocalize.processReformatCode().get(),
            CodeInsightLocalize.processScopeDirectory(dir.getVirtualFile().getPath()).get(),
            FormatChangedTextUtil.hasChanges(dir)
        );

        boolean enableIncludeDirectoriesCb = dir.getSubdirectories().length > 0;
        dialog.setEnabledIncludeSubdirsCb(enableIncludeDirectoriesCb);
        dialog.setSelectedIncludeSubdirsCb(enableIncludeDirectoriesCb);

        if (dialog.showAndGet()) {
            return dialog;
        }
        return null;
    }

    @RequiredUIAccess
    private static void reformatDirectory(
        @Nonnull Project project,
        @Nonnull PsiDirectory dir,
        @Nonnull DirectoryFormattingOptions options
    ) {
        AbstractLayoutCodeProcessor processor = new ReformatCodeProcessor(
            project, dir, options.isIncludeSubdirectories(), options.getTextRangeType() == TextRangeType.VCS_CHANGED_TEXT
        );

        registerScopeFilter(processor, options.getSearchScope());
        registerFileMaskFilter(processor, options.getFileTypeMask());

        if (options.isOptimizeImports()) {
            processor = new OptimizeImportsProcessor(processor);
        }
        if (options.isRearrangeCode()) {
            processor = new RearrangeCodeProcessor(processor);
        }

        processor.run();
    }

    @RequiredUIAccess
    private static void reformatModule(
        @Nonnull Project project,
        @Nullable Module moduleContext,
        @Nonnull ReformatFilesOptions selectedFlags
    ) {
        boolean shouldOptimizeImports = selectedFlags.isOptimizeImports() && !DumbService.getInstance(project).isDumb();
        boolean processOnlyChangedText = selectedFlags.getTextRangeType() == TextRangeType.VCS_CHANGED_TEXT;

        AbstractLayoutCodeProcessor processor;
        if (moduleContext != null) {
            processor = new ReformatCodeProcessor(project, moduleContext, processOnlyChangedText);
        }
        else {
            processor = new ReformatCodeProcessor(project, processOnlyChangedText);
        }

        registerScopeFilter(processor, selectedFlags.getSearchScope());
        registerFileMaskFilter(processor, selectedFlags.getFileTypeMask());

        if (shouldOptimizeImports) {
            processor = new OptimizeImportsProcessor(processor);
        }

        if (selectedFlags.isRearrangeCode()) {
            processor = new RearrangeCodeProcessor(processor);
        }

        processor.run();
    }

    public static void registerScopeFilter(
        @Nonnull AbstractLayoutCodeProcessor processor,
        @Nullable SearchScope scope
    ) {
        if (scope == null) {
            return;
        }

        processor.addFileFilter(scope::contains);
    }

    public static void registerFileMaskFilter(@Nonnull AbstractLayoutCodeProcessor processor, @Nullable String fileTypeMask) {
        if (fileTypeMask == null) {
            return;
        }

        Predicate<CharSequence> patternCondition = getFileTypeMaskPattern(fileTypeMask);
        processor.addFileFilter(file -> patternCondition.test(file.getNameSequence()));
    }

    private static Predicate<CharSequence> getFileTypeMaskPattern(@Nullable String mask) {
        try {
            return FindInProjectUtil.createFileMaskCondition(mask);
        }
        catch (PatternSyntaxException e) {
            LOG.info("Error while processing file mask: ", e);
            return Predicates.alwaysTrue();
        }
    }

    @RequiredReadAction
    public static PsiFile[] convertToPsiFiles(VirtualFile[] files, Project project) {
        PsiManager manager = PsiManager.getInstance(project);
        ArrayList<PsiFile> result = new ArrayList<>();
        for (VirtualFile virtualFile : files) {
            PsiFile psiFile = manager.findFile(virtualFile);
            if (psiFile != null) {
                result.add(psiFile);
            }
        }
        return PsiUtilCore.toPsiFileArray(result);
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        DataContext dataContext = event.getDataContext();
        Project project = dataContext.getData(Project.KEY);
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }

        Editor editor = dataContext.getData(Editor.KEY);

        VirtualFile[] files = dataContext.getData(VirtualFile.KEY_OF_ARRAY);

        if (editor != null) {
            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (file == null || file.getVirtualFile() == null) {
                presentation.setEnabled(false);
                return;
            }

            if (FormattingModelBuilder.forContext(file) != null) {
                presentation.setEnabled(true);
                return;
            }
        }
        else if (files != null && containsAtLeastOneFile(files)) {
            boolean anyFormatters = false;
            for (VirtualFile virtualFile : files) {
                if (virtualFile.isDirectory()) {
                    presentation.setEnabled(false);
                    return;
                }
                PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                if (psiFile == null) {
                    presentation.setEnabled(false);
                    return;
                }
                FormattingModelBuilder builder = FormattingModelBuilder.forContext(psiFile);
                if (builder != null) {
                    anyFormatters = true;
                }
            }
            if (!anyFormatters) {
                presentation.setEnabled(false);
                return;
            }
        }
        else if (files != null && files.length == 1) {
            // skip. Both directories and single files are supported.
        }
        else {
            if (dataContext.getData(LangDataKeys.MODULE_CONTEXT) == null &&
                dataContext.getData(PlatformDataKeys.PROJECT_CONTEXT) == null) {
                PsiElement element = dataContext.getData(PsiElement.KEY);
                if (element == null) {
                    presentation.setEnabled(false);
                    return;
                }
                if (!(element instanceof PsiDirectory)) {
                    PsiFile file = element.getContainingFile();
                    if (file == null || FormattingModelBuilder.forContext(file) == null) {
                        presentation.setEnabled(false);
                        return;
                    }
                }
            }
        }
        presentation.setEnabled(true);
    }

    @Nullable
    @RequiredUIAccess
    private static ReformatFilesOptions getReformatFilesOptions(@Nonnull Project project, @Nonnull VirtualFile[] files) {
        if (Application.get().isUnitTestMode()) {
            return myTestOptions;
        }
        ReformatFilesDialog dialog = new ReformatFilesDialog(project, files);
        if (!dialog.showAndGet()) {
            return null;
        }
        return dialog;
    }

    @Nullable
    @RequiredUIAccess
    private static ReformatFilesOptions getLayoutProjectOptions(@Nonnull Project project, @Nullable Module module) {
        if (Application.get().isUnitTestMode()) {
            return myTestOptions;
        }

        LocalizeValue text = (module != null)
            ? CodeInsightLocalize.processScopeModule(module.getModuleDir())
            : CodeInsightLocalize.processScopeProject(project.getPresentableUrl());

        boolean enableOnlyVCSChangedRegions = module != null
            ? FormatChangedTextUtil.hasChanges(module)
            : FormatChangedTextUtil.hasChanges(project);

        LayoutProjectCodeDialog dialog = new LayoutProjectCodeDialog(
            project,
            CodeInsightLocalize.processReformatCode().get(),
            text.get(),
            enableOnlyVCSChangedRegions
        );
        return !dialog.showAndGet() ? null : dialog;
    }

    @TestOnly
    protected static void setTestOptions(ReformatFilesOptions options) {
        myTestOptions = options;
    }

    public static boolean containsAtLeastOneFile(VirtualFile[] files) {
        if (files == null) {
            return false;
        }
        if (files.length < 1) {
            return false;
        }
        for (VirtualFile virtualFile : files) {
            if (virtualFile.isDirectory()) {
                return false;
            }
        }
        return true;
    }
}
