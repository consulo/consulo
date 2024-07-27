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
package consulo.versionControlSystem.impl.internal.change;

import consulo.project.Project;
import consulo.util.collection.MultiMap;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.root.VcsRoot;

import java.util.Collection;
import java.util.function.Function;

public class SortByVcsRoots<T> {
  private final Project myProject;
  private final Function<T, FilePath> myConvertor;
  private ProjectLevelVcsManager myVcsManager;
  public static final VcsRoot ourFictiveValue = new VcsRoot(null, null);

  public SortByVcsRoots(Project project, final Function<T, FilePath> convertor) {
    myProject = project;
    myVcsManager = ProjectLevelVcsManager.getInstance(project);
    myConvertor = convertor;
  }

  public MultiMap<VcsRoot, T> sort(final Collection<T> in) {
    final MultiMap<VcsRoot, T> result = new MultiMap<>();
    for (T t : in) {
      final VcsRoot root = myVcsManager.getVcsRootObjectFor(myConvertor.apply(t));
      if (root != null) {
        result.putValue(root, t);
      }
      else {
        result.putValue(ourFictiveValue, t);
      }
    }
    return result;
  }
}
