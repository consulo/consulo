/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.vfs.encoding;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.vfs.VirtualFile;
import kava.beans.PropertyChangeListener;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.util.Collection;

/**
 * @author cdr
 */
public abstract class EncodingManager extends EncodingRegistry {
  @NonNls public static final String PROP_NATIVE2ASCII_SWITCH = "native2ascii";
  @NonNls public static final String PROP_PROPERTIES_FILES_ENCODING = "propertiesFilesEncoding";

  @Nonnull
  public static EncodingManager getInstance() {
    return ServiceManager.getService(EncodingManager.class);
  }

  @Nonnull
  public abstract Collection<Charset> getFavorites();

  @Override
  public abstract boolean isNative2AsciiForPropertiesFiles();

  public abstract void setNative2AsciiForPropertiesFiles(VirtualFile virtualFile, boolean native2Ascii);

  @Nonnull
  // returns empty for system default
  public abstract String getDefaultCharsetName();

  public void setDefaultCharsetName(@Nonnull String name) {
    throw new UnsupportedOperationException("Not implemented");
  }

  /**
   * @return null for system-default
   */
  @Override
  @Nullable
  public abstract Charset getDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile);
  public abstract void setDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile, @Nullable Charset charset);

  public abstract void addPropertyChangeListener(@Nonnull PropertyChangeListener listener, @Nonnull Disposable parentDisposable);

  @Nullable
  public abstract Charset getCachedCharsetFromContent(@Nonnull Document document);

  public boolean shouldAddBOMForNewUtf8File() {
    return false;
  }
}
