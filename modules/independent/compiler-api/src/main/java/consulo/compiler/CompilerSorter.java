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
package consulo.compiler;

import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.util.Chunk;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 08.05.14
 */
public interface CompilerSorter {
  public static final ExtensionPointName<CompilerSorter> EP_NAME = ExtensionPointName.create("com.intellij.compiler.sorter");

  void sort(Chunk<Module> moduleChunk, @Nonnull Compiler[] compilers, Class<? extends Compiler> clazz);
}
