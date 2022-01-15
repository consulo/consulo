/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.psi.codeStyle.arrangement;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * Not thread-safe.
 *
 * @author Denis Zhdanov
 * @since 7/20/12 4:53 PM
 */
public class DefaultArrangementEntry implements ArrangementEntry {

  @Nonnull
  private final List<ArrangementEntry> myChildren = new ArrayList<ArrangementEntry>();

  @javax.annotation.Nullable
  private final ArrangementEntry       myParent;
  @javax.annotation.Nullable
  private       List<ArrangementEntry> myDependencies;

  private final int     myStartOffset;
  private final int     myEndOffset;
  private final boolean myCanBeMatched;

  public DefaultArrangementEntry(@javax.annotation.Nullable ArrangementEntry parent, int startOffset, int endOffset, boolean canBeMatched) {
    myCanBeMatched = canBeMatched;
    assert startOffset < endOffset;
    myParent = parent;
    myStartOffset = startOffset;
    myEndOffset = endOffset;
  }

  @javax.annotation.Nullable
  @Override
  public ArrangementEntry getParent() {
    return myParent;
  }

  @Nonnull
  @Override
  public List<? extends ArrangementEntry> getChildren() {
    return myChildren;
  }

  public void addChild(@Nonnull ArrangementEntry entry) {
    myChildren.add(entry);
  }

  @javax.annotation.Nullable
  @Override
  public List<? extends ArrangementEntry> getDependencies() {
    return myDependencies;
  }

  public void addDependency(@Nonnull ArrangementEntry dependency) {
    if (myDependencies == null) {
      myDependencies = new ArrayList<ArrangementEntry>();
    }
    myDependencies.add(dependency);
  }

  @Override
  public int getStartOffset() {
    return myStartOffset;
  }

  @Override
  public int getEndOffset() {
    return myEndOffset;
  }

  @Override
  public boolean canBeMatched() {
    return myCanBeMatched;
  }
}
