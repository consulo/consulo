/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light.impl;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.encoding.EncodingProjectManager;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightEncodingProjectManager extends EncodingProjectManager {
  @Override
  public boolean isNative2Ascii(@Nonnull VirtualFile virtualFile) {
    return false;
  }

  @Nonnull
  @Override
  public Charset getDefaultCharset() {
    return CharsetToolkit.getDefaultSystemCharset();
  }

  @Override
  public Charset getEncoding(@Nullable VirtualFile virtualFile, boolean useParentDefaults) {
    return getDefaultCharset();
  }

  @Override
  public void setEncoding(@Nullable VirtualFile virtualFileOrDir, @Nullable Charset charset) {
  }

  @Override
  public boolean isNative2AsciiForPropertiesFiles() {
    return false;
  }

  @Nonnull
  @Override
  public Collection<Charset> getFavorites() {
    return Collections.singletonList(CharsetToolkit.UTF8_CHARSET);
  }

  @Override
  public void setNative2AsciiForPropertiesFiles(VirtualFile virtualFile, boolean native2Ascii) {

  }

  @Nonnull
  @Override
  public String getDefaultCharsetName() {
    return getDefaultCharset().name();
  }

  @Nullable
  @Override
  public Charset getDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile) {
    return null;
  }

  @Override
  public void setDefaultCharsetForPropertiesFiles(@Nullable VirtualFile virtualFile, @Nullable Charset charset) {

  }

  @Nonnull
  @Override
  public Charset getDefaultConsoleEncoding() {
    return Charset.defaultCharset();
  }

  @Nullable
  @Override
  public Charset getCachedCharsetFromContent(@Nonnull Document document) {
    return null;
  }

  @Override
  public void setDefaultCharsetName(@Nonnull String name) {

  }
}
