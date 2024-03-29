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
package consulo.execution.ui.console.language;

import consulo.execution.ui.console.ConsoleView;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.language.Language;
import consulo.document.Document;
import consulo.codeEditor.EditorEx;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import consulo.disposer.Disposable;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author gregsh
 */
public interface LanguageConsoleView extends ConsoleView, Disposable {
  @Nonnull
  Project getProject();

  @Nonnull
  String getTitle();

  void setTitle(String title);

  @Nonnull
  PsiFile getFile();

  @Nonnull
  VirtualFile getVirtualFile();

  @Nonnull
  EditorEx getCurrentEditor();

  @Nonnull
  EditorEx getConsoleEditor();

  @Nonnull
  Document getEditorDocument();

  @Nonnull
  EditorEx getHistoryViewer();

  @Nonnull
  Language getLanguage();

  void setLanguage(@Nonnull Language language);

  @Nullable
  String getPrompt();

  @Nullable
  ConsoleViewContentType getPromptAttributes();

  void setPrompt(@Nullable String prompt);

  void setPromptAttributes(@Nonnull ConsoleViewContentType textAttributes);

  void setInputText(@Nonnull String inputText);

  boolean isEditable();

  void setEditable(boolean editable);

  boolean isConsoleEditorEnabled();

  void setConsoleEditorEnabled(boolean enabled);
}
