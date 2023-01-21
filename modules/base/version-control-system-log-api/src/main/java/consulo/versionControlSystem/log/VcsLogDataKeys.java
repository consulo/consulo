/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.versionControlSystem.log;

import consulo.util.dataholder.Key;

import java.util.List;

/**
 * Provides {@link Key Key} which can be used by actions to access data available in the VCS log.
 */
public interface VcsLogDataKeys {
  @Deprecated
  Key<VcsLog> VCS_LOG = VcsLog.KEY;
  @Deprecated
  Key<VcsLogUi> VCS_LOG_UI = VcsLogUi.KEY;
  @Deprecated
  Key<VcsLogDataProvider> VCS_LOG_DATA_PROVIDER = VcsLogDataProvider.KEY;

  Key<List<VcsRef>> VCS_LOG_BRANCHES = Key.create("Vcs.Log.Branches");
}
