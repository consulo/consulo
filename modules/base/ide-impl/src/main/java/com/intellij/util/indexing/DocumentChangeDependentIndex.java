// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

/**
 * Marker interface of index that use document changes to update it's data. These indices shouldn't depend on PSI-related stuff.
 * <p>
 * Note, every {@link FileBasedIndexExtension} where {@link FileBasedIndexExtension#dependsOnFileContent()} returns false is treated as document change dependent.
 */
public interface DocumentChangeDependentIndex {

}
