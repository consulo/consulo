/*
 * Copyright 2013-2021 consulo.io
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
package consulo.compiler.impl.build;

import com.intellij.build.BuildWorkspaceConfiguration;
import com.intellij.compiler.CompilerWorkspaceConfiguration;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 28/11/2021
 */
@Singleton
public class BuildWorkspaceConfigurationImpl implements BuildWorkspaceConfiguration {
  private final CompilerWorkspaceConfiguration myCompilerConfiguration;

  @Inject
  public BuildWorkspaceConfigurationImpl(CompilerWorkspaceConfiguration compilerConfiguration) {
    myCompilerConfiguration = compilerConfiguration;
  }

  @Override
  public boolean isShowFirstErrorInEditor() {
    return myCompilerConfiguration.AUTO_SHOW_ERRORS_IN_EDITOR;
  }
}
