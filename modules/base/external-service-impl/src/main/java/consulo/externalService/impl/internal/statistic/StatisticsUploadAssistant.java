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
package consulo.externalService.impl.internal.statistic;

import consulo.application.Application;
import consulo.externalService.statistic.CollectUsagesException;
import consulo.externalService.statistic.UsageDescriptor;
import consulo.externalService.statistic.UsagesCollector;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.function.Condition;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class StatisticsUploadAssistant {

    private static final Logger LOG = Logger.getInstance(StatisticsUploadAssistant.class);

    @Nonnull
    public static Map<String, Set<PatchedUsage>> getPatchedUsages(
        @Nonnull final Map<String, Set<UsageDescriptor>> allUsages,
        final Map<String, Set<UsageDescriptor>> sentUsageMap
    ) {
        Map<String, Set<PatchedUsage>> patchedUsages = mapToPatchedUsagesMap(allUsages);

        for (Map.Entry<String, Set<UsageDescriptor>> sentUsageEntry : sentUsageMap.entrySet()) {
            final String sentUsageGroupDescriptor = sentUsageEntry.getKey();

            final Set<UsageDescriptor> sentUsages = sentUsageEntry.getValue();

            for (UsageDescriptor sentUsage : sentUsages) {
                final PatchedUsage descriptor = findDescriptor(patchedUsages, Pair.create(sentUsageGroupDescriptor, sentUsage.getKey()));
                if (descriptor == null) {
                    if (!patchedUsages.containsKey(sentUsageGroupDescriptor)) {
                        patchedUsages.put(sentUsageGroupDescriptor, new LinkedHashSet<>());
                    }
                    patchedUsages.get(sentUsageGroupDescriptor).add(new PatchedUsage(sentUsage.getKey(), -sentUsage.getValue()));
                }
                else {
                    descriptor.subValue(sentUsage.getValue());
                }
            }

        }

        return packCollection(patchedUsages, patchedUsage -> patchedUsage.getDelta() != 0);
    }

    private static Map<String, Set<PatchedUsage>> mapToPatchedUsagesMap(Map<String, Set<UsageDescriptor>> allUsages) {
        Map<String, Set<PatchedUsage>> patchedUsages = new LinkedHashMap<>();
        for (Map.Entry<String, Set<UsageDescriptor>> entry : allUsages.entrySet()) {
            patchedUsages.put(entry.getKey(), new HashSet<>(ContainerUtil.map2Set(entry.getValue(), PatchedUsage::new)));
        }
        return patchedUsages;
    }

    @Nonnull
    private static Map<String, Set<PatchedUsage>> packCollection(
        @Nonnull Map<String, Set<PatchedUsage>> patchedUsages,
        Condition<PatchedUsage> condition
    ) {
        Map<String, Set<PatchedUsage>> result = new LinkedHashMap<>();
        for (String descriptor : patchedUsages.keySet()) {
            final Set<PatchedUsage> usages = packCollection(patchedUsages.get(descriptor), condition);
            if (usages.size() > 0) {
                result.put(descriptor, usages);
            }
        }

        return result;
    }

    @Nonnull
    private static <T> Set<T> packCollection(@Nonnull Collection<T> set, @Nonnull Condition<T> condition) {
        final Set<T> result = new LinkedHashSet<>();
        for (T t : set) {
            if (condition.value(t)) {
                result.add(t);
            }
        }
        return result;
    }

    @Nullable
    public static <T extends UsageDescriptor> T findDescriptor(
        @Nonnull Map<String, Set<T>> descriptors,
        @Nonnull final Pair<String, String> id
    ) {
        final Set<T> usages = descriptors.get(id.getFirst());
        if (usages == null) {
            return null;
        }

        return ContainerUtil.find(usages, t -> id.getSecond().equals(t.getKey()));
    }

    @Nonnull
    public static Map<String, Set<UsageDescriptor>> getAllUsages(@Nullable Project project, @Nonnull Set<String> disabledGroups) {
        Map<String, Set<UsageDescriptor>> usageDescriptors = new LinkedHashMap<>();

        Application.get().getExtensionPoint(UsagesCollector.class).forEachExtensionSafe(usagesCollector -> {
            final String groupDescriptor = usagesCollector.getGroupId();

            if (!disabledGroups.contains(groupDescriptor)) {
                try {
                    final Set<UsageDescriptor> usages = usagesCollector.getUsages(project);
                    usageDescriptors.put(groupDescriptor, usages);
                }
                catch (CollectUsagesException e) {
                    LOG.info(e);
                }
            }
        });

        return usageDescriptors;
    }
}
