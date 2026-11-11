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
package consulo.module.content.impl.internal.root;

import consulo.module.content.internal.WorkspaceExcludeFileSet;

/**
 * Excludes the root and its descendants from the kinds selected by {@link #getMask()}.
 * A nested inclusion rule can include files again.
 */
final class ExcludedFileSetByFileKind implements WorkspaceExcludeFileSet.ByFileKind {
    private final int myMask;

    ExcludedFileSetByFileKind(int mask) {
        myMask = mask;
    }

    @Override
    public int getMask() {
        return myMask;
    }

    @Override
    public String toString() {
        return "ExcludedFileSet.ByFileKind{mask=" + myMask + "}";
    }
}
