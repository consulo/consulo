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
package com.intellij.usages;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Key;
import com.intellij.psi.search.SearchScope;
import com.intellij.usageView.UsageInfo;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import javax.swing.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author max
 */
public interface UsageView extends Disposable {
  /**
   * Returns {@link com.intellij.usages.UsageTarget} to look usages for
   */
  Key<UsageTarget[]> USAGE_TARGETS_KEY = Key.create("usageTarget");
  /**
   * Returns {@link com.intellij.usages.Usage} which are selected in usage view
   */
  Key<Usage[]> USAGES_KEY = Key.create("usages");
  Key<UsageView> USAGE_VIEW_KEY = Key.create("UsageView.new");
  Key<UsageInfo> USAGE_INFO_KEY = Key.create("UsageInfo");
  Key<SearchScope> USAGE_SCOPE = Key.create("UsageScope");
  Key<List<UsageInfo>> USAGE_INFO_LIST_KEY = Key.create("UsageInfo.List");

  void appendUsage(@Nonnull Usage usage);
  void removeUsage(@Nonnull Usage usage);
  void includeUsages(@Nonnull Usage[] usages);
  void excludeUsages(@Nonnull Usage[] usages);
  void selectUsages(@Nonnull Usage[] usages);

  void close();
  boolean isSearchInProgress();

  /**
   * @deprecated please specify mnemonic by prefixing the mnenonic character with an ampersand (&& for Mac-specific ampersands)
   */
  void addButtonToLowerPane(@Nonnull Runnable runnable, @Nonnull String text, char mnemonic);
  void addButtonToLowerPane(@Nonnull Runnable runnable, @Nonnull String text);

  void setAdditionalComponent(@Nullable JComponent component);

  void addPerformOperationAction(@Nonnull Runnable processRunnable, String commandName, String cannotMakeString, @Nonnull String shortDescription);

  /**
   * @param checkReadOnlyStatus if false, check is performed inside processRunnable
   */
  void addPerformOperationAction(@Nonnull Runnable processRunnable, String commandName, String cannotMakeString, @Nonnull String shortDescription, boolean checkReadOnlyStatus);

  @Nonnull
  UsageViewPresentation getPresentation();

  @Nonnull
  Set<Usage> getExcludedUsages();

  @Nullable
  Set<Usage> getSelectedUsages();
  @Nonnull
  Set<Usage> getUsages();
  @Nonnull
  List<Usage> getSortedUsages();

  @Nonnull
  JComponent getComponent();

  int getUsagesCount();

  /**
   * Removes all specified usages from the usage view in one heroic swoop.
   * Reloads the whole tree model once instead of firing individual remove event for each node.
   * Useful for processing huge number of usages faster, e.g. during "find in path/replace all".
   */
  void removeUsagesBulk(@Nonnull Collection<Usage> usages);
}
