// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.ElementPattern;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
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

  void addAdvertisement(@Nonnull String message, @Nullable Icon icon);

  CompletionParameters getParameters();

  void setParameters(@Nonnull CompletionParameters parameters);

  void scheduleRestart();

  void prefixUpdated();
}
