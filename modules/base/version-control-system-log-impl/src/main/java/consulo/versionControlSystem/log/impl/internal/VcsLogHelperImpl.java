/*
 * Copyright 2013-2025 consulo.io
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
package consulo.versionControlSystem.log.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.versionControlSystem.log.TimedVcsCommit;
import consulo.versionControlSystem.log.VcsLogHelper;
import consulo.versionControlSystem.log.impl.internal.data.VcsLogSorter;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 2025-08-30
 */
@ServiceImpl
@Singleton
public class VcsLogHelperImpl implements VcsLogHelper {
    @Override
    public <Commit extends TimedVcsCommit> List<Commit> sortByDateTopoOrder(@Nonnull Collection<Commit> commits) {
        return VcsLogSorter.sortByDateTopoOrder(commits);
    }
}
