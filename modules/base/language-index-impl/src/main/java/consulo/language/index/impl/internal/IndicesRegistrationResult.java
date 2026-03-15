// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.index.io.ID;
import consulo.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class IndicesRegistrationResult {
    private final Map<ID<?, ?>, IndexState> updatedIndices = new ConcurrentHashMap<>();

    
    public String changedIndices() {
        return buildAffectedIndicesString(IndexState.VERSION_CHANGED);
    }

    
    private String buildAffectedIndicesString(IndexState state) {
        return updatedIndices.keySet().stream().filter(id -> updatedIndices.get(id) == state).map(id -> id.getName()).collect(Collectors.joining(","));
    }

    private String fullyBuiltIndices() {
        return buildAffectedIndicesString(IndexState.INITIAL_BUILD);
    }

    public void logChangedAndFullyBuiltIndices(Logger log, String changedIndicesLogMessage, String fullyBuiltIndicesLogMessage) {
        String changedIndices = changedIndices();
        if (!changedIndices.isEmpty()) {
            log.info(changedIndicesLogMessage + changedIndices);
        }
        String fullyBuiltIndices = fullyBuiltIndices();
        if (!fullyBuiltIndices.isEmpty()) {
            log.info(fullyBuiltIndicesLogMessage + fullyBuiltIndices);
        }
    }

    private enum IndexState {
        VERSION_CHANGED,
        INITIAL_BUILD
    }

    public void registerIndexAsUptoDate(ID<?, ?> index) {
        updatedIndices.remove(index);
    }

    public void registerIndexAsInitiallyBuilt(ID<?, ?> index) {
        updatedIndices.put(index, IndexState.INITIAL_BUILD);
    }

    public void registerIndexAsChanged(ID<?, ?> index) {
        updatedIndices.put(index, IndexState.VERSION_CHANGED);
    }
}