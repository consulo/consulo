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
package consulo.usage;

import consulo.application.ApplicationManager;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.util.function.Processor;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.util.lang.ref.Ref;

import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

public abstract class UsageInfoSearcherAdapter implements UsageSearcher {
    protected void processUsages(final @Nonnull Processor<Usage> processor, @Nonnull Project project) {
        final Ref<UsageInfo[]> refUsages = new Ref<UsageInfo[]>();
        final Ref<Boolean> dumbModeOccurred = new Ref<Boolean>();
        ApplicationManager.getApplication().runReadAction(() -> {
            try {
                refUsages.set(findUsages());
            }
            catch (IndexNotReadyException e) {
                dumbModeOccurred.set(true);
            }
        });
        if (!dumbModeOccurred.isNull()) {
            DumbService.getInstance(project).showDumbModeNotification("Usage search is not available until indices are ready");
            return;
        }
        final Usage[] usages =
            ApplicationManager.getApplication().runReadAction((Supplier<Usage[]>)() -> UsageInfo2UsageAdapter.convert(refUsages.get()));

        for (final Usage usage : usages) {
            ApplicationManager.getApplication().runReadAction(() -> {
                processor.process(usage);
            });
        }
    }

    protected abstract UsageInfo[] findUsages();
}
