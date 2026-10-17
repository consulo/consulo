/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.impl.internal.push;

import consulo.versionControlSystem.distributed.push.PushInfo;
import consulo.versionControlSystem.distributed.push.PushSource;
import consulo.versionControlSystem.distributed.push.PushSpec;
import consulo.versionControlSystem.distributed.push.PushTarget;
import consulo.versionControlSystem.distributed.repository.Repository;
import consulo.versionControlSystem.log.VcsFullCommitDetails;

import java.util.List;

final class PushInfoImpl implements PushInfo {
    private final Repository myRepository;
    private final PushSpec<PushSource, PushTarget> myPushSpec;
    private final List<VcsFullCommitDetails> myCommits;

    PushInfoImpl(Repository repository, PushSpec<PushSource, PushTarget> spec, List<VcsFullCommitDetails> commits) {
        myRepository = repository;
        myPushSpec = spec;
        myCommits = commits;
    }

    @Override
    public Repository getRepository() {
        return myRepository;
    }

    @Override
    public PushSpec<PushSource, PushTarget> getPushSpec() {
        return myPushSpec;
    }

    @Override
    public List<VcsFullCommitDetails> getCommits() {
        return myCommits;
    }
}
