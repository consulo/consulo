/*
 * Copyright 2013-2016 consulo.io
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
package consulo.backgroundTaskByVfsChange;

import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * @author VISTALL
 * @since 23:20/06.10.13
 */
@Tag("parameters")
public class BackgroundTaskByVfsParametersImpl implements BackgroundTaskByVfsParameters {
  public static BackgroundTaskByVfsParametersImpl EMPTY = new BackgroundTaskByVfsParametersImpl(null);

  @Transient
  private final Project myProject;

  private String myProgramParameters;
  private String myWorkDirectory;
  private String myExePath = "";
  private String myOutPath = "";
  private boolean myShowConsole = true;
  private Map<String, String> myEnvs = Collections.emptyMap();
  private boolean myPassParentEnvs;

  public BackgroundTaskByVfsParametersImpl(Project project) {
    myProject = project;
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public void setProgramParameters(@Nullable String value) {
    myProgramParameters = value;
  }

  @Nullable
  @Override
  public String getProgramParameters() {
    return myProgramParameters;
  }

  @Override
  public void setWorkingDirectory(@Nullable String value) {
    myWorkDirectory = value;
  }

  @Nullable
  @Override
  public String getWorkingDirectory() {
    return myWorkDirectory;
  }

  @Override
  public void setEnvs(@NotNull Map<String, String> envs) {
    myEnvs = envs;
  }

  @NotNull
  @Override
  public Map<String, String> getEnvs() {
    return myEnvs;
  }

  @Override
  public void setPassParentEnvs(boolean passParentEnvs) {
    myPassParentEnvs = passParentEnvs;
  }

  @Override
  public boolean isPassParentEnvs() {
    return myPassParentEnvs;
  }

  @Override
  public void setExePath(@NotNull String path) {
    myExePath = path;
  }

  @NotNull
  @Override
  public String getExePath() {
    return myExePath;
  }

  @Override
  public void setOutPath(@Nullable String path) {
    myOutPath = path;
  }

  @Nullable
  @Override
  public String getOutPath() {
    return myOutPath;
  }

  @Override
  public void set(@NotNull BackgroundTaskByVfsParameters parameters) {
    setEnvs(parameters.getEnvs());
    setExePath(parameters.getExePath());
    setOutPath(parameters.getOutPath());
    setPassParentEnvs(parameters.isPassParentEnvs());
    setProgramParameters(parameters.getProgramParameters());
    setWorkingDirectory(parameters.getWorkingDirectory());
    setShowConsole(parameters.isShowConsole());
  }

  @Override
  public boolean isShowConsole() {
    return myShowConsole;
  }

  @Override
  public void setShowConsole(boolean console) {
    myShowConsole = console;
  }
}
