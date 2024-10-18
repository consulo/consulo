/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.diff.fragment;

import consulo.desktop.awt.internal.diff.simple.SimpleOnesideDiffViewer;
import consulo.diff.DiffContext;
import consulo.diff.FrameDiffTool;
import consulo.diff.request.DiffRequest;
import jakarta.annotation.Nonnull;

public class UnifiedDiffTool implements FrameDiffTool {
    public static final UnifiedDiffTool INSTANCE = new UnifiedDiffTool();

    @Nonnull
    @Override
    public DiffViewer createComponent(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
        if (SimpleOnesideDiffViewer.canShowRequest(context, request)) {
            return new SimpleOnesideDiffViewer(context, request);
        }
        if (UnifiedDiffViewer.canShowRequest(context, request)) {
            return new UnifiedDiffViewer(context, request);
        }
        throw new IllegalArgumentException(request.toString());
    }

    @Override
    public boolean canShow(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
        return SimpleOnesideDiffViewer.canShowRequest(context, request) || UnifiedDiffViewer.canShowRequest(context, request);
    }

    @Nonnull
    @Override
    public String getName() {
        return "Oneside viewer";
    }
}
