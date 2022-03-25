// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.impl.cache.impl.id;

import consulo.index.io.DataIndexer;
import consulo.language.psi.stub.FileContent;

/**
 * @author traff
 */
public interface IdIndexer extends DataIndexer<IdIndexEntry, Integer, FileContent> {
  default int getVersion() {
    return 1;
  }
}
