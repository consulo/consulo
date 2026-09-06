// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal;

public record UnindexedFileStatus(
    boolean shouldIndex,
    boolean indexesWereProvidedByInfrastructureExtension,
    long timeProcessingUpToDateFiles,
    long timeUpdatingContentLessIndexes,
    long timeIndexingWithoutContentViaInfrastructureExtension,
    long timeTotal
) {
    public boolean wasFullyIndexedByInfrastructureExtension() {
        return !shouldIndex && indexesWereProvidedByInfrastructureExtension;
    }
}
