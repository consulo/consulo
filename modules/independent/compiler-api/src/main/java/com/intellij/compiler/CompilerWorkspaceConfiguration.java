/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

/*
 * @author Eugene Zhuravlev
 */
package com.intellij.compiler;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import consulo.compiler.CompilationType;

import javax.inject.Singleton;

@State(name = "CompilerWorkspaceConfiguration", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@Singleton
public class CompilerWorkspaceConfiguration implements PersistentStateComponent<CompilerWorkspaceConfiguration> {
  public CompilationType COMPILATION_TYPE = CompilationType.IN_PROGRESS;
  public boolean AUTO_SHOW_ERRORS_IN_EDITOR = true;
  public boolean CLEAR_OUTPUT_DIRECTORY = true;

  @Deprecated
  public static CompilerWorkspaceConfiguration getInstance(Project project) {
    return ServiceManager.getService(project, CompilerWorkspaceConfiguration.class);
  }

  @Override
  public CompilerWorkspaceConfiguration getState() {
    return this;
  }

  @Override
  public void loadState(CompilerWorkspaceConfiguration state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
