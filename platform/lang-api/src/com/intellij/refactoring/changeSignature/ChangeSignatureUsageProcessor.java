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
package com.intellij.refactoring.changeSignature;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.ResolveSnapshotProvider;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Maxim.Medvedev
 */
public interface ChangeSignatureUsageProcessor {
  ExtensionPointName<ChangeSignatureUsageProcessor> EP_NAME =
    new ExtensionPointName<ChangeSignatureUsageProcessor>("com.intellij.refactoring.changeSignatureUsageProcessor");

  @NotNull
  UsageInfo[] findUsages(@NotNull ChangeInfo info);

  @NotNull
  MultiMap<PsiElement, String> findConflicts(@NotNull ChangeInfo info, Ref<UsageInfo[]> refUsages);

  boolean processUsage(@NotNull ChangeInfo changeInfo, @NotNull UsageInfo usageInfo, boolean beforeMethodChange, @NotNull UsageInfo[] usages);

  boolean processPrimaryMethod(@NotNull ChangeInfo changeInfo);

  boolean shouldPreviewUsages(@NotNull ChangeInfo changeInfo, @NotNull UsageInfo[] usages);

  void registerConflictResolvers(@NotNull List<ResolveSnapshotProvider.ResolveSnapshot> snapshots,
                                 @NotNull ResolveSnapshotProvider resolveSnapshotProvider,
                                 @NotNull UsageInfo[] usages,
                                 @NotNull ChangeInfo changeInfo);
}
