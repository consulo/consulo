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

package com.intellij.execution.configurations;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.openapi.projectRoots.JdkUtil;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.util.PathsList;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;

/**
 * @author Gregory.Shrago
 */
//TODO [VISTALL] move to java-platform
public class SimpleJavaParameters extends SimpleProgramParameters {
  private Sdk myJdk;
  private String myMainClass;
  private final PathsList myClassPath = new PathsList();
  private final ParametersList myVmParameters = new ParametersList();
  private Charset myCharset = CharsetToolkit.getDefaultSystemCharset();
  private boolean myUseDynamicClasspath;
  private boolean myUseDynamicVMOptions;
  
  public String getMainClass() {
    return myMainClass;
  }

  /**
   * @return jdk used to launch the application.
   * If the instance of the JavaParameters is used to configure app server startup script,
   * then null is returned.
   */
  @Nullable
  public Sdk getJdk() {
    return myJdk;
  }

  public void setJdk(final Sdk jdk) {
    myJdk = jdk;
  }

  public void setMainClass(@NonNls final String mainClass) {
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

  public void setUseDynamicClasspath(final boolean useDynamicClasspath) {
    myUseDynamicClasspath = useDynamicClasspath;
  }

  public void setUseDynamicVMOptions(boolean useDynamicVMOptions) {
    myUseDynamicVMOptions = useDynamicVMOptions;
  }

  public boolean isDynamicVMOptions() {
    return myUseDynamicVMOptions;
  }

  public OSProcessHandler createOSProcessHandler() throws ExecutionException {
    final Sdk sdk = getJdk();
    assert sdk != null : "SDK should be defined";
    final GeneralCommandLine commandLine = JdkUtil.setupJVMCommandLine(sdk, this, myUseDynamicClasspath);
    final OSProcessHandler processHandler = new OSProcessHandler(commandLine.createProcess(), commandLine.getCommandLineString()) {
      @Override
      public Charset getCharset() {
        return commandLine.getCharset();
      }
    };
    ProcessTerminatedListener.attach(processHandler);
    return processHandler;
  }
}
