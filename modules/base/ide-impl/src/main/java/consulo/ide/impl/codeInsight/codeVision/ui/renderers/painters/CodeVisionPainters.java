// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.ide.impl.codeInsight.codeVision.ui.renderers.painters;

import consulo.language.editor.codeVision.CodeVisionEntry;
import consulo.language.editor.codeVision.TextCodeVisionEntry;

/**
 * Static dispatch utility replacing JetBrains' ClassExtension-based painter lookup.
 * Returns the appropriate {@link ICodeVisionEntryBasePainter} for a given {@link CodeVisionEntry}.
 * <p>
 * In JetBrains, this is implemented via {@code CodeVisionPainterProviders} using
 * {@code ClassExtension<ICodeVisionEntryBasePainter>}. Consulo uses a simpler
 * {@code instanceof} dispatch since only {@link TextCodeVisionEntry} exists.
 */
public final class CodeVisionPainters {
    private static final ICodeVisionEntryBasePainter<TextCodeVisionEntry> TEXT_ENTRY_PAINTER =
        new DefaultCodeVisionPainter<>(
            (project, entry, state) -> entry.icon,
            new CodeVisionVisionTextPainter<>(entry -> entry.text)
        );

    private static final ICodeVisionEntryBasePainter<CodeVisionEntry> FALLBACK_PAINTER =
        new CodeVisionVisionTextPainter<>(Object::toString);

    @SuppressWarnings("unchecked")
    public static ICodeVisionEntryBasePainter<CodeVisionEntry> getPainter(CodeVisionEntry entry) {
        if (entry instanceof TextCodeVisionEntry) {
            return (ICodeVisionEntryBasePainter<CodeVisionEntry>) (ICodeVisionEntryBasePainter<?>) TEXT_ENTRY_PAINTER;
        }
        return FALLBACK_PAINTER;
    }

    private CodeVisionPainters() {
    }
}
