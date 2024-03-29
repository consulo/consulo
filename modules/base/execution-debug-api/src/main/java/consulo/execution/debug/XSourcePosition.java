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

package consulo.execution.debug;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.navigation.Navigatable;
import jakarta.annotation.Nonnull;

/**
 * Represents position in a source file. Use {@link XDebuggerUtil#createPosition} and {@link XDebuggerUtil#createPositionByOffset} to
 * create instances of this interface.
 * @author nik
 */
public interface XSourcePosition {
  /**
   * @return 0-based number of line
   */
  int getLine();

  /**
   * @return offset from the beginning of file
   */
  int getOffset();

  @Nonnull
  VirtualFile getFile();

  @Nonnull
  Navigatable createNavigatable(@Nonnull Project project);
}
