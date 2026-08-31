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

import consulo.localize.LocalizeValue;

/**
 * A group type of DVCS references shown in the branches tree, e.g. local/remote branches, tags or bookmarks.
 * <p>
 * Generic counterpart of {@code git4idea.branch.GitRefType}. Each DVCS supplies its own set of types
 * (Git: local/remote/recent/tags; Mercurial: branch/bookmark).
 * <p>
 *
 * @author VISTALL
 */
public interface DvcsRefType {
    /**
     * Stable identifier of the type, used as a tree path element id (e.g. "LOCAL", "REMOTE", "TAG").
     */
    String getName();

    /**
     * Section title when a single repository is displayed.
     */
    LocalizeValue getText();

    /**
     * Section title when references of a specific repository are displayed inside a multi-repo view.
     */
    LocalizeValue getInRepoText(String repositoryName);

    /**
     * Section title when references common to several repositories are displayed.
     */
    LocalizeValue getCommonText();
}
