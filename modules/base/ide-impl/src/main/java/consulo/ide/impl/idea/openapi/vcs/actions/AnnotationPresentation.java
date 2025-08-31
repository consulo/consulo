/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.ui.ex.action.AnAction;
import consulo.colorScheme.EditorColorKey;
import consulo.colorScheme.EditorFontType;
import consulo.ide.impl.idea.openapi.localVcs.UpToDateLineNumberProvider;
import consulo.ide.impl.idea.openapi.vcs.annotate.*;
import consulo.versionControlSystem.annotate.AnnotationSource;
import consulo.versionControlSystem.annotate.AnnotationSourceSwitcher;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.util.lang.ObjectUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

class AnnotationPresentation implements TextAnnotationPresentation {
  @Nonnull
  private final FileAnnotation myFileAnnotation;
  @Nonnull
  private final UpToDateLineNumberProvider myUpToDateLineNumberProvider;
  @Nullable private final AnnotationSourceSwitcher mySwitcher;
  private final ArrayList<AnAction> myActions = new ArrayList<>();

  @Nonnull
  private final Disposable myDisposable;
  private boolean myDisposed = false;

  AnnotationPresentation(@Nonnull FileAnnotation fileAnnotation,
                         @Nonnull UpToDateLineNumberProvider upToDateLineNumberProvider,
                         @Nullable AnnotationSourceSwitcher switcher,
                         @Nonnull Disposable disposable) {
    myUpToDateLineNumberProvider = upToDateLineNumberProvider;
    myFileAnnotation = fileAnnotation;
    mySwitcher = switcher;
    myDisposable = disposable;
  }

  @Override
  public EditorFontType getFontType(int line) {
    VcsRevisionNumber revision = myFileAnnotation.originalRevision(line);
    VcsRevisionNumber currentRevision = myFileAnnotation.getCurrentRevision();
    return currentRevision != null && currentRevision.equals(revision) ? EditorFontType.BOLD : EditorFontType.PLAIN;
  }

  @Override
  public EditorColorKey getColor(int line) {
    if (mySwitcher == null) return AnnotationSource.LOCAL.getColor();
    return mySwitcher.getAnnotationSource(line).getColor();
  }

  @Override
  public List<AnAction> getActions(int line) {
    int correctedNumber = myUpToDateLineNumberProvider.getLineNumber(line);
    for (AnAction action : myActions) {
      UpToDateLineNumberListener upToDateListener = ObjectUtil.tryCast(action, UpToDateLineNumberListener.class);
      if (upToDateListener != null) upToDateListener.accept(correctedNumber);

      LineNumberListener listener = ObjectUtil.tryCast(action, LineNumberListener.class);
      if (listener != null) listener.accept(line);
    }

    return myActions;
  }

  @Nonnull
  public List<AnAction> getActions() {
    return myActions;
  }

  public void addAction(AnAction action) {
    myActions.add(action);
  }

  public void addAction(AnAction action, int index) {
    myActions.add(index, action);
  }

  @Override
  public void gutterClosed() {
    if (myDisposed) return;
    myDisposed = true;
    Disposer.dispose(myDisposable);
  }
}
