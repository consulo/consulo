/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.editor.refactoring.copy;

import consulo.application.ApplicationManager;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.HelpManager;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.util.DirectoryUtil;
import consulo.language.psi.*;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.ex.RecentsManager;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.internal.DialogWrapperPeer;
import consulo.ui.ex.keymap.util.KeymapUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.util.io.PathUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.util.List;

public class CopyFilesOrDirectoriesDialog extends DialogWrapper {
  public static final int MAX_PATH_LENGTH = 70;

  private static final String COPY_OPEN_IN_EDITOR = "Copy.OpenInEditor";
  private static final String RECENT_KEYS = "CopyFile.RECENT_KEYS";

  public static String shortenPath(VirtualFile file) {
    return StringUtil.shortenPathWithEllipsis(file.getPresentableUrl(), MAX_PATH_LENGTH);
  }

  public static JCheckBox createOpenInEditorCB() {
    JCheckBox checkBox = new JCheckBox("Open copy in editor", ApplicationPropertiesComponent.getInstance().getBoolean(COPY_OPEN_IN_EDITOR, true));
    checkBox.setMnemonic('o');
    return checkBox;
  }

  public static void saveOpenInEditorState(boolean selected) {
    ApplicationPropertiesComponent.getInstance().setValue(COPY_OPEN_IN_EDITOR, String.valueOf(selected));
  }

  private JLabel myInformationLabel;
  private TextFieldWithHistoryWithBrowseButton myTargetDirectoryField;
  private JCheckBox myOpenFilesInEditor = createOpenInEditorCB();
  private JTextField myNewNameField;
  private final Project myProject;
  private final boolean myShowDirectoryField;
  private final boolean myShowNewNameField;

  private PsiDirectory myTargetDirectory;
  private boolean myFileCopy = false;

  public CopyFilesOrDirectoriesDialog(PsiElement[] elements, PsiDirectory defaultTargetDirectory, Project project, boolean doClone) {
    super(project, true);
    myProject = project;
    myShowDirectoryField = !doClone;
    myShowNewNameField = elements.length == 1;

    if (doClone && elements.length != 1) {
      throw new IllegalArgumentException("wrong number of elements to clone: " + elements.length);
    }

    setTitle(RefactoringBundle.message(doClone ? "copy.files.clone.title" : "copy.files.copy.title"));
    init();

    if (elements.length == 1) {
      String text;
      if (elements[0] instanceof PsiFile) {
        PsiFile file = (PsiFile)elements[0];
        String url = shortenPath(file.getVirtualFile());
        text = RefactoringBundle.message(doClone ? "copy.files.clone.file.0" : "copy.files.copy.file.0", url);
        final String fileName = file.getName();
        myNewNameField.setText(fileName);
        final int dotIdx = fileName.lastIndexOf(".");
        if (dotIdx > -1) {
          myNewNameField.select(0, dotIdx);
          myNewNameField.putClientProperty(DialogWrapperPeer.HAVE_INITIAL_SELECTION, true);
        }
        myFileCopy = true;
      }
      else {
        PsiDirectory directory = (PsiDirectory)elements[0];
        String url = shortenPath(directory.getVirtualFile());
        text = RefactoringBundle.message(doClone ? "copy.files.clone.directory.0" : "copy.files.copy.directory.0", url);
        myNewNameField.setText(directory.getName());
      }
      myInformationLabel.setText(text);
    }
    else {
      setMultipleElementCopyLabel(elements);
    }

    boolean allBinary = true;
    for (PsiElement element : elements) {
      if (!(element.getContainingFile() instanceof PsiBinaryFile)) {
        allBinary = false;
        break;
      }
    }
    if (allBinary) {
      myOpenFilesInEditor.setVisible(false);
    }
    if (myShowDirectoryField) {
      String targetPath = defaultTargetDirectory == null ? "" : defaultTargetDirectory.getVirtualFile().getPresentableUrl();
      myTargetDirectoryField.getChildComponent().setText(targetPath);
    }
    validateOKButton();
  }

  private void setMultipleElementCopyLabel(PsiElement[] elements) {
    boolean allFiles = true;
    boolean allDirectories = true;
    for (PsiElement element : elements) {
      if (element instanceof PsiDirectory) {
        allFiles = false;
      }
      else {
        allDirectories = false;
      }
    }
    if (allFiles) {
      myInformationLabel.setText(RefactoringBundle.message("copy.files.copy.specified.files.label"));
    }
    else if (allDirectories) {
      myInformationLabel.setText(RefactoringBundle.message("copy.files.copy.specified.directories.label"));
    }
    else {
      myInformationLabel.setText(RefactoringBundle.message("copy.files.copy.specified.mixed.label"));
    }
  }

  @Override
  @Nonnull
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myShowNewNameField ? myNewNameField : myTargetDirectoryField.getChildComponent();
  }

  @Override
  protected JComponent createCenterPanel() {
    return new JPanel(new BorderLayout());
  }

  @Override
  protected JComponent createNorthPanel() {
    myInformationLabel = JBLabelDecorator.createJBLabelDecorator().setBold(true);
    final FormBuilder formBuilder = FormBuilder.createFormBuilder().addComponent(myInformationLabel).addVerticalGap(
      UIUtil.LARGE_VGAP - UIUtil.DEFAULT_VGAP);
    DocumentListener documentListener = new DocumentAdapter() {
      @Override
      public void textChanged(DocumentEvent event) {
        validateOKButton();
      }
    };

    if (myShowNewNameField) {
      myNewNameField = new JTextField();
      myNewNameField.getDocument().addDocumentListener(documentListener);
      formBuilder.addLabeledComponent(RefactoringBundle.message("copy.files.new.name.label"), myNewNameField);
    }

    if (myShowDirectoryField) {
      myTargetDirectoryField = new TextFieldWithHistoryWithBrowseButton();
      myTargetDirectoryField.setTextFieldPreferredWidth(MAX_PATH_LENGTH);
      final List<String> recentEntries = RecentsManager.getInstance(myProject).getRecentEntries(RECENT_KEYS);
      if (recentEntries != null) {
        myTargetDirectoryField.getChildComponent().setHistory(recentEntries);
      }
      final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
      myTargetDirectoryField.addBrowseFolderListener(RefactoringBundle.message("select.target.directory"),
                                                     RefactoringBundle.message("the.file.will.be.copied.to.this.directory"),
                                                     myProject, descriptor,
                                                     TextComponentAccessor.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT);
      myTargetDirectoryField.getChildComponent().addDocumentListener(new DocumentAdapter() {
        @Override
        protected void textChanged(DocumentEvent e) {
          validateOKButton();
        }
      });
      formBuilder.addLabeledComponent(RefactoringBundle.message("copy.files.to.directory.label"), myTargetDirectoryField);

      String shortcutText =
        KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_CODE_COMPLETION));
      formBuilder.addTooltip(RefactoringBundle.message("path.completion.shortcut", shortcutText));
    }

    final JPanel wrapper = new JPanel(new BorderLayout());
    wrapper.add(myOpenFilesInEditor, BorderLayout.EAST);
    formBuilder.addComponent(wrapper);
    return formBuilder.getPanel();
  }

  public PsiDirectory getTargetDirectory() {
    return myTargetDirectory;
  }

  public String getNewName() {
    return myNewNameField != null ? myNewNameField.getText().trim() : null;
  }

  public boolean openInEditor() {
    return myOpenFilesInEditor.isSelected();
  }

  @Override
  protected void doOKAction() {
    if (myShowNewNameField) {
      String newName = getNewName();

      if (newName.length() == 0) {
        Messages.showErrorDialog(myProject, RefactoringBundle.message("no.new.name.specified"), RefactoringBundle.message("error.title"));
        return;
      }

      if (myFileCopy && !PathUtil.isValidFileName(newName)) {
        Messages.showErrorDialog(myNewNameField, "Name is not a valid file name");
        return;
      }
    }

    saveOpenInEditorState(myOpenFilesInEditor.isSelected());
    if (myShowDirectoryField) {
      final String targetDirectoryName = myTargetDirectoryField.getChildComponent().getText();

      if (targetDirectoryName.length() == 0) {
        Messages.showErrorDialog(myProject, RefactoringBundle.message("no.target.directory.specified"),
                                 RefactoringBundle.message("error.title"));
        return;
      }

      RecentsManager.getInstance(myProject).registerRecentEntry(RECENT_KEYS, targetDirectoryName);

      CommandProcessor.getInstance().executeCommand(myProject, new Runnable() {
        @Override
        public void run() {
          ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
              try {
                myTargetDirectory =
                  DirectoryUtil.mkdirs(PsiManager.getInstance(myProject), targetDirectoryName.replace(File.separatorChar, '/'));
              }
              catch (IncorrectOperationException ignored) { }
            }
          });
        }
      }, RefactoringBundle.message("create.directory"), null);

      if (myTargetDirectory == null) {
        Messages.showErrorDialog(myProject, RefactoringBundle.message("cannot.create.directory"), RefactoringBundle.message("error.title"));
        return;
      }
    }

    super.doOKAction();
  }

  private void validateOKButton() {
    if (myShowDirectoryField) {
      if (myTargetDirectoryField.getChildComponent().getText().length() == 0) {
        setOKActionEnabled(false);
        return;
      }
    }
    if (myShowNewNameField) {
      final String newName = getNewName();
      if (newName.length() == 0 || myFileCopy && !PathUtil.isValidFileName(newName)) {
        setOKActionEnabled(false);
        return;
      }
    }
    setOKActionEnabled(true);
  }

  @Override
  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp("refactoring.copyClass");
  }
}
