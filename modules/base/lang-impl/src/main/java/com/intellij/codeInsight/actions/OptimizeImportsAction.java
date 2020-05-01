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

package com.intellij.codeInsight.actions;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.idea.ActionsBundle;
import com.intellij.lang.ImportOptimizer;
import com.intellij.lang.LanguageImportStatements;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.TestOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.util.*;

public class OptimizeImportsAction extends AnAction {
  private static final String HELP_ID = "editing.manageImports";

  private static final String NO_IMPORTS_OPTIMIZED = "Unused imports not found";
  private static boolean myProcessVcsChangedFilesInTests;

  public OptimizeImportsAction() {
    List<ImportOptimizer> extensions = LanguageImportStatements.INSTANCE.getExtensions();

    updatePresentation(getTemplatePresentation(), extensions);
  }

  private void updatePresentation(Presentation presentation, List<ImportOptimizer> importOptimizers) {
    Set<String> actionNames = new LinkedHashSet<>();
    Set<String> actionDescriptions = new LinkedHashSet<>();
    for (ImportOptimizer importOptimizer : importOptimizers) {
      actionNames.add(importOptimizer.getActionName());
      actionDescriptions.add(importOptimizer.getActionDescription());
    }

    if (!actionNames.isEmpty() && !actionDescriptions.isEmpty()) {
      presentation.setText(StringUtil.join(actionNames, " | "));
      presentation.setDescription(StringUtil.join(actionDescriptions, " | "));
    }
    else {
      presentation.setText(ActionsBundle.message("not.action.OptimizeImports.text"));
      presentation.setDescription(ActionsBundle.message("not.action.OptimizeImports.description"));
    }
  }

  @RequiredUIAccess
  private void updatePresentationForFiles(@Nonnull Presentation presentation, boolean enabled, @Nonnull List<PsiFile> files) {
    presentation.setEnabled(enabled);

    List<ImportOptimizer> importOptimizers = new ArrayList<>(files.size());
    for (PsiFile file : files) {
      importOptimizers.addAll(LanguageImportStatements.INSTANCE.forFile(file));
    }

    updatePresentation(presentation, importOptimizers);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent event) {
    actionPerformedImpl(event.getDataContext());
  }

  @RequiredUIAccess
  public static void actionPerformedImpl(final DataContext dataContext) {
    final Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      return;
    }
    PsiDocumentManager.getInstance(project).commitAllDocuments();
    final Editor editor = BaseCodeInsightAction.getInjectedEditor(project, dataContext.getData(CommonDataKeys.EDITOR));

    final VirtualFile[] files = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);

    PsiFile file = null;
    PsiDirectory dir;

    if (editor != null) {
      file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
      if (file == null) return;
      dir = file.getContainingDirectory();
    }
    else if (files != null && ReformatCodeAction.containsAtLeastOneFile(files)) {
      final ReadonlyStatusHandler.OperationStatus operationStatus = ReadonlyStatusHandler.getInstance(project).ensureFilesWritable(files);
      if (!operationStatus.hasReadonlyFiles()) {
        new OptimizeImportsProcessor(project, ReformatCodeAction.convertToPsiFiles(files, project), null).run();
      }
      return;
    }
    else {
      Project projectContext = dataContext.getData(PlatformDataKeys.PROJECT_CONTEXT);
      Module moduleContext = dataContext.getData(LangDataKeys.MODULE_CONTEXT);

      if (projectContext != null || moduleContext != null) {
        final String text;
        final boolean hasChanges;
        if (moduleContext != null) {
          text = CodeInsightBundle.message("process.scope.module", moduleContext.getName());
          hasChanges = FormatChangedTextUtil.hasChanges(moduleContext);
        }
        else {
          text = CodeInsightBundle.message("process.scope.project", projectContext.getPresentableUrl());
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

      PsiElement element = dataContext.getData(CommonDataKeys.PSI_ELEMENT);
      if (element == null) return;
      if (element instanceof PsiDirectoryContainer) {
        dir = ((PsiDirectoryContainer)element).getDirectories()[0];
      }
      else if (element instanceof PsiDirectory) {
        dir = (PsiDirectory)element;
      }
      else {
        file = element.getContainingFile();
        if (file == null) return;
        dir = file.getContainingDirectory();
      }
    }

    boolean processDirectory = false;
    boolean processOnlyVcsChangedFiles = false;
    if (!ApplicationManager.getApplication().isUnitTestMode() && file == null && dir != null) {
      String message = CodeInsightBundle.message("process.scope.directory", dir.getName());
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
      final OptimizeImportsProcessor optimizer = new OptimizeImportsProcessor(project, file);
      if (editor != null && EditorSettingsExternalizable.getInstance().getOptions().SHOW_NOTIFICATION_AFTER_OPTIMIZE_IMPORTS_ACTION) {
        optimizer.setCollectInfo(true);
        optimizer.setPostRunnable(() -> {
          LayoutCodeInfoCollector collector = optimizer.getInfoCollector();
          if (collector != null) {
            String info = collector.getOptimizeImportsNotification();
            if (!editor.isDisposed() && editor.getComponent().isShowing()) {
              String message = info != null ? info : NO_IMPORTS_OPTIMIZED;
              FileInEditorProcessor.showHint(editor, StringUtil.capitalize(message), null);
            }
          }
        });
      }
      optimizer.run();
    }
  }

  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent event) {
    Presentation presentation = event.getPresentation();
    if (!LanguageImportStatements.INSTANCE.hasAnyExtensions()) {
      presentation.setVisible(false);
      return;
    }

    DataContext dataContext = event.getDataContext();
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    if (project == null) {
      updatePresentationForFiles(presentation, false, Collections.emptyList());
      return;
    }

    final VirtualFile[] files = dataContext.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);
    List<PsiFile> psiFiles = new ArrayList<>();

    final Editor editor = BaseCodeInsightAction.getInjectedEditor(project, dataContext.getData(CommonDataKeys.EDITOR), false);
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
    else {
      if (dataContext.getData(LangDataKeys.MODULE_CONTEXT) == null && dataContext.getData(PlatformDataKeys.PROJECT_CONTEXT) == null) {
        PsiElement element = dataContext.getData(CommonDataKeys.PSI_ELEMENT);
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
    }

    updatePresentationForFiles(presentation, true, psiFiles);
  }

  @RequiredUIAccess
  private static boolean isOptimizeImportsAvailable(final PsiFile file) {
    return !LanguageImportStatements.INSTANCE.forFile(file).isEmpty();
  }

  private static Boolean isProcessVcsChangedText(Project project, String text, boolean hasChanges) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
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

    private final String myText;
    private JCheckBox myOnlyVcsCheckBox;
    private final LastRunReformatCodeOptionsProvider myLastRunOptions;

    OptimizeImportsDialog(Project project, String text, boolean hasChanges) {
      super(project, false);
      myText = text;
      myContextHasChanges = hasChanges;
      myLastRunOptions = new LastRunReformatCodeOptionsProvider(PropertiesComponent.getInstance());
      setOKButtonText(CodeInsightBundle.message("reformat.code.accept.button.text"));
      setTitle(CodeInsightBundle.message("process.optimize.imports"));
      init();
    }

    public boolean isProcessOnlyVcsChangedFiles() {
      return myOnlyVcsCheckBox.isSelected();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      JPanel panel = new JPanel();
      BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
      panel.setLayout(layout);

      panel.add(new JLabel(myText));
      myOnlyVcsCheckBox = new JCheckBox(CodeInsightBundle.message("process.scope.changed.files"));
      boolean lastRunVcsChangedTextEnabled = myLastRunOptions.getLastTextRangeType() == TextRangeType.VCS_CHANGED_TEXT;

      myOnlyVcsCheckBox.setEnabled(myContextHasChanges);
      myOnlyVcsCheckBox.setSelected(myContextHasChanges && lastRunVcsChangedTextEnabled);
      myOnlyVcsCheckBox.setBorder(new EmptyBorder(0, 10, 0, 0));
      panel.add(myOnlyVcsCheckBox);
      return panel;
    }

    @Nullable
    @Override
    protected String getHelpId() {
      return HELP_ID;
    }
  }
}
