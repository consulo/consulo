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

package consulo.application.util;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author nik
 */
public class UrlConnectionUtil {
  private UrlConnectionUtil() {
  }

  @Nullable
  public static InputStream getConnectionInputStream(URLConnection connection, ProgressIndicator pi) {
    try {
      return getConnectionInputStreamWithException(connection, pi);
    }
    catch (ProcessCanceledException e) {
      return null;
    }
    catch (IOException e) {
      return null;
    }
  }

  public static InputStream getConnectionInputStreamWithException(URLConnection connection, ProgressIndicator pi) throws IOException {
    InputStreamGetter getter = new InputStreamGetter(connection);
    final Future<?> getterFuture = Application.get().executeOnPooledThread(getter);

    while (true) {
      pi.checkCanceled();
      try {
        try {
          getterFuture.get(50, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException ignored) {
        }

        pi.setIndeterminate(true);
        pi.setText(pi.getText());
        if (getterFuture.isDone()) break;
      }
      catch (Exception e) {
        throw new ProcessCanceledException(e);
      }
    }
    if (getter.getException() != null) {
      throw getter.getException();
    }

    return getter.getInputStream();
  }

  private static class InputStreamGetter implements Runnable {
    private InputStream myInputStream;
    private final URLConnection myUrlConnection;
    private IOException myException;

    public InputStreamGetter(URLConnection urlConnection) {
      myUrlConnection = urlConnection;
    }

    public IOException getException() {
      return myException;
    }

    public InputStream getInputStream() {
      return myInputStream;
    }

    @Override
    public void run() {
      try {
        myInputStream = myUrlConnection.getInputStream();
      }
      catch (IOException e) {
        myException = e;
        myInputStream = null;
      }
      catch (Exception e) {
        myException = new IOException();
        myException.initCause(e);
        myInputStream = null;
      }
    }
  }
}
