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
package consulo.ide.impl.idea.openapi.vfs.impl.http;

import consulo.ide.IdeBundle;
import consulo.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VfsBundle;
import consulo.virtualFileSystem.VirtualFile;
import consulo.application.util.UrlConnectionUtil;
import consulo.virtualFileSystem.http.RemoteContentProvider;

import jakarta.annotation.Nonnull;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author nik
 */
public class DefaultRemoteContentProvider extends RemoteContentProvider {
  private static final Logger LOG = Logger.getInstance(DefaultRemoteContentProvider.class);
  private static final int CONNECT_TIMEOUT = 60 * 1000;
  private static final int READ_TIMEOUT = 60 * 1000;

  @Override
  public boolean canProvideContent(@Nonnull final String url) {
    return true;
  }

  @Override
  public void saveContent(final String url, @Nonnull final File file, @Nonnull final DownloadingCallback callback) {
    ApplicationManager.getApplication().executeOnPooledThread(new Runnable() {
      @Override
      public void run() {
        downloadContent(url, file, callback);
      }
    });
  }

  private static void downloadContent(final String url, final File file, final DownloadingCallback callback) {
    LOG.debug("Downloading started: " + url);
    InputStream input = null;
    OutputStream output = null;
    try {
      String presentableUrl = StringUtil.first(url, 40, true);
      callback.setProgressText(VfsBundle.message("download.progress.connecting", presentableUrl), true);
      HttpURLConnection connection = (HttpURLConnection)new URL(url).openConnection();
      connection.setConnectTimeout(CONNECT_TIMEOUT);
      connection.setReadTimeout(READ_TIMEOUT);
      input = UrlConnectionUtil.getConnectionInputStreamWithException(connection, new EmptyProgressIndicator());

      final int responseCode = connection.getResponseCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
        throw new IOException(IdeBundle.message("error.connection.failed.with.http.code.N", responseCode));
      }

      final int size = connection.getContentLength();
      output = new BufferedOutputStream(new FileOutputStream(file));
      callback.setProgressText(VfsBundle.message("download.progress.downloading", presentableUrl), size == -1);
      if (size != -1) {
        callback.setProgressFraction(0);
      }
      String contentType = connection.getContentType();
      FileType fileType = RemoteFileUtil.getFileType(contentType);

      int len;
      final byte[] buf = new byte[1024];
      int count = 0;
      while ((len = input.read(buf)) > 0) {
        if (callback.isCancelled()) {
          return;
        }
        count += len;
        if (size > 0) {
          callback.setProgressFraction((double)count / size);
        }
        output.write(buf, 0, len);
      }
      output.close();
      output = null;
      LOG.debug("Downloading finished, " + size + " bytes downloaded");
      callback.finished(fileType);
    }
    catch (IOException e) {
      LOG.info(e);
      callback.errorOccurred(VfsBundle.message("cannot.load.remote.file", url, e.getMessage()), false);
    }
    finally {
      if (input != null) {
        try {
          input.close();
        }
        catch (IOException e) {
          LOG.info(e);
        }
      }
      if (output != null) {
        try {
          output.close();
        }
        catch (IOException e) {
          LOG.info(e);
        }
      }
    }
  }

  @Override
  public boolean isUpToDate(@Nonnull final String url, @Nonnull final VirtualFile local) {
    return false;
  }
}
