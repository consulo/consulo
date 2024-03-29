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
package consulo.language.editor.ui;

import consulo.language.psi.PsiFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Predicate;

/**
 * Shows dialog with two tabs: Project View-like tree and Goto symbol-like list with quick search capability
 * Allows to quickly locate and choose PsiFile among all files inside project
 * (optionally filtered based on file type or general file filter(see PsiFileFilter))
 * @see TreeFileChooserFactory#createFileChooser(String, PsiFile, FileType, Predicate)
 */
public interface TreeFileChooser {
  /**
   * @return null when no files were selected or dialog has been canceled
   */
  @Nullable PsiFile getSelectedFile();

  /**
   * @param file to be selected in tree view tab of this dialog
   */
  void selectFile(@Nonnull PsiFile file);

  void showDialog();
}
