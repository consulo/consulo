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
package com.intellij.compiler.impl.javaCompiler.javac;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

@State(
  name = "JavacCompilerConfiguration",
  storages = {
    @Storage(file = StoragePathMacros.PROJECT_FILE),
    @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/compiler.xml", scheme = StorageScheme.DIRECTORY_BASED)
  }
)
public class JavacCompilerConfiguration implements PersistentStateComponent<JpsJavaCompilerOptions> {
  @NotNull
  public static JpsJavaCompilerOptions getInstance(Project project) {
    final JavacCompilerConfiguration service = ServiceManager.getService(project, JavacCompilerConfiguration.class);
    return service.mySettings;
  }
  
  private final JpsJavaCompilerOptions mySettings = new JpsJavaCompilerOptions();
  private final Project myProject;

  public JavacCompilerConfiguration(Project project) {
    myProject = project;
  }

  @Override
  @NotNull
  public JpsJavaCompilerOptions getState() {
    JpsJavaCompilerOptions state = new JpsJavaCompilerOptions();
    XmlSerializerUtil.copyBean(mySettings, state);
    state.ADDITIONAL_OPTIONS_STRING = PathMacroManager.getInstance(myProject).collapsePathsRecursively(state.ADDITIONAL_OPTIONS_STRING);
    return state;
  }

  @Override
  public void loadState(JpsJavaCompilerOptions state) {
    XmlSerializerUtil.copyBean(state, mySettings);
  }
}