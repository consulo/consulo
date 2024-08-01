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
package consulo.versionControlSystem.change;

import consulo.util.collection.ContainerUtil;
import consulo.util.concurrent.ActionCallback;
import consulo.versionControlSystem.FilePath;

import java.util.List;

public record VcsInvalidated(List<VcsModifiableDirtyScope> scopes, boolean isEverythingDirty, ActionCallback callback) {
    public boolean isFileDirty(final FilePath fp) {
        return isEverythingDirty() || ContainerUtil.any(scopes(), dirtyScope -> dirtyScope.belongsTo(fp));
    }

    public boolean isEmpty() {
        return scopes().isEmpty();
    }

    public void doWhenCanceled(Runnable task) {
        callback().doWhenRejected(task);
    }

    public List<VcsModifiableDirtyScope> getScopes() {
        return scopes();
    }
}