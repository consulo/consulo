/*
 * Copyright 2013-2022 consulo.io
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
package consulo.virtualFileSystem.http;

import consulo.virtualFileSystem.http.event.FileDownloadingListener;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 24-Apr-22
 */
public interface RemoteFileInfo extends RemoteContentProvider.DownloadingCallback {
  void addDownloadingListener(@Nonnull FileDownloadingListener listener);

  void removeDownloadingListener(@Nonnull FileDownloadingListener listener);

  String getUrl();

  RemoteFileState getState();

  void cancelDownloading();

  void startDownloading();

  void restartDownloading();

  String getErrorMessage();
}
