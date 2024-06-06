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

package consulo.ide.impl.idea.refactoring.lang;

import consulo.language.editor.refactoring.util.DirectoryUtil;
import consulo.application.ApplicationManager;
import consulo.undoRedo.CommandProcessor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.ide.impl.idea.openapi.fileTypes.ex.FileTypeChooser;
import consulo.application.HelpManager;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiManager;
import consulo.language.psi.path.PsiFileSystemItemUtil;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.language.util.IncorrectOperationException;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.io.File;

/**
 * @author ven
 */
public class ExtractIncludeDialog extends DialogWrapper {
  protected TextFieldWithBrowseButton myTargetDirectoryField;
  private JTextField myNameField;
  private final PsiDirectory myCurrentDirectory;
  private static final String REFACTORING_NAME = RefactoringBundle.message("extractIncludeFile.name");
  protected final String myExtension;
  protected JLabel myTargetDirLabel;

  public PsiDirectory getTargetDirectory() {
    return myTargetDirectory;
  }

  private PsiDirectory myTargetDirectory;

  public String getTargetFileName () {
    String name = myNameField.getText().trim();
    return name.contains(".") ? name: name + "." + myExtension;
  }

  public ExtractIncludeDialog(final PsiDirectory currentDirectory, final String extension) {
    super(true);
    myCurrentDirectory = currentDirectory;
    myExtension = extension;
    setTitle(REFACTORING_NAME);
    init();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myNameField;
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel panel = new JPanel(new VerticalFlowLayout());

    JLabel nameLabel = new JLabel();
    panel.add(nameLabel);

    myNameField = new JTextField();
    nameLabel.setLabelFor(myNameField);
    myNameField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        validateOKButton();
      }
    });
    panel.add(myNameField);
    nameLabel.setText(getNameLabel());

    myTargetDirLabel = new JLabel();
    panel.add(myTargetDirLabel);

    myTargetDirectoryField = new TextFieldWithBrowseButton();
    myTargetDirectoryField.setText(myCurrentDirectory.getVirtualFile().getPresentableUrl());
    myTargetDirectoryField.addBrowseFolderListener(RefactoringBundle.message("select.target.directory"),
                                                   RefactoringBundle.message("select.target.directory.description"),
                                                   null, FileChooserDescriptorFactory.createSingleFolderDescriptor());

    myTargetDirLabel.setText(RefactoringBundle.message("extract.to.directory"));
    panel.add(myTargetDirectoryField);

    myTargetDirectoryField.getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void textChanged(DocumentEvent event) {
        validateOKButton();
      }
    });

    validateOKButton();

    return panel;
  }

  protected String getNameLabel() {
    return RefactoringBundle.message("name.for.extracted.include.file", myExtension);
  }

  private void validateOKButton() {
    final String fileName = myNameField.getText().trim();
    setOKActionEnabled(myTargetDirectoryField.getText().trim().length() > 0 &&
                       fileName.length() > 0 && fileName.indexOf(File.separatorChar) < 0 &&
                       !StringUtil.containsAnyChar(fileName, "*?><\":;|") && fileName.indexOf(".") != 0);
  }

  private static boolean isFileExist(@Nonnull final String directory, @Nonnull final String fileName) {
    return LocalFileSystem.getInstance().findFileByIoFile(new File(directory, fileName)) != null;
  }

  @Override
  protected void doOKAction() {
    final Project project = myCurrentDirectory.getProject();

    final String directoryName = myTargetDirectoryField.getText().replace(File.separatorChar, '/');
    final String targetFileName = getTargetFileName();

    if (isFileExist(directoryName, targetFileName)) {
      Messages.showErrorDialog(project, RefactoringBundle.message("file.already.exist", targetFileName), RefactoringBundle.message("file.already.exist.title"));
      return;
    }

    final FileType type = FileTypeChooser.getKnownFileTypeOrAssociate(targetFileName);
    if (type == null) {
      return;
    }

    CommandProcessor.getInstance().executeCommand(project, new Runnable() {
      @Override
      public void run() {
        final Runnable action = new Runnable() {
          @Override
          public void run() {
            try {
              PsiDirectory targetDirectory = DirectoryUtil.mkdirs(PsiManager.getInstance(project), directoryName);
              targetDirectory.checkCreateFile(targetFileName);
              final String webPath = PsiFileSystemItemUtil.getRelativePath(myCurrentDirectory, targetDirectory);
              myTargetDirectory = webPath == null ? null : targetDirectory;
            }
            catch (IncorrectOperationException e) {
              CommonRefactoringUtil.showErrorMessage(REFACTORING_NAME, e.getMessage(), null, project);
            }
          }
        };
        ApplicationManager.getApplication().runWriteAction(action);
      }
    }, RefactoringBundle.message("create.directory"), null);
    if (myTargetDirectory == null) return;
    super.doOKAction();
  }

  @Override
  protected void doHelpAction() {
    HelpManager.getInstance().invokeHelp(getHelpTopic());
  }

  protected String getHelpTopic() {
    return ExtractIncludeFileBase.HELP_ID;
  }

  @Override
  @Nonnull
  protected Action[] createActions() {
    return new Action[]{getOKAction(), getCancelAction(), getHelpAction()};
  }

  protected void hideTargetDirectory() {
    myTargetDirectoryField.setVisible(false);
    myTargetDirLabel.setVisible(false);
  }
}
