/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.execution.util;

import consulo.component.macro.PathMacroUtil;
import consulo.execution.CommonProgramRunConfigurationParameters;
import consulo.execution.RuntimeConfigurationWarning;
import consulo.execution.WorkingDirectoryProvider;
import consulo.execution.configuration.ModuleBasedConfiguration;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.module.macro.ModulePathMacroManager;
import consulo.process.cmd.SimpleProgramParameters;
import consulo.process.local.EnvironmentUtil;
import consulo.project.Project;
import consulo.project.macro.ProjectPathMacroManager;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFilePathUtil;
import org.jetbrains.annotations.SystemIndependent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ProgramParametersConfigurator {
  public void configureConfiguration(SimpleProgramParameters parameters, CommonProgramRunConfigurationParameters configuration) {
    Project project = configuration.getProject();
    Module module = getModule(configuration);

    parameters.getProgramParametersList().addParametersString(expandPath(configuration.getProgramParameters(), module, project));

    parameters.setWorkingDirectory(getWorkingDir(configuration, project, module));

    Map<String, String> envs = new HashMap<>(configuration.getEnvs());
    EnvironmentUtil.inlineParentOccurrences(envs);
    for (Map.Entry<String, String> each : envs.entrySet()) {
      each.setValue(expandPath(each.getValue(), module, project));
    }

    parameters.setEnv(envs);
    parameters.setPassParentEnvs(configuration.isPassParentEnvs());
  }

  @Nullable
  public String getWorkingDir(CommonProgramRunConfigurationParameters configuration, Project project, Module module) {
    String workingDirectory = configuration.getWorkingDirectory();
    String defaultWorkingDir = getDefaultWorkingDir(project);
    if (StringUtil.isEmptyOrSpaces(workingDirectory)) {
      workingDirectory = defaultWorkingDir;
      if (workingDirectory == null) {
        return null;
      }
    }
    workingDirectory = expandPath(workingDirectory, module, project);
    if (!FileUtil.isAbsolute(workingDirectory) && defaultWorkingDir != null) {
      if (PathMacroUtil.MODULE_DIR_MACRO.equals(workingDirectory)) {
        if(module != null) {
          String moduleDirPath = module.getModuleDirPath();
          if(moduleDirPath != null) {
            return moduleDirPath;
          }
        }
        return defaultWorkingDir;
      }

      if (PathMacroUtil.MODULE_WORKING_DIR.equals(workingDirectory)) {
        if(module != null) {
          String workingDir = getDefaultWorkingDir(module);
          if (workingDir != null) return workingDir;
        }
      }
      workingDirectory = defaultWorkingDir + "/" + workingDirectory;
    }
    return workingDirectory;
  }

  @Nullable
  protected String getDefaultWorkingDir(@Nonnull Project project) {
    return VirtualFilePathUtil.getLocalPath(project.getBaseDir());
  }

  @Nullable
  protected String getDefaultWorkingDir(@Nonnull Module module) {
    for (WorkingDirectoryProvider provider : module.getApplication().getExtensionList(WorkingDirectoryProvider.class)) {
      @SystemIndependent String path = provider.getWorkingDirectoryPath(module);
      if (path != null) return path;
    }
    VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
    if (roots.length > 0) {
      return VirtualFilePathUtil.getLocalPath(roots[0]);
    }
    return null;
  }

  public void checkWorkingDirectoryExist(CommonProgramRunConfigurationParameters configuration, Project project, Module module) throws RuntimeConfigurationWarning {
    final String workingDir = getWorkingDir(configuration, project, module);
    if (workingDir == null) {
      throw new RuntimeConfigurationWarning("Working directory is null for " +
                                            "project '" +
                                            project.getName() +
                                            "' (" +
                                            project.getBasePath() +
                                            ")" +
                                            ", module " +
                                            (module == null ? "null" : "'" + module.getName() + "' (" + module.getModuleDirPath() + ")"));
    }
    if (!new File(workingDir).exists()) {
      throw new RuntimeConfigurationWarning("Working directory '" + workingDir + "' doesn't exist");
    }
  }

  protected String expandPath(@Nullable String path, Module module, Project project) {
    path = ProjectPathMacroManager.getInstance(project).expandPath(path);
    if (module != null) {
      path = ModulePathMacroManager.getInstance(module).expandPath(path);
    }
    return path;
  }

  @Nullable
  protected Module getModule(CommonProgramRunConfigurationParameters configuration) {
    if (configuration instanceof ModuleBasedConfiguration) {
      return ((ModuleBasedConfiguration)configuration).getConfigurationModule().getModule();
    }
    return null;
  }
}
