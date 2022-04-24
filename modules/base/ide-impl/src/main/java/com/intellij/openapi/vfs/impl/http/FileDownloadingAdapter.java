/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.vfs.impl.http;

import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.http.event.FileDownloadingListener;

import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class FileDownloadingAdapter implements FileDownloadingListener {
  @Override
  public void fileDownloaded(final VirtualFile localFile) {
  }

  @Override
  public void errorOccurred(@Nonnull final String errorMessage) {
  }

  @Override
  public void downloadingStarted() {
  }

  @Override
  public void downloadingCancelled() {
  }

  @Override
  public void progressMessageChanged(final boolean indeterminate, @Nonnull final String message) {
  }

  @Override
  public void progressFractionChanged(final double fraction) {
  }
}
