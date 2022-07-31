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
package consulo.ide.impl.idea.openapi.vcs.changes.committed;

import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.CachingCommittedChangesProvider;
import consulo.versionControlSystem.CommittedChangesProvider;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.ide.impl.idea.util.NotNullFunction;
import javax.annotation.Nonnull;

/**
 * @author yole
 */
public class IncomingChangesVisibilityPredicate implements NotNullFunction<Project, Boolean> {
  @Nonnull
  public Boolean fun(final Project project) {
    final AbstractVcs[] abstractVcses = ProjectLevelVcsManager.getInstance(project).getAllActiveVcss();
    for(AbstractVcs vcs: abstractVcses) {
      CommittedChangesProvider provider = vcs.getCommittedChangesProvider();
      if (provider instanceof CachingCommittedChangesProvider && provider.supportsIncomingChanges()) {
        return Boolean.TRUE;
      }
    }
    return Boolean.FALSE;
  }
}
