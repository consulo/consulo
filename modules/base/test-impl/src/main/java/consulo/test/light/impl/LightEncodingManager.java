/*
 * Copyright 2013-2023 consulo.io
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

import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.encoding.EncodingManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2023-11-06
 */
public abstract class LightEncodingManager implements EncodingManager {
  @Nonnull
  @Override
  public Collection<Charset> getFavorites() {
    return List.of(StandardCharsets.UTF_8);
  }

  @Override
  public boolean isNative2Ascii(@Nonnull VirtualFile virtualFile) {
    return false;
  }

  @Override
  public boolean isNative2AsciiForPropertiesFiles() {
    return false;
  }

  @Nonnull
  @Override
  public Charset getDefaultCharset() {
    return StandardCharsets.UTF_8;
  }

  @Nullable
  @Override
  public Charset getEncoding(@Nullable VirtualFile virtualFile, boolean useParentDefaults) {
    return null;
  }

  @Override
  public void setEncoding(@Nullable VirtualFile virtualFileOrDir, @Nullable Charset charset) {

  }

  @Override
  public void setNative2AsciiForPropertiesFiles(VirtualFile virtualFile, boolean native2Ascii) {

  }

  @Nonnull
  @Override
  public String getDefaultCharsetName() {
    return StandardCharsets.UTF_8.name();
  }

  @Override
  public void setDefaultCharsetName(@Nonnull String name) {

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
    return StandardCharsets.UTF_8;
  }
}
