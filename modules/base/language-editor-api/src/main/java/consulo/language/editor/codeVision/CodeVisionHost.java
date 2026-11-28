// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.language.editor.codeVision;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * Entry point for invalidating code vision lenses.
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface CodeVisionHost {
    static CodeVisionHost getInstance(Project project) {
        return project.getInstance(CodeVisionHost.class);
    }

    /**
     * Pass empty list to update ALL providers in editor
     */
    record LensInvalidateSignal(@Nullable Editor editor, Collection<String> providerIds) {
        public LensInvalidateSignal(@Nullable Editor editor) {
            this(editor, List.of());
        }
    }

    /**
     * Recalculates the lenses of the given providers in the editor of the signal ({@code null} editor means all editors of the project).
     */
    void invalidateProvider(LensInvalidateSignal signal);
}
