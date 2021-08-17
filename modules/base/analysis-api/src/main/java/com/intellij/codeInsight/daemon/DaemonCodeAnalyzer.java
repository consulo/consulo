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

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.messages.Topic;
import consulo.disposer.Disposable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;

/**
 * Manages the background highlighting and auto-import for files displayed in editors.
 */
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
    /**
     * Fired when the background code analysis is being scheduled for the specified set of files.
     *
     * @param fileEditors The list of files that will be analyzed during the current execution of the daemon.
     */
    default void daemonStarting(@Nonnull Collection<FileEditor> fileEditors) {
    }

    /**
     * Fired when the background code analysis is done.
     */
    default void daemonFinished() {
    }

    /**
     * Fired when the background code analysis is done.
     *
     * @param fileEditors The list of files analyzed during the current execution of the daemon.
     */
    default void daemonFinished(@Nonnull Collection<FileEditor> fileEditors) {
      daemonFinished();
    }

    default void daemonCancelEventOccurred(@Nonnull String reason) {
    }
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
