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
package consulo.language.editor.refactoring.changeSignature;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.editor.refactoring.ResolveSnapshotProvider;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.usage.UsageInfo;
import consulo.util.collection.MultiMap;

import consulo.util.lang.ref.SimpleReference;

import java.util.List;

/**
 * @author Maxim.Medvedev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface ChangeSignatureUsageProcessor {
    ExtensionPointName<ChangeSignatureUsageProcessor> EP_NAME = ExtensionPointName.create(ChangeSignatureUsageProcessor.class);

    
    UsageInfo[] findUsages(ChangeInfo info);

    
    MultiMap<PsiElement, LocalizeValue> findConflicts(ChangeInfo info, SimpleReference<UsageInfo[]> refUsages);

    boolean processUsage(
        ChangeInfo changeInfo,
        UsageInfo usageInfo,
        boolean beforeMethodChange,
        UsageInfo[] usages
    );

    boolean processPrimaryMethod(ChangeInfo changeInfo);

    boolean shouldPreviewUsages(ChangeInfo changeInfo, UsageInfo[] usages);

    void registerConflictResolvers(
        List<ResolveSnapshotProvider.ResolveSnapshot> snapshots,
        ResolveSnapshotProvider resolveSnapshotProvider,
        UsageInfo[] usages,
        ChangeInfo changeInfo
    );
}
