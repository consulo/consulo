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

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 12:57/10.06.13
 */
public abstract class CompilerConfiguration {
  @Nonnull
  public static CompilerConfiguration getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, CompilerConfiguration.class);
  }

  @Nullable
  public abstract VirtualFile getCompilerOutput();

  @Nonnull
  public abstract String getCompilerOutputUrl();

  public abstract VirtualFilePointer getCompilerOutputPointer();

  public abstract void setCompilerOutputUrl(@Nullable String compilerOutputUrl);
}
