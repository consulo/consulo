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
package consulo.project;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.application.util.function.ThrowableComputable;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author max
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class ProjectLocator {
  public static ProjectLocator getInstance() {
    return Application.get().getInstance(ProjectLocator.class);
  }

  /**
   * Returns an open project which contains the given file.
   * This is a guess-method, so if several projects contain the file, only one will be returned.
   * Also a project may be returned though it doesn't contain the file for sure (see implementations).
   *
   * @param file file to be located in projects.
   * @return project which probably contains the file, or null if couldn't guess (for example, there are no open projects).
   */
  @Nullable
  public abstract Project guessProjectForFile(@Nullable VirtualFile file);

  /**
   * Gets all open projects containing the given file.
   * If none does, an empty list is returned.
   *
   * @param file file to be located in projects.
   * @return list of open projects containing this file.
   */
  @Nonnull
  public abstract Collection<Project> getProjectsForFile(VirtualFile file);

  public static <T, E extends Throwable> T computeWithPreferredProject(@Nonnull VirtualFile file, @Nonnull Project preferredProject, @Nonnull ThrowableComputable<T, E> action) throws E {
    Map<VirtualFile, Project> local = ourPreferredProjects.get();
    local.put(file, preferredProject);
    try {
      return action.compute();
    }
    finally {
      local.remove(file);
    }
  }

  @Nullable
  public static Project getPreferredProject(@Nonnull VirtualFile file) {
    return ourPreferredProjects.get().get(file);
  }

  private static final ThreadLocal<Map<VirtualFile, Project>> ourPreferredProjects = ThreadLocal.withInitial(HashMap::new);

}