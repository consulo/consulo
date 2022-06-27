// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.encoding;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.virtualFileSystem.VirtualFile;
import javax.annotation.Nonnull;

/**
 * Allows to overwrite project level UTF-8 BOM option for a specific virtual file.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface Utf8BomOptionProvider {
  ExtensionPointName<Utf8BomOptionProvider> EP_NAME = ExtensionPointName.create(Utf8BomOptionProvider.class);

  /**
   * @param file The file to check.
   * @return true if BOM should be added for UTF-8-encoded file.
   * @see EncodingManager#shouldAddBOMForNewUtf8File()
   */
  boolean shouldAddBOMForNewUtf8File(@Nonnull VirtualFile file);
}
