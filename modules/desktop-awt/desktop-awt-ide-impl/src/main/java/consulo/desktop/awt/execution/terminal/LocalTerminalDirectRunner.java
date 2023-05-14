/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.execution.terminal;

import com.jediterm.terminal.TtyConnector;
import com.pty4j.PtyProcess;
import com.pty4j.util.PtyUtil;
import consulo.logging.Logger;
import consulo.platform.Platform;

import jakarta.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * @author traff
 */
public class LocalTerminalDirectRunner extends AbstractTerminalRunner<PtyProcess> {
  private static final Logger LOG = Logger.getInstance(LocalTerminalDirectRunner.class);

  private final Charset myDefaultCharset;
  private final String myConnectorName;
  private final Supplier<String> myShellPathGetter;
  private final String myWorkingDirectory;

  public LocalTerminalDirectRunner(String connectorName, String workDirectory, Supplier<String> shellPathGetter) {
    myConnectorName = connectorName;
    myDefaultCharset = StandardCharsets.UTF_8;
    myShellPathGetter = shellPathGetter;
    myWorkingDirectory = workDirectory;
  }

  public String getWorkingDirectory() {
    return myWorkingDirectory;
  }

  private static boolean hasLoginArgument(String name) {
    return name.equals("bash") || name.equals("sh") || name.equals("zsh");
  }

  private static String getShellName(String path) {
    return new File(path).getName();
  }

  private static File findRCFile() {
    try {
      final String folder = PtyUtil.getPtyLibFolderPath();
      if (folder != null) {
        File rcFile = new File(folder, "jediterm.in");
        if (rcFile.exists()) {
          return rcFile;
        }
      }
    }
    catch (Exception e) {
      LOG.warn("Unable to get JAR folder", e);
    }
    return null;
  }

  @Override
  protected PtyProcess createProcess(@Nonnull String directory) throws ExecutionException {
    Map<String, String> envs = new HashMap<>(Platform.current().os().environmentVariables());
    envs.put("TERM", "xterm-256color");
    //EncodingEnvironmentUtil.setLocaleEnvironmentIfMac(envs, myDefaultCharset);
    try {
      return PtyProcess.exec(getCommand(), envs, directory);
    }
    catch (IOException e) {
      throw new ExecutionException(e);
    }
  }

  @Override
  protected TtyConnector createTtyConnector(PtyProcess process) {
    return new PtyProcessTtyConnector(process, myDefaultCharset, myConnectorName);
  }

  public String[] getCommand() {
    String[] command;
    String shellPath = myShellPathGetter.get();

    if (Platform.current().os().isUnix()) {
      File rcFile = findRCFile();

      String shellName = getShellName(shellPath);

      if (rcFile != null && (shellName.equals("bash") || shellName.equals("sh"))) {
        command = new String[]{
          shellPath,
          "--rcfile",
          rcFile.getAbsolutePath(),
          "-i"
        };
      }
      else if (hasLoginArgument(shellName)) {
        command = new String[]{
          shellPath,
          "--login"
        };
      }
      else {
        command = shellPath.split(" ");
      }
    }
    else {
      command = new String[]{shellPath};
    }

    return command;
  }

}
