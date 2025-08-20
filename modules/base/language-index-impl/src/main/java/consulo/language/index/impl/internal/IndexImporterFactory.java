// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.index.io.IndexExtension;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface IndexImporterFactory {
    ExtensionPointName<IndexImporterFactory> EP_NAME = ExtensionPointName.create(IndexImporterFactory.class);

    @Nullable
    <Key, Value, Input> SnapshotInputMappingIndex<Key, Value, Input> createImporter(@Nonnull IndexExtension<Key, Value, Input> extension);
}
