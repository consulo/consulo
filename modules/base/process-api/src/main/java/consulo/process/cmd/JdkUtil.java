/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.process.cmd;

import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.util.io.CharsetToolkit;
import consulo.virtualFileSystem.encoding.EncodingManager;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

/**
 * @author max
 */
class JdkUtil {
  private static final Logger LOG = Logger.getInstance(JdkUtil.class);

  private JdkUtil() {
  }

  public static GeneralCommandLine setupJVMCommandLine(final String jdkHome, final SimpleJavaParameters javaParameters) {
    final GeneralCommandLine commandLine = new GeneralCommandLine();
    commandLine.setExePath(getBinPath(jdkHome) + File.separator + (Platform.current().os().isWindows() ? "java.exe" : "java"));

    final ParametersList vmParametersList = javaParameters.getVMParametersList();
    commandLine.getEnvironment().putAll(javaParameters.getEnv());
    commandLine.setPassParentEnvironment(javaParameters.isPassParentEnvs());

    commandLine.addParameters(vmParametersList.getList());

    appendEncoding(javaParameters, commandLine, vmParametersList);

    if (!vmParametersList.hasParameter("-classpath") && !vmParametersList.hasParameter("-cp")) {
      commandLine.addParameter("-classpath");
      commandLine.addParameter(javaParameters.getClassPath().getPathsString());
    }

    final String mainClass = javaParameters.getMainClass();
    commandLine.addParameter(mainClass);
    commandLine.addParameters(javaParameters.getProgramParametersList().getList());

    commandLine.setWorkDirectory(javaParameters.getWorkingDirectory());

    return commandLine;
  }

  private static String getBinPath(String sdk) {
    return getConvertedHomePath(sdk) + "bin";
  }

  private static String getConvertedHomePath(String sdk) {
    String path = sdk.replace('/', File.separatorChar);
    if (!path.endsWith(File.separator)) {
      path += File.separator;
    }
    return path;
  }

  private static void appendEncoding(SimpleJavaParameters javaParameters, GeneralCommandLine commandLine, ParametersList parametersList) {
    // Value of -Dfile.encoding and charset of GeneralCommandLine should be in sync in order process's input and output be correctly handled.
    String encoding = parametersList.getPropertyValue("file.encoding");
    if (encoding == null) {
      Charset charset = javaParameters.getCharset();
      if (charset == null) charset = EncodingManager.getInstance().getDefaultCharset();
      if (charset == null) charset = CharsetToolkit.getDefaultSystemCharset();
      commandLine.addParameter("-Dfile.encoding=" + charset.name());
      commandLine.setCharset(charset);
    }
    else {
      try {
        Charset charset = Charset.forName(encoding);
        commandLine.setCharset(charset);
      }
      catch (UnsupportedCharsetException ignore) {
      }
    }
  }
}