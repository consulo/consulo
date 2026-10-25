// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.log.base;

import consulo.versionControlSystem.change.Change;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class VcsStatusMerger<S, P> {
    public List<MergedStatusInfo<S>> merge(List<List<S>> statuses) {
        if (statuses.size() == 1) {
            List<MergedStatusInfo<S>> result = new ArrayList<>();
            for (S status : statuses.getFirst()) {
                result.add(new MergedStatusInfo<>(status));
            }
            return result;
        }

        List<Map<P, S>> pathsToStatusesMap = new ArrayList<>();
        for (List<S> infos : statuses) {
            Map<P, S> map = new LinkedHashMap<>();
            for (S info : infos) {
                P path = getPath(info);
                if (path != null) {
                    map.put(path, info);
                }
            }
            pathsToStatusesMap.add(map);
        }

        List<MergedStatusInfo<S>> result = new ArrayList<>();

        outer:
        for (P path : pathsToStatusesMap.getFirst().keySet()) {
            List<S> statusesList = new ArrayList<>();
            for (Map<P, S> pathsToStatusesForParent : pathsToStatusesMap) {
                S status = pathsToStatusesForParent.get(path);
                if (status == null) {
                    continue outer;
                }
                statusesList.add(status);
            }

            result.add(new MergedStatusInfo<>(merge(path, statusesList), statusesList));
        }

        return result;
    }

    public S merge(P path, List<S> statuses) {
        List<Change.Type> types = statuses.stream().map(this::getType).distinct().toList();

        if (types.size() == 1) {
            if (types.getFirst() == Change.Type.MOVED) {
                P renamedFrom = null;
                for (S status : statuses) {
                    if (renamedFrom == null) {
                        renamedFrom = getFirstPath(status);
                    }
                    else if (!Objects.equals(renamedFrom, getFirstPath(status))) {
                        return createStatus(Change.Type.MODIFICATION, path, null);
                    }
                }
            }
            return statuses.getFirst();
        }

        return types.contains(Change.Type.DELETED)
            ? createStatus(Change.Type.DELETED, path, null)
            : createStatus(Change.Type.MODIFICATION, path, null);
    }

    private @Nullable P getPath(S info) {
        return switch (getType(info)) {
            case MODIFICATION, NEW, DELETED -> getFirstPath(info);
            case MOVED -> getSecondPath(info);
        };
    }

    protected abstract S createStatus(Change.Type type, P path, @Nullable P secondPath);

    protected abstract P getFirstPath(S info);

    protected abstract @Nullable P getSecondPath(S info);

    protected abstract Change.Type getType(S info);
}
