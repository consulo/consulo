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

package consulo.language.editor.refactoring.move.fileOrDirectory;

import consulo.application.ApplicationManager;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.HelpManager;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserFactory;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.RefactoringSettings;
import consulo.language.editor.refactoring.copy.CopyFilesOrDirectoriesDialog;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.editor.refactoring.util.DirectoryUtil;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.util.IncorrectOperationException;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.RecentsManager;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.undoRedo.CommandProcessor;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.io.File;
import java.util.List;

public class MoveFilesOrDirectoriesDialog extends DialogWrapper {
  @NonNls private static final String RECENT_KEYS = "MoveFile.RECENT_KEYS";
  @NonNls private static final String MOVE_FILES_OPEN_IN_EDITOR = "MoveFile.OpenInEditor";


  public interface Callback {
    void run(MoveFilesOrDirectoriesDialog dialog);
  }

  private JLabel myNameLabel;
  private TextFieldWithHistoryWithBrowseButton myTargetDirectoryField;
  private String myHelpID;
  private final Project myProject;
  private final Callback myCallback;
  private PsiDirectory myTargetDirectory;
  private JCheckBox myCbSearchForReferences;
  private JCheckBox myOpenInEditorCb;

  public MoveFilesOrDirectoriesDialog(Project project, Callback callback) {
    super(project, true);
    myProject = project;
    myCallback = callback;
    setTitle(RefactoringBundle.message("move.title"));
    init();
  }

  @Override
  @Nonnull
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myTargetDirectoryField;
  }

  @Override
  protected JComponent createCenterPanel() {
    return null;
  }

  @Override
  protected JComponent createNorthPanel() {
    myNameLabel = JBLabelDecorator.createJBLabelDecorator().setBold(true);

    myTargetDirectoryField = new TextFieldWithHistoryWithBrowseButton();
    final List<String> recentEntries = RecentsManager.getInstance(myProject).getRecentEntries(RECENT_KEYS);
    if (recentEntries != null) {
      myTargetDirectoryField.getChildComponent().setHistory(recentEntries);
    }
    final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
    myTargetDirectoryField.addBrowseFolderListener(RefactoringBundle.message("select.target.directory"),
                                                   RefactoringBundle.message("the.file.will.be.moved.to.this.directory"),
                                                   myProject,
                                                   descriptor,
                                                   TextComponentAccessor.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT);
    final JTextField textField = myTargetDirectoryField.getChildComponent().getTextEditor();
    FileChooserFactory.getInstance().installFileCompletion(textField, descriptor, true, getDisposable());
    textField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        validateOKButton();
      }
    });
    myTargetDirectoryField.setTextFieldPreferredWidth(CopyFilesOrDirectoriesDialog.MAX_PATH_LENGTH);
    Disposer.register(getDisposable(), myTargetDirectoryField);

    String shortcutText = KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION));

    myCbSearchForReferences = new NonFocusableCheckBox(RefactoringBundle.message("search.for.references"));
    myCbSearchForReferences.setSelected(RefactoringSettings.getInstance().MOVE_SEARCH_FOR_REFERENCES_FOR_FILE);

    myOpenInEditorCb = new NonFocusableCheckBox("Open moved files in editor");
    myOpenInEditorCb.setSelected(isOpenInEditor());

    return FormBuilder.createFormBuilder().addComponent(myNameLabel)
            .addLabeledComponent(RefactoringBundle.message("move.files.to.directory.label"), myTargetDirectoryField, UIUtil.LARGE_VGAP)
            .addTooltip(RefactoringBundle.message("path.completion.shortcut", shortcutText))
            .addComponentToRightColumn(myCbSearchForReferences, UIUtil.LARGE_VGAP)
            .addComponentToRightColumn(myOpenInEditorCb, UIUtil.LARGE_VGAP)
            .getPanel();
  }

  public void setData(PsiElement[] psiElements, PsiDirectory initialTargetDirectory, @NonNls String helpID) {
    if (psiElements.length == 1) {
      String text;
      if (psiElements[0] instanceof PsiFile) {
        text = RefactoringBundle.message("move.file.0",
                                         CopyFilesOrDirectoriesDialog.shortenPath(((PsiFile)psiElements[0]).getVirtualFile()));
      }
      else {
        text = RefactoringBundle.message("move.directory.0",
                                         CopyFilesOrDirectoriesDialog.shortenPath(((PsiDirectory)psiElements[0]).getVirtualFile()));
      }
      myNameLabel.setText(text);
    }
    else {
      boolean isFile = true;
      boolean isDirectory = true;
      for (PsiElement psiElement : psiElements) {
        isFile &= psiElement instanceof PsiFile;
        isDirectory &= psiElement instanceof PsiDirectory;
      }
      myNameLabel.setText(isFile ?
                          RefactoringBundle.message("move.specified.files") :
                          isDirectory ?
                          RefactoringBundle.message("move.specified.directories") :
                          RefactoringBundle.message("move.specified.elements"));
    }

    final String initialTargetPath = initialTargetDirectory == null ? "" : initialTargetDirectory.getVirtualFile().getPresentableUrl();
    myTargetDirectoryField.getChildComponent().setText(initialTargetPath);
    final int lastDirectoryIdx = initialTargetPath.lastIndexOf(File.separator);
    final int textLength = initialTargetPath.length();
    if (lastDirectoryIdx > 0 && lastDirectoryIdx + 1 < textLength) {
      myTargetDirectoryField.getChildComponent().getTextEditor().select(lastDirectoryIdx + 1, textLength);
    }

    validateOKButton();
    myHelpID = helpID;
  }

  @Override
  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp(myHelpID);
  }

  public static boolean isOpenInEditor() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return false;
    }
    return ApplicationPropertiesComponent.getInstance().getBoolean(MOVE_FILES_OPEN_IN_EDITOR, false);
  }

  private void validateOKButton() {
    setOKActionEnabled(myTargetDirectoryField.getChildComponent().getText().length() > 0);
  }

  @Override
  protected void doOKAction() {
    ApplicationPropertiesComponent.getInstance().setValue(MOVE_FILES_OPEN_IN_EDITOR, myOpenInEditorCb.isSelected(), false);
    //myTargetDirectoryField.getChildComponent().addCurrentTextToHistory();
    RecentsManager.getInstance(myProject).registerRecentEntry(RECENT_KEYS, myTargetDirectoryField.getChildComponent().getText());
    RefactoringSettings.getInstance().MOVE_SEARCH_FOR_REFERENCES_FOR_FILE = myCbSearchForReferences.isSelected();

    if (DumbService.isDumb(myProject)) {
      Messages.showMessageDialog(myProject, "Move refactoring is not available while indexing is in progress", "Indexing", null);
      return;
    }

    CommandProcessor.getInstance().executeCommand(myProject, () -> {
      final Runnable action = () -> {
        String directoryName = myTargetDirectoryField.getChildComponent().getText().replace(File.separatorChar, '/');
        try {
          myTargetDirectory = DirectoryUtil.mkdirs(PsiManager.getInstance(myProject), directoryName);
        }
        catch (IncorrectOperationException e) {
          // ignore
        }
      };

      ApplicationManager.getApplication().runWriteAction(action);
      if (myTargetDirectory == null) {
        CommonRefactoringUtil.showErrorMessage(getTitle(),
                                               RefactoringBundle.message("cannot.create.directory"), myHelpID, myProject);
        return;
      }
      myCallback.run(this);
    }, RefactoringBundle.message("move.title"), null);
  }

  public PsiDirectory getTargetDirectory() {
    return myTargetDirectory;
  }
}
