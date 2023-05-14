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
package consulo.ide.impl.idea.openapi.deployment;

import consulo.compiler.artifact.ArtifactUtil;

import jakarta.annotation.Nonnull;

@Deprecated
public abstract class DeploymentUtil {
  public static String trimForwardSlashes(@Nonnull String path) {
    return ArtifactUtil.trimForwardSlashes(path);
  }

  public static String concatPaths(String... paths) {
    return ArtifactUtil.concatPaths(paths);
  }

  public static String appendToPath(@Nonnull String basePath, @Nonnull String relativePath) {
    return ArtifactUtil.appendToPath(basePath, relativePath);
  }
}
