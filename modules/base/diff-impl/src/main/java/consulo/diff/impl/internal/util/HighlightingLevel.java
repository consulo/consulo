/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.diff.impl.internal.util;

import consulo.application.AllIcons;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.diff.localize.DiffLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Predicate;

public enum HighlightingLevel {
    INSPECTIONS(
        DiffLocalize.optionHighlightingLevelInspections(),
        AllIcons.Ide.HectorOn,
        rangeHighlighter -> true
    ),
    ADVANCED(
        DiffLocalize.optionHighlightingLevelSyntax(),
        AllIcons.Ide.HectorSyntax,
        rangeHighlighter -> rangeHighlighter.getLayer() <= HighlighterLayer.ADDITIONAL_SYNTAX
    ),
    SIMPLE(
        DiffLocalize.optionHighlightingLevelNone(),
        AllIcons.Ide.HectorOff,
        rangeHighlighter -> rangeHighlighter.getLayer() <= HighlighterLayer.SYNTAX
    );

    @Nonnull
    private final LocalizeValue myText;
    @Nullable
    private final Image myIcon;
    @Nonnull
    private final Predicate<RangeHighlighter> myCondition;

    HighlightingLevel(@Nonnull LocalizeValue text, @Nullable Image icon, @Nonnull Predicate<RangeHighlighter> condition) {
        myText = text;
        myIcon = icon;
        myCondition = condition;
    }

    @Nonnull
    public LocalizeValue getText() {
        return myText;
    }

    @Nullable
    public Image getIcon() {
        return myIcon;
    }

    @Nonnull
    public Predicate<RangeHighlighter> getCondition() {
        return myCondition;
    }
}