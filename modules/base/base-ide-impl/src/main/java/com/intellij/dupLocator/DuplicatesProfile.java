/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.intellij.dupLocator;

import com.intellij.dupLocator.treeHash.FragmentsCollector;
import com.intellij.dupLocator.util.PsiFragment;
import com.intellij.lang.Language;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.psi.PsiElement;
import com.intellij.util.indexing.FileContent;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.util.List;

public abstract class DuplicatesProfile {
  public static final ExtensionPointName<DuplicatesProfile> EP_NAME = ExtensionPointName.create("com.intellij.duplicates.profile");

  @Nonnull
  public abstract DuplocateVisitor createVisitor(@Nonnull FragmentsCollector collector);

  @Nonnull
  public DuplocateVisitor createVisitor(@Nonnull FragmentsCollector collector, boolean forIndexing) {
    return createVisitor(collector);
  }

  public abstract boolean isMyLanguage(@Nonnull Language language);

  @Nonnull
  public abstract DuplocatorState getDuplocatorState(@Nonnull Language language);

  @Nullable
  public String getComment(@Nonnull DupInfo info, int index) {
    return null;
  }

  public abstract boolean isMyDuplicate(@Nonnull DupInfo info, int index);

  public boolean supportIndex() {
    return true;
  }

  public boolean supportDuplicatesIndex() {
    return false;
  }

  public boolean acceptsContentForIndexing(FileContent fileContent) {
    return true;
  }

  private static final int FACTOR = 2;
  private static final int MAX_COST = 7000;

  public boolean shouldPutInIndex(PsiFragment fragment, int cost, DuplocatorState state) {
    final int lowerBound = state.getLowerBound();
    if (cost < FACTOR*lowerBound || cost > MAX_COST) {
      return false;
    }

    return true;
  }

  @Nullable
  public static DuplicatesProfile findProfileForLanguage(@Nonnull Language language) {
    return findProfileForLanguage(EP_NAME.getExtensionList(), language);
  }

  @Nonnull
  public static List<DuplicatesProfile> getAllProfiles() {
    return EP_NAME.getExtensionList();
  }

  @Nullable
  public static DuplicatesProfile findProfileForLanguage(List<? extends DuplicatesProfile> profiles, @Nonnull Language language) {
    for (DuplicatesProfile profile : profiles) {
      if (profile.isMyLanguage(language)) {
        return profile;
      }
    }

    return null;
  }

  @Nonnull
  public Language getLanguage(@Nonnull PsiElement element) {
    return element.getLanguage();
  }

  @Nullable
  public PsiElementRole getRole(@Nonnull PsiElement element) {
    return null;
  }
}
