/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.process.local;

import consulo.annotation.DeprecationInfo;
import consulo.container.boot.ContainerPathManager;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandlerBuilder;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.internal.LocalProcessHandler;
import consulo.util.io.CharsetToolkit;
import consulo.util.io.FileUtil;
import org.jspecify.annotations.Nullable;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;

@Deprecated
@DeprecationInfo("Use CapturingProcessUtil, or GeneralCommandLine#withSudo etc")
public class ExecUtil {
  private ExecUtil() {
  }

  
  public static String loadTemplate(ClassLoader loader,
                                    String templateName,
                                    @Nullable Map<String, String> variables) throws IOException {
    @SuppressWarnings("IOResourceOpenedButNotSafelyClosed") InputStream stream = loader.getResourceAsStream(templateName);
    if (stream == null) {
      throw new IOException("Template '" + templateName + "' not found by " + loader);
    }

    String template = FileUtil.loadTextAndClose(new InputStreamReader(stream, CharsetToolkit.UTF8));
    if (variables == null || variables.size() == 0) {
      return template;
    }

    StringBuilder buffer = new StringBuilder(template);
    for (Map.Entry<String, String> var : variables.entrySet()) {
      String name = var.getKey();
      int pos = buffer.indexOf(name);
      if (pos >= 0) {
        buffer.replace(pos, pos + name.length(), var.getValue());
      }
    }
    return buffer.toString();
  }

  
  public static File createTempExecutableScript(String prefix,
                                                String suffix,
                                                String content) throws IOException, ExecutionException {
    File tempDir = new File(ContainerPathManager.get().getTempPath());
    File tempFile = FileUtil.createTempFile(tempDir, prefix, suffix, true, true);
    FileUtil.writeToFile(tempFile, content.getBytes(CharsetToolkit.UTF8));
    if (!tempFile.setExecutable(true, true)) {
      throw new ExecutionException("Failed to make temp file executable: " + tempFile);
    }
    return tempFile;
  }

  
  public static String getOpenCommandPath() {
    return "/usr/bin/open";
  }

  
  public static String getWindowsShellName() {
    return "cmd.exe";
  }

  public static @Nullable String execAndReadLine(GeneralCommandLine commandLine) {
    try {
      return readFirstLine(commandLine.createProcess().getInputStream(), commandLine.getCharset());
    }
    catch (ExecutionException ignored) {
      return null;
    }
  }

  public static @Nullable String readFirstLine(InputStream stream, @Nullable Charset cs) {
    try {
      try (BufferedReader reader = new BufferedReader(cs == null ? new InputStreamReader(stream) : new InputStreamReader(stream, cs))) {
        return reader.readLine();
      }
    }
    catch (IOException ignored) {
      return null;
    }
  }

  
  @Deprecated
  @DeprecationInfo("Use GeneralCommandLine#withSudo()")
  public static Process sudo(GeneralCommandLine commandLine, String prompt) throws ExecutionException, IOException {
    LocalProcessHandler handler = (LocalProcessHandler)ProcessHandlerBuilder.create(commandLine.withSudo(prompt)).build();
    return handler.getProcess();
  }
}