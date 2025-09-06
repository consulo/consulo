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
package consulo.versionControlSystem.internal;

import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2025-09-03
 */
public interface LineStatusTrackerI {
    boolean isOperational();

    int transferLineToVcs(int line, boolean approximate);

    int transferLineToFromVcs(int line, boolean approximate);

    boolean isLineModified(int line);

    boolean isRangeModified(int line1, int line2);

    @Nonnull
    Document getVcsDocument();

    boolean isValid();

    List<VcsRange> getRanges();

    @Nullable
    VcsRange getNextRange(VcsRange range);

    @Nullable
    VcsRange getPrevRange(VcsRange range);

    @Nullable
    VcsRange getNextRange(int line);

    @Nullable
    VcsRange getPrevRange(int line);

    @Nonnull
    CharSequence getVcsContent(@Nonnull VcsRange range);

    @Nonnull
    Document getDocument();

    @Nonnull
    TextRange getVcsTextRange(@Nonnull VcsRange range);

    @Nonnull
    TextRange getCurrentTextRange(@Nonnull VcsRange range);

    @Nonnull
    VirtualFile getVirtualFile();

    @Nullable
    Project getProject();
}
