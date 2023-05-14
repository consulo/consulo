/*
 * Copyright 2013-2022 consulo.io
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
package consulo.compiler.artifact.internal;

import consulo.compiler.artifact.ArtifactManager;
import consulo.compiler.artifact.ArtifactPointer;
import consulo.compiler.artifact.ArtifactPointerManager;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 03-Sep-22
 */
public interface ArtifactPointerManagerEx extends ArtifactPointerManager {
  /**
   * Special method for serialization, while target artifact manager did not provide recursion.
   * <p>
   * It will be called from consulo.compiler.artifact.element.ArtifactPackagingElement, while ArtifactManager#loadState()
   */
  @Nonnull
  public ArtifactPointer create(@Nonnull ArtifactManager artifactManager, @Nonnull String name);
}
