// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.encoding;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.ComponentManager;
import consulo.component.util.ModificationTracker;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.nio.charset.Charset;
import java.util.Map;

@ServiceAPI(ComponentScope.PROJECT)
public interface EncodingProjectManager extends EncodingManager {
  public static EncodingProjectManager getInstance(@Nonnull ComponentManager project) {
    return project.getInstance(EncodingProjectManager.class);
  }

  /**
   * @return Project encoding name (configured in Settings|File Encodings|Project Encoding) or empty string if it's configured to "System Default"
   */
  @Nonnull
  @Override
  String getDefaultCharsetName();

  /**
   * @return Project encoding (configured in Settings|File Encodings|Project Encoding)
   */
  @Nonnull
  @Override
  Charset getDefaultCharset();

  /**
   * Sets Project encoding (configured in Settings|File Encodings|Project Encoding). Use empty string to specify "System Default"
   */
  @Override
  void setDefaultCharsetName(@Nonnull String name);

  @Nonnull
  ModificationTracker getModificationTracker();

  /**
   * @return readonly map of current mappings. to modify mappings use {@link #setMapping(Map)}
   */
  @Nonnull
  Map<? extends VirtualFile, ? extends Charset> getAllMappings();
}
