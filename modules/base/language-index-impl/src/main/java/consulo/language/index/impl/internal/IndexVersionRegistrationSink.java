// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.index.io.ID;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class IndexVersionRegistrationSink {
    private final Map<ID<?, ?>, IndexVersion.IndexVersionDiff> myIndexVersionDiffs = new ConcurrentHashMap<>();

    public boolean hasChangedIndexes() {
        return ContainerUtil.find(myIndexVersionDiffs.values(), diff -> isRebuildRequired(diff)) != null;
    }

    public boolean hasNewIndexes() {
        return ContainerUtil.find(myIndexVersionDiffs.values(), diff -> diff instanceof IndexVersion.IndexVersionDiff.InitialBuild) != null;
    }

    public String changedIndices() {
        return buildString(diff -> isRebuildRequired(diff));
    }

    public void logChangedAndFullyBuiltIndices(Logger log, String changedIndicesLogMessage, String fullyBuiltIndicesLogMessage) {
        String changedIndices = changedIndices();
        if (!changedIndices.isEmpty()) {
            log.info(changedIndicesLogMessage + changedIndices);
        }
        String fullyBuiltIndices = initiallyBuiltIndices();
        if (!fullyBuiltIndices.isEmpty()) {
            log.info(fullyBuiltIndicesLogMessage + fullyBuiltIndices);
        }
    }

    private String buildString(Predicate<? super IndexVersion.IndexVersionDiff> condition) {
        return myIndexVersionDiffs
            .entrySet()
            .stream()
            .filter(e -> condition.test(e.getValue()))
            .map(e -> e.getKey().getName() + e.getValue().getLogText())
            .collect(Collectors.joining(","));
    }

    private String initiallyBuiltIndices() {
        return buildString(diff -> diff instanceof IndexVersion.IndexVersionDiff.InitialBuild);
    }

    public <K, V> void setIndexVersionDiff(ID<K, V> name, IndexVersion.IndexVersionDiff diff) {
        myIndexVersionDiffs.put(name, diff);
    }

    private static boolean isRebuildRequired(IndexVersion.IndexVersionDiff diff) {
        return diff instanceof IndexVersion.IndexVersionDiff.CorruptedRebuild ||
            diff instanceof IndexVersion.IndexVersionDiff.VersionChanged;
    }
}
