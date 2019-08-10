/*
 * Copyright 2013-2017 consulo.io
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
package consulo.startup;

import com.intellij.openapi.application.PathManager;
import com.intellij.util.ExceptionUtil;
import consulo.logging.Logger;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author VISTALL
 * @since 27-May-17
 */
public class StartupActionLogger implements Logger, Closeable {
  public static boolean ourPrintToConsole = false;

  private final StringBuilder myBuilder = new StringBuilder();

  @Override
  public boolean isDebugEnabled() {
    return false;
  }

  @Override
  public void debug(@NonNls String message) {

  }

  @Override
  public void debug(@Nullable Throwable t) {

  }

  @Override
  public void debug(@NonNls String message, @Nullable Throwable t) {

  }

  @Override
  public void info(@NonNls String message) {
    print("INFO: " + message, false);
  }

  @Override
  public void info(@NonNls String message, @Nullable Throwable t) {
    print("INFO: " + message, false);

    if (t != null) {
      print(ExceptionUtil.getThrowableText(t), false);
    }
  }

  @Override
  public void warn(@NonNls String message, @Nullable Throwable t) {
    print("WARN: " + message, false);

    if (t != null) {
      print(ExceptionUtil.getThrowableText(t), false);
    }
  }

  @Override
  public void error(@NonNls String message, @Nullable Throwable t, @NonNls @Nonnull String... details) {
    print("ERROR: " + message, false);

    if (t != null) {
      print(ExceptionUtil.getThrowableText(t), true);
    }
  }

  @Override
  public void close() throws IOException {
    String systemPath = PathManager.getPluginTempPath();

    File file = new File(systemPath, "start.log");

    FileWriter logFile = new FileWriter(file, false);

    logFile.append(myBuilder);

    logFile.close();
  }

  private void print(String str, boolean err) {
    if(ourPrintToConsole) {
      if (err) {
        System.err.println(str);
      }
      else {
        System.out.println(str);
      }
    }

    myBuilder.append(str).append("\n");
  }
}
