// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.usages;

import com.intellij.usageView.UsageInfo;
import com.intellij.usages.rules.MergeableUsage;

import javax.annotation.Nonnull;

public interface UsageInfoAdapter extends Usage, MergeableUsage {
  @Nonnull
  String getPath();

  int getLine();

  int getNavigationOffset();

  @Nonnull
  UsageInfo[] getMergedInfos();
}
