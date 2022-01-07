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
package com.intellij.codeInsight.daemon.impl.analysis;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter;
import com.intellij.lang.annotation.AnnotationSession;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.TextAttributesScheme;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HighlightInfoHolder {
  private final PsiFile myContextFile;
  private final HighlightInfoFilter[] myFilters;
  private final AnnotationSession myAnnotationSession;
  private int myErrorCount;
  private final List<HighlightInfo> myInfos = new ArrayList<>(5);

  public HighlightInfoHolder(@Nonnull PsiFile contextFile, @Nonnull HighlightInfoFilter ... filters) {
    myContextFile = contextFile;
    myAnnotationSession = new AnnotationSession(contextFile);
    myFilters = filters;
  }

  @Nonnull
  public AnnotationSession getAnnotationSession() {
    return myAnnotationSession;
  }

  public boolean add(@Nullable HighlightInfo info) {
    if (info == null || !accepted(info)) return false;

    HighlightSeverity severity = info.getSeverity();
    if (severity == HighlightSeverity.ERROR) {
      myErrorCount++;
    }

    return myInfos.add(info);
  }

  public void clear() {
    myErrorCount = 0;
    myInfos.clear();
  }

  public boolean hasErrorResults() {
    return myErrorCount != 0;
  }

  public boolean addAll(@Nonnull Collection<? extends HighlightInfo> highlightInfos) {
    boolean added = false;
    for (HighlightInfo highlightInfo : highlightInfos) {
      added |= add(highlightInfo);
    }
    return added;
  }

  public int size() {
    return myInfos.size();
  }

  @Nonnull
  public HighlightInfo get(int i) {
    return myInfos.get(i);
  }

  @Nonnull
  public Project getProject() {
    return myContextFile.getProject();
  }

  @Nonnull
  public PsiFile getContextFile() {
    return myContextFile;
  }

  private boolean accepted(@Nonnull HighlightInfo info) {
    for (HighlightInfoFilter filter : myFilters) {
      if (!filter.accept(info, getContextFile())) return false;
    }
    return true;
  }

  @Nonnull
  public TextAttributesScheme getColorsScheme() {
    return key -> key.getDefaultAttributes();
  }

  // internal optimization method to reduce latency between creating HighlightInfo and showing it on screen
  // (Do not) call this method to
  // 1. state that all HighlightInfos in this holder are final (no further HighlightInfo.setXXX() or .registerFix() are following) and
  // 2. queue them all for converting to RangeHighlighters in EDT
  public void queueToUpdateIncrementally() {
  }
}
