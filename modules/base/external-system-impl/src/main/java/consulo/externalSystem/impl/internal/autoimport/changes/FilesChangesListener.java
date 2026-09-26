// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.impl.internal.autoimport.changes;

import consulo.externalSystem.autoimport.ExternalSystemModificationType;
import consulo.externalSystem.impl.internal.autoimport.AutoImportProjectStatus.Stamp;

/**
 * Describes interface for listening modifications in files or documents.
 * All listener functions can be called with write/read/none contexts.
 * Call sequence of {@link #init}, {@link #onFileChange} and {@link #apply} must be called on the same thread,
 * but threads may be different for different call sequences.
 */
public interface FilesChangesListener {
    default void init() {
    }

    default void onFileChange(Stamp stamp, String path, long modificationStamp, ExternalSystemModificationType modificationType) {
    }

    default void apply() {
    }
}
