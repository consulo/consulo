/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * @author nik
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class ArtifactSortingUtil {
  public static ArtifactSortingUtil getInstance(@Nonnull Project project) {
    return project.getInstance(ArtifactSortingUtil.class);
  }

  public abstract Map<String, String> getArtifactToSelfIncludingNameMap();

  public abstract List<String> getArtifactsSortedByInclusion();
}
