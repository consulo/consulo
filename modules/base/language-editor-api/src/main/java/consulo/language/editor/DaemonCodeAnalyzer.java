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

package consulo.language.editor;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.disposer.Disposable;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Manages the background highlighting and auto-import for files displayed in editors.
 */
@ServiceAPI(value = ComponentScope.PROJECT, lazy = false)
public abstract class DaemonCodeAnalyzer {
  @Nonnull
  public static DaemonCodeAnalyzer getInstance(@Nonnull Project project) {
    return project.getComponent(DaemonCodeAnalyzer.class);
  }

  public abstract void settingsChanged();

  public abstract void setUpdateByTimerEnabled(boolean value);

  public abstract void disableUpdateByTimer(@Nonnull Disposable parentDisposable);

  public abstract boolean isHighlightingAvailable(@Nullable PsiFile file);

  public abstract void setImportHintsEnabled(@Nonnull PsiFile file, boolean value);

  public abstract void resetImportHintsEnabledForProject();

  public abstract void setHighlightingEnabled(@Nonnull PsiFile file, boolean value);

  public abstract boolean isImportHintsEnabled(@Nonnull PsiFile file);

  public abstract boolean isAutohintsAvailable(@Nullable PsiFile file);

  public abstract ProgressIndicator createDaemonProgressIndicator();

  /**
   * Force rehighlighting for all files.
   */
  public abstract void restart();

  /**
   * Force rehighlighting for a specific file.
   *
   * @param file the file to rehighlight.
   */
  public abstract void restart(@Nonnull PsiFile file);

  public abstract void autoImportReferenceAtCursor(@Nonnull Editor editor, @Nonnull PsiFile file);
}
