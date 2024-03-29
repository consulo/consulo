/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import jakarta.inject.Inject;

import jakarta.inject.Singleton;

@Singleton
@ServiceImpl
@State(name = "RecentProjectsManager", storages = {@Storage(value = "recentProjects.xml", roamingType = RoamingType.DISABLED)})
public class RecentDirectoryProjectsManager extends RecentProjectsManagerBase {
  @Inject
  public RecentDirectoryProjectsManager(Application application) {
    super(application);
  }
}
