/*
 * Copyright 2013 Consulo.org
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
package com.intellij.compiler;

import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author VISTALL
 * @since 13:05/10.06.13
 */
@State(
  name = "CompilerConfiguration",
  storages = {
    @Storage(file = StoragePathMacros.PROJECT_FILE),
    @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/compiler.xml", scheme = StorageScheme.DIRECTORY_BASED)
  }
)
public class CompilerConfigurationImpl extends CompilerConfiguration implements PersistentStateComponent<CompilerConfigurationImpl>{
  private CompilationType myCompilationType = CompilationType.IN_PROGRESS;

  @NotNull
  @Override
  public CompilationType getCompilationType() {
    return myCompilationType;
  }

  @Override
  public void setCompilationType(@NotNull CompilationType compilationType) {
    myCompilationType = compilationType;
  }

  @Nullable
  @Override
  public CompilerConfigurationImpl getState() {
    return this;
  }

  @Override
  public void loadState(CompilerConfigurationImpl state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}
