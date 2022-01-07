// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.util.containers.ContainerUtil;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

import java.util.Map;
import java.util.stream.Collectors;

public class IndicesRegistrationResult {
  private final Map<ID<?, ?>, IndexState> updatedIndices = ContainerUtil.newConcurrentMap();

  @Nonnull
  public String changedIndices() {
    return buildAffectedIndicesString(IndexState.VERSION_CHANGED);
  }

  @Nonnull
  private String buildAffectedIndicesString(IndexState state) {
    return updatedIndices.keySet().stream().filter(id -> updatedIndices.get(id) == state).map(id -> id.getName()).collect(Collectors.joining(","));
  }

  private String fullyBuiltIndices() {
    return buildAffectedIndicesString(IndexState.INITIAL_BUILD);
  }

  public void logChangedAndFullyBuiltIndices(@Nonnull Logger log, @Nonnull String changedIndicesLogMessage, @Nonnull String fullyBuiltIndicesLogMessage) {
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

  public void registerIndexAsUptoDate(@Nonnull ID<?, ?> index) {
    updatedIndices.remove(index);
  }

  public void registerIndexAsInitiallyBuilt(@Nonnull ID<?, ?> index) {
    updatedIndices.put(index, IndexState.INITIAL_BUILD);
  }

  public void registerIndexAsChanged(@Nonnull ID<?, ?> index) {
    updatedIndices.put(index, IndexState.VERSION_CHANGED);
  }
}