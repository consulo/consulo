/*
 * Copyright 2013-2021 consulo.io
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
package consulo.execution.ui.console;

import consulo.application.Application;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditorManager;
import consulo.navigation.OpenFileDescriptor;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 14/01/2021
 * <p>
 * extract part of {@link OpenFileHyperlinkInfo}
 */
public abstract class FileHyperlinkInfoBase implements FileHyperlinkInfo {
  private static final int UNDEFINED_OFFSET = -1;

  private final Project myProject;
  private final boolean myIncludeInOccurenceNavigation;
  private final int myDocumentLine;
  private final int myDocumentColumn;

  public FileHyperlinkInfoBase(@Nonnull Project project, boolean includeInOccurenceNavigation, int documentLine, int documentColumn) {
    myProject = project;
    myIncludeInOccurenceNavigation = includeInOccurenceNavigation;
    myDocumentLine = documentLine;
    myDocumentColumn = documentColumn;
  }

  public FileHyperlinkInfoBase(@Nonnull Project project, int line) {
    this(project, line, 0);
  }

  public FileHyperlinkInfoBase(@Nonnull Project project, int line, int column) {
    this(project, true, line, column);
  }

  @Nullable
  protected abstract VirtualFile getVirtualFile();

  @Override
  public OpenFileDescriptor getDescriptor() {
    VirtualFile file = getVirtualFile();
    if (file == null || !file.isValid()) {
      return null;
    }

    int line = myDocumentLine;
    FileDocumentManager.getInstance().getDocument(file); // need to load decompiler text
    LineNumbersMapping mapping = file.getUserData(LineNumbersMapping.LINE_NUMBERS_MAPPING_KEY);
    if (mapping != null) {
      line = mapping.bytecodeToSource(myDocumentLine + 1) - 1;
      if (line < 0) {
        line = myDocumentLine;
      }
    }

    int offset = calculateOffset(file, line, myDocumentColumn);
    if (offset != UNDEFINED_OFFSET) {
      return OpenFileDescriptorFactory.getInstance(myProject).builder(file).offset(offset).build();
    }
    // although document position != logical position, it seems better than returning 'null'
    return OpenFileDescriptorFactory.getInstance(myProject).builder(file).line(line).column(myDocumentColumn).build();
  }

  @RequiredUIAccess
  @Override
  public void navigate(Project project) {
    Application.get().runReadAction(() -> {
      OpenFileDescriptor descriptor = getDescriptor();
      if (descriptor != null) {
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
      }
    });
  }

  @Override
  public boolean includeInOccurenceNavigation() {
    return myIncludeInOccurenceNavigation;
  }

  /**
   * Calculates an offset, that matches given line and column of the document.
   *
   * @param file           VirtualFile instance
   * @param documentLine   zero-based line of the document
   * @param documentColumn zero-based column of the document
   * @return calculated offset or UNDEFINED_OFFSET if it's impossible to calculate
   */
  private static int calculateOffset(@Nonnull VirtualFile file, int documentLine, int documentColumn) {
    return Application.get().runReadAction((Supplier<Integer>)() -> {
      Document document = FileDocumentManager.getInstance().getDocument(file);
      if (document != null) {
        int lineCount = document.getLineCount();
        if (0 <= documentLine && documentLine < lineCount) {
          int lineStartOffset = document.getLineStartOffset(documentLine);
          int lineEndOffset = document.getLineEndOffset(documentLine);
          int fixedColumn = Math.min(Math.max(documentColumn, 0), lineEndOffset - lineStartOffset);
          return lineStartOffset + fixedColumn;
        }
      }
      return UNDEFINED_OFFSET;
    });
  }
}