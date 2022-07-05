/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.local.ProcessHandlerFactory;
import consulo.util.io.CharsetToolkit;
import consulo.virtualFileSystem.util.PathsList;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.Charset;

/**
 * This parameter for java executable. This builder is raw version of what we have inside Java plugin.
 *
 * This implementation can be outdated to new java versions, since we do not need all functions what we have inside Java plugin.
 *
 * Useful for creating java processes in some plugins which create some java subprocesses with communicate with Consulo process
 *
 * @author Gregory.Shrago
 */
public final class SimpleJavaParameters extends SimpleProgramParameters {
  private String myJdkHome;
  private String myMainClass;
  private final PathsList myClassPath = new PathsList();
  private final ParametersList myVmParameters = new ParametersList();
  private Charset myCharset = CharsetToolkit.getDefaultSystemCharset();

  public String getMainClass() {
    return myMainClass;
  }

  /**
   * @return jdk used to launch the application.
   * If the instance of the JavaParameters is used to configure app server startup script,
   * then null is returned.
   */
  @Nullable
  public String getJdkHome() {
    return myJdkHome;
  }

  public void setJdkHome(final String jdkHome) {
    myJdkHome = jdkHome;
  }

  public void setMainClass(final String mainClass) {
    myMainClass = mainClass;
  }

  public PathsList getClassPath() {
    return myClassPath;
  }

  public ParametersList getVMParametersList() {
    return myVmParameters;
  }

  @Nullable
  public Charset getCharset() {
    return myCharset;
  }

  public void setCharset(@Nullable final Charset charset) {
    myCharset = charset;
  }

  @Nonnull
  public ProcessHandler createProcessHandler() throws ExecutionException {
    final String sdk = getJdkHome();
    assert sdk != null : "SDK should be defined";
    final GeneralCommandLine commandLine = JdkUtil.setupJVMCommandLine(sdk, this);
    return ProcessHandlerFactory.getInstance().createProcessHandler(commandLine);
  }
}
