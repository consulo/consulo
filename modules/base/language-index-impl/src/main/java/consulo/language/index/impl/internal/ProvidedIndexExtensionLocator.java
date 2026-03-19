// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.index.impl.internal.provided.ProvidedIndexExtension;
import consulo.language.psi.stub.FileBasedIndexExtension;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface ProvidedIndexExtensionLocator {
    ExtensionPointName<ProvidedIndexExtensionLocator> EP_NAME = ExtensionPointName.create(ProvidedIndexExtensionLocator.class);

    @Nullable
    <K, V> ProvidedIndexExtension<K, V> findProvidedIndexExtension(FileBasedIndexExtension<K, V> originalExtension);

    static @Nullable <K, V> ProvidedIndexExtension<K, V> findProvidedIndexExtensionFor(FileBasedIndexExtension<K, V> originalExtension) {
        return EP_NAME.getExtensionList()
            .stream()
            .map(ex -> ex.findProvidedIndexExtension(originalExtension))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }
}
