// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.provided;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.indexing.FileBasedIndexExtension;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Objects;

public interface ProvidedIndexExtensionLocator {
  ExtensionPointName<ProvidedIndexExtensionLocator> EP_NAME = ExtensionPointName.create("com.intellij.fileBasedIndex.providedLocator");

  @Nullable
  <K, V> ProvidedIndexExtension<K, V> findProvidedIndexExtension(@Nonnull FileBasedIndexExtension<K, V> originalExtension);

  @Nullable
  static <K, V> ProvidedIndexExtension<K, V> findProvidedIndexExtensionFor(@Nonnull FileBasedIndexExtension<K, V> originalExtension) {
    return EP_NAME.getExtensionList().stream().map(ex -> ex.findProvidedIndexExtension(originalExtension)).filter(Objects::nonNull).findFirst().orElse(null);
  }
}
