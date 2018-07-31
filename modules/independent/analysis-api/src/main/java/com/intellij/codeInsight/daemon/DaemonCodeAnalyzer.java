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

package com.intellij.codeInsight.daemon;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.messages.Topic;
import javax.annotation.Nonnull;

/**
 * Manages the background highlighting and auto-import for files displayed in editors.
 */
public abstract class DaemonCodeAnalyzer {
  @Nonnull
  @Deprecated
  public static DaemonCodeAnalyzer getInstance(@Nonnull Project project) {
    return project.getComponent(DaemonCodeAnalyzer.class);
  }

  public abstract void settingsChanged();

  @Deprecated
  public abstract void updateVisibleHighlighters(@Nonnull Editor editor);

  public abstract void setUpdateByTimerEnabled(boolean value);
  public abstract void disableUpdateByTimer(@Nonnull Disposable parentDisposable);

  public abstract boolean isHighlightingAvailable(@javax.annotation.Nullable PsiFile file);

  public abstract void setImportHintsEnabled(@Nonnull PsiFile file, boolean value);
  public abstract void resetImportHintsEnabledForProject();
  public abstract void setHighlightingEnabled(@Nonnull PsiFile file, boolean value);
  public abstract boolean isImportHintsEnabled(@Nonnull PsiFile file);
  public abstract boolean isAutohintsAvailable(@javax.annotation.Nullable PsiFile file);

  /**
   * Force rehighlighting for all files.
   */
  public abstract void restart();

  /**
   * Force rehighlighting for a specific file.
   * @param file the file to rehighlight.
   */
  public abstract void restart(@Nonnull PsiFile file);

  public abstract void autoImportReferenceAtCursor(@Nonnull Editor editor, @Nonnull PsiFile file);

  public static final Topic<DaemonListener> DAEMON_EVENT_TOPIC = Topic.create("DAEMON_EVENT_TOPIC", DaemonListener.class);

  public interface DaemonListener {
    void daemonFinished();
    void daemonCancelEventOccurred(@Nonnull String reason);
  }

  public abstract static class DaemonListenerAdapter implements DaemonListener {
    @Override
    public void daemonFinished() {
    }

    @Override
    public void daemonCancelEventOccurred(@Nonnull String reason) {
    }
  }
}
