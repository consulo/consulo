/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import consulo.compiler.scope.CompileScope;
import consulo.component.extension.ExtensionPointName;
import consulo.virtualFileSystem.fileType.FileType;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Base interface for a custom compiler which participates in the IDE build process.
 */
public interface Compiler {
  ExtensionPointName<Compiler> EP_NAME = ExtensionPointName.create("consulo.compiler");
  
  /**
   * Returns the description of the compiler. All registered compilers should have unique description.
   *
   * @return the description string.
   */
  @Nonnull
  String getDescription();

  /**
   * Called before compilation starts. If at least one of registered compilers returned false, compilation won't start.
   * 
   * @param scope the scope on which the compilation is started.
   * @return true if everything is ok, false otherwise
   */
  boolean validateConfiguration(CompileScope scope);

  default void registerCompilableFileTypes(@Nonnull Consumer<FileType> fileTypeConsumer) {
  }
}
