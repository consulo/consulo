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
package consulo.versionControlSystem.distributed.ui.branch.popup;

import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.distributed.ui.branch.popup.DvcsRef;
import org.jspecify.annotations.Nullable;

/**
 * Lightweight view of a repository used by the branches tree popup.
 * <p>
 *
 * @author VISTALL
 */
public interface DvcsRepositoryModel extends Comparable<DvcsRepositoryModel> {
    /**
     * Short, human-readable repository name shown in the tree.
     */
    String getShortName();

    /**
     * @return true if the given reference is the current one in this repository.
     */
    boolean isCurrentRef(DvcsRef ref);

    /**
     * @return true if the given reference is marked as favorite in this repository.
     */
    boolean isFavorite(DvcsRef ref);

    /**
     * @return the name of the remote branch tracked by the given local branch reference, or
     * {@code null} if the reference is not a local branch or has no tracking information.
     */
    @Nullable
    String getTrackedBranchName(DvcsRef ref);

    /**
     * Working-tree root of the repository.
     */
    FilePath getRoot();

    /**
     * Icon shown next to the repository node in a multi-repo tree, or {@code null} for the default icon.
     */
    @Nullable
    Image getIcon();

    @Override
    default int compareTo(DvcsRepositoryModel other) {
        return StringUtil.naturalCompare(getShortName(), other.getShortName());
    }
}
