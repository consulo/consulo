/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.editor.impl.intention;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.file.FileTypeManager;
import consulo.language.impl.internal.psi.PsiManagerImpl;
import consulo.language.impl.psi.PsiDirectoryImpl;
import consulo.language.psi.*;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.IOException;

/**
 * @author peter
 */
public class CreateFileFix extends LocalQuickFixAndIntentionActionOnPsiElement {
  private final boolean myIsDirectory;
  private final String myNewFileName;
  private final String myText;
  @Nonnull
  private final String myKey;
  private boolean myIsAvailable;
  private long myIsAvailableTimeStamp;
  private static final int REFRESH_INTERVAL = 1000;

  public CreateFileFix(boolean isDirectory, @Nonnull String newFileName, @Nonnull PsiDirectory directory, @Nullable String text, @Nonnull String key) {
    super(directory);

    myIsDirectory = isDirectory;
    myNewFileName = newFileName;
    myText = text;
    myKey = key;
    myIsAvailable = isDirectory || !FileTypeManager.getInstance().getFileTypeByFileName(newFileName).isBinary();
    myIsAvailableTimeStamp = System.currentTimeMillis();
  }

  public CreateFileFix(@Nonnull String newFileName, @Nonnull PsiDirectory directory, String text) {
    this(false, newFileName, directory, text, "create.file.text");
  }

  public CreateFileFix(boolean isDirectory, @Nonnull String newFileName, @Nonnull PsiDirectory directory) {
    this(isDirectory, newFileName, directory, null, isDirectory ? "create.directory.text" : "create.file.text");
  }

  @Nullable
  protected String getFileText() {
    return myText;
  }

  @Override
  @Nonnull
  public String getText() {
    return CodeInsightBundle.message(myKey, myNewFileName);
  }

  @Override
  @Nonnull
  public String getFamilyName() {
    return CodeInsightBundle.message("create.file.family");
  }

  @Override
  public void invoke(@Nonnull Project project, @Nonnull PsiFile file, Editor editor, @Nonnull PsiElement startElement, @Nonnull PsiElement endElement) {
    if (isAvailable(project, null, file)) {
      invoke(project, (PsiDirectory)startElement);
    }
  }

  @Override
  public void applyFix() {
    invoke(myStartElement.getProject(), (PsiDirectory)myStartElement.getElement());
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, @Nonnull PsiFile file, @Nonnull PsiElement startElement, @Nonnull PsiElement endElement) {
    PsiDirectory myDirectory = (PsiDirectory)startElement;
    long current = System.currentTimeMillis();

    if (ApplicationManager.getApplication().isUnitTestMode() || current - myIsAvailableTimeStamp > REFRESH_INTERVAL) {
      myIsAvailable &= myDirectory.getVirtualFile().findChild(myNewFileName) == null;
      myIsAvailableTimeStamp = current;
    }

    return myIsAvailable;
  }

  private void invoke(@Nonnull Project project, PsiDirectory myDirectory) throws IncorrectOperationException {
    myIsAvailableTimeStamp = 0; // to revalidate applicability

    try {
      if (myIsDirectory) {
        myDirectory.createSubdirectory(myNewFileName);
      }
      else {
        String newFileName = myNewFileName;
        String newDirectories = null;
        if (myNewFileName.contains("/")) {
          int pos = myNewFileName.lastIndexOf("/");
          newFileName = myNewFileName.substring(pos + 1);
          newDirectories = myNewFileName.substring(0, pos);
        }
        PsiDirectory directory = myDirectory;
        if (newDirectories != null) {
          try {
            VirtualFileUtil.createDirectoryIfMissing(myDirectory.getVirtualFile(), newDirectories);
            VirtualFile vfsDir = VirtualFileUtil.findRelativeFile(myDirectory.getVirtualFile(), ArrayUtil.toStringArray(StringUtil.split(newDirectories, "/")));
            directory = new PsiDirectoryImpl((PsiManagerImpl)myDirectory.getManager(), vfsDir);
          }
          catch (IOException e) {
            throw new IncorrectOperationException(e.getMessage());
          }
        }
        PsiFile newFile = directory.createFile(newFileName);
        String text = getFileText();

        if (text != null) {
          FileType type = FileTypeRegistry.getInstance().getFileTypeByFileName(newFileName);
          PsiFile psiFile = PsiFileFactory.getInstance(project).createFileFromText("_" + newFileName, type, text);
          PsiElement psiElement = CodeStyleManager.getInstance(project).reformat(psiFile);
          text = psiElement.getText();
        }

        openFile(project, directory, newFile, text);
      }
    }
    catch (IncorrectOperationException e) {
      myIsAvailable = false;
    }
  }

  protected void openFile(@Nonnull Project project, PsiDirectory directory, PsiFile newFile, String text) {
    FileEditorManager editorManager = FileEditorManager.getInstance(directory.getProject());
    FileEditor[] fileEditors = editorManager.openFile(newFile.getVirtualFile(), true);

    if (text != null) {
      for (FileEditor fileEditor : fileEditors) {
        if (fileEditor instanceof TextEditor) { // JSP is not safe to edit via Psi
          Document document = ((TextEditor)fileEditor).getEditor().getDocument();
          document.setText(text);

          if (ApplicationManager.getApplication().isUnitTestMode()) {
            FileDocumentManager.getInstance().saveDocument(document);
          }
          PsiDocumentManager.getInstance(project).commitDocument(document);
          break;
        }
      }
    }
  }
}
