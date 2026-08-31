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

import consulo.util.lang.StringUtil;

import java.util.Comparator;

/**
 * The base contract for a named DVCS reference, like a branch, tag or bookmark.
 * <p>
 * Generic counterpart of {@code git4idea.GitReference}. Implementations must provide
 * value-based {@code equals}/{@code hashCode} over the reference name, since references are
 * used as keys in the branches tree model and looked up in favorite sets.
 *
 * @author VISTALL
 */
public interface DvcsRef {
    /**
     * Natural, numeric-aware comparator over reference names.
     */
    Comparator<String> REFS_NAMES_COMPARATOR = StringUtil::naturalCompare;

    /**
     * The name of the reference, e.g. "origin/master" or "feature".
     */
    String getName();

    /**
     * The full name of the reference, e.g. "refs/remotes/origin/master" or "refs/heads/master".
     */
    String getFullName();
}
