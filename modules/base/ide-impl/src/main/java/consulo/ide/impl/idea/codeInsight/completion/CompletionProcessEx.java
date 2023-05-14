// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.completion;

import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProcess;
import consulo.language.editor.completion.OffsetMap;
import consulo.project.Project;
import consulo.language.pattern.ElementPattern;
import consulo.disposer.Disposable;
import consulo.ui.image.Image;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Supplier;

/**
 * @author yole
 */
interface CompletionProcessEx extends CompletionProcess {
  @Nonnull
  Project getProject();

  @Nonnull
  Editor getEditor();

  @Nonnull
  Caret getCaret();

  @Nonnull
  OffsetMap getOffsetMap();

  @Nonnull
  OffsetsInFile getHostOffsets();

  @Nullable
  Lookup getLookup();

  void registerChildDisposable(@Nonnull Supplier<? extends Disposable> child);

  void itemSelected(LookupElement item, char aChar);

  void addWatchedPrefix(int startOffset, ElementPattern<String> restartCondition);

  void addAdvertisement(@Nonnull String message, @Nullable Image icon);

  CompletionParameters getParameters();

  void setParameters(@Nonnull CompletionParameters parameters);

  void scheduleRestart();

  void prefixUpdated();
}
