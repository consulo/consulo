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
package consulo.compiler.impl;

import com.intellij.compiler.impl.FileIndexCompileScope;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 12:55/20.12.13
 */
public interface CompileModuleScopeFactory {
  ExtensionPointName<CompileModuleScopeFactory> EP_NAME = ExtensionPointName.create("com.intellij.compiler.moduleScopeFactory");

  @Nullable
  FileIndexCompileScope createScope(@Nonnull final Module module, final boolean includeDependentModules);
}
