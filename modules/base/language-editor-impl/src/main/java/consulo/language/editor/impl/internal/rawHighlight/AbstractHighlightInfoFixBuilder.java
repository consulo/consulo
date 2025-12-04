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
package consulo.language.editor.impl.internal.rawHighlight;

import consulo.document.util.TextRange;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

import static consulo.language.editor.rawHighlight.HighlightInfo.FixBuilderBase;

/**
 * @author UNV
 * @since 2025-12-04
 */
public abstract class AbstractHighlightInfoFixBuilder<THIS extends FixBuilderBase<THIS>> implements FixBuilderBase<THIS> {
    @Nonnull
    protected final IntentionAction myAction;
    @Nullable
    protected List<IntentionAction> myOptions = null;
    @Nonnull
    protected LocalizeValue myDisplayName = LocalizeValue.empty();
    @Nullable
    protected TextRange myFixRange;
    @Nullable
    protected HighlightDisplayKey myKey;

    public AbstractHighlightInfoFixBuilder(@Nonnull IntentionAction action) {
        myAction = action;
    }

    @Nonnull
    @Override
    public THIS options(@Nonnull List<IntentionAction> options) {
        myOptions = options;
        return self();
    }

    @Nonnull
    @Override
    public THIS displayName(@Nonnull LocalizeValue displayName) {
        myDisplayName = displayName;
        return self();
    }

    @Nonnull
    @Override
    public THIS fixRange(@Nullable TextRange fixRange) {
        myFixRange = fixRange;
        return self();
    }

    @Nonnull
    @Override
    public THIS key(@Nullable HighlightDisplayKey key) {
        myKey = key;
        return self();
    }

    @SuppressWarnings("unchecked")
    private THIS self() {
        return (THIS) this;
    }
}
