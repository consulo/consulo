// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.encoding;

import consulo.annotation.DeprecationInfo;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.charset.Charset;
import java.util.Collection;

/**
 * @see ApplicationEncodingManager
 * @see EncodingProjectManager
 */
public interface EncodingManager extends EncodingRegistry {
  public static final String PROP_NATIVE2ASCII_SWITCH = "native2ascii";
  public static final String PROP_PROPERTIES_FILES_ENCODING = "propertiesFilesEncoding";

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use ApplicationEncodingManager class")
  public static EncodingManager getInstance() {
    return ApplicationEncodingManager.getInstance();
  }

  @Nonnull
  Collection<Charset> getFavorites();

  @Override
  boolean isNative2AsciiForPropertiesFiles();

  void setNative2AsciiForPropertiesFiles(VirtualFile virtualFile, boolean native2Ascii);

  /**
   * @return returns empty for system default
   */
  @Nonnull
  String getDefaultCharsetName();

  void setDefaultCharsetName(@Nonnull String name);

  /**
   * @return null for system-default
   */
  @Override
  @Nullable
  Charset getDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile);

  void setDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile, @Nullable Charset charset);

  /**
   * @return encoding used by default in {@link GeneralCommandLine}
   */
  @Override
  @Nonnull
  Charset getDefaultConsoleEncoding();

  default boolean shouldAddBOMForNewUtf8File() {
    return false;
  }
}
