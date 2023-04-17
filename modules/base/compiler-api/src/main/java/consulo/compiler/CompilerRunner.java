/*
 * Copyright 2013-2023 consulo.io
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 17/04/2023
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface CompilerRunner {
  @Nonnull
  LocalizeValue getName();

  boolean isAvailable(CompileContextEx context);

  default void cleanUp(CompileDriver compileDriver, CompileContextEx context) {
  }

  boolean build(CompileDriver compileDriver,
                CompileContextEx context,
                boolean isRebuild,
                boolean forceCompile,
                boolean onlyCheckStatus) throws ExitException;
}
