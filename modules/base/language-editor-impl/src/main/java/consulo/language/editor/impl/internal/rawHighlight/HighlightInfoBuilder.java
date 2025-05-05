/*
 * Copyright 2013-2024 consulo.io
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

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.document.util.TextRange;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.annotation.ProblemGroup;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-03-09
 */
class HighlightInfoBuilder implements HighlightInfo.Builder {
    private static final LocalizeValue UNSET = LocalizeValue.of("__UNSET__");

    private static final Logger LOG = Logger.getInstance(HighlightInfoBuilder.class);

    private record FixInfo(
        @Nonnull IntentionAction action,
        @Nullable List<IntentionAction> options,
        @Nullable String displayName,
        @Nullable TextRange fixRange,
        @Nullable HighlightDisplayKey key
    ) {
    }

    private Boolean myNeedsUpdateOnTyping;
    private TextAttributes forcedTextAttributes;
    private TextAttributesKey forcedTextAttributesKey;

    private final HighlightInfoType type;
    private int startOffset = -1;
    private int endOffset = -1;

    private LocalizeValue escapedDescription = UNSET;
    private LocalizeValue escapedToolTip = UNSET;
    private HighlightSeverity severity;

    private boolean isAfterEndOfLine;
    private boolean isFileLevelAnnotation;
    private int navigationShift;

    private GutterIconRenderer gutterIconRenderer;
    private ProblemGroup problemGroup;
    private String inspectionToolId;
    private PsiElement psiElement;
    private int group;
    private final List<FixInfo> fixes = new ArrayList<>();
    private boolean created;

    HighlightInfoBuilder(@Nonnull HighlightInfoType type) {
        this.type = type;
    }

    private void assertNotCreated() {
        assert !created : "Must not call this method after Builder.create() was called";
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder gutterIconRenderer(@Nonnull GutterIconRenderer gutterIconRenderer) {
        assertNotCreated();
        assert this.gutterIconRenderer == null : "gutterIconRenderer already set";
        this.gutterIconRenderer = gutterIconRenderer;
        return this;
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder problemGroup(@Nonnull ProblemGroup problemGroup) {
        assertNotCreated();
        assert this.problemGroup == null : "problemGroup already set";
        this.problemGroup = problemGroup;
        return this;
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder inspectionToolId(@Nonnull String inspectionToolId) {
        assertNotCreated();
        assert this.inspectionToolId == null : "inspectionToolId already set";
        this.inspectionToolId = inspectionToolId;
        return this;
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder description(@Nonnull LocalizeValue description) {
        assertNotCreated();
        assert escapedDescription == UNSET : "description already set";
        escapedDescription = description;
        return this;
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder textAttributes(@Nonnull TextAttributes attributes) {
        assertNotCreated();
        assert forcedTextAttributes == null : "textAttributes already set";
        forcedTextAttributes = attributes;
        return this;
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder textAttributes(@Nonnull TextAttributesKey attributesKey) {
        assertNotCreated();
        assert forcedTextAttributesKey == null : "textAttributesKey already set";
        forcedTextAttributesKey = attributesKey;
        return this;
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder unescapedToolTip(@Nonnull LocalizeValue unescapedToolTip) {
        assertNotCreated();
        assert escapedToolTip == UNSET : "Tooltip was already set";
        escapedToolTip = unescapedToolTip.map((m, text) -> HighlightInfoImpl.htmlEscapeToolTip(text));
        return this;
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder escapedToolTip(@Nonnull LocalizeValue escapedToolTip) {
        assertNotCreated();
        assert this.escapedToolTip == UNSET : "Tooltip was already set";
        this.escapedToolTip = escapedToolTip;
        return this;
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder range(int start, int end) {
        assertNotCreated();

        assert startOffset == -1 && endOffset == -1 : "Offsets already set";

        startOffset = start;
        endOffset = end;
        return this;
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public HighlightInfo.Builder range(@Nonnull PsiElement element) {
        assertNotCreated();
        assert psiElement == null : " psiElement already set";
        psiElement = element;
        return range(element.getTextRange());
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder range(@Nonnull PsiElement element, int start, int end) {
        assertNotCreated();
        assert psiElement == null : " psiElement already set";
        psiElement = element;
        return range(start, end);
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder endOfLine() {
        assertNotCreated();
        isAfterEndOfLine = true;
        return this;
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder needsUpdateOnTyping(boolean update) {
        assertNotCreated();
        assert myNeedsUpdateOnTyping == null : " needsUpdateOnTyping already set";
        myNeedsUpdateOnTyping = update;
        return this;
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder severity(@Nonnull HighlightSeverity severity) {
        assertNotCreated();
        assert this.severity == null : " severity already set";
        this.severity = severity;
        return this;
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder fileLevelAnnotation() {
        assertNotCreated();
        isFileLevelAnnotation = true;
        return this;
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder navigationShift(int navigationShift) {
        assertNotCreated();
        this.navigationShift = navigationShift;
        return this;
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder group(int group) {
        assertNotCreated();
        this.group = group;
        return this;
    }

    @Nonnull
    @Override
    public HighlightInfo.Builder registerFix(
        @Nonnull IntentionAction action,
        @Nullable List<IntentionAction> options,
        @Nls @Nullable String displayName,
        @Nullable TextRange fixRange,
        @Nullable HighlightDisplayKey key
    ) {
        assertNotCreated();
        fixes.add(new FixInfo(action, options, displayName, fixRange, key));
        return this;
    }

    @Nullable
    @Override
    public HighlightInfoImpl create() {
        HighlightInfoImpl info = createUnconditionally();
        LOG.assertTrue(
            psiElement != null
                || severity == HighlightInfoType.SYMBOL_TYPE_SEVERITY
                || severity == HighlightInfoType.INJECTED_FRAGMENT_SEVERITY
                || ArrayUtil.find(HighlightSeverity.DEFAULT_SEVERITIES, severity) != -1,
            "Custom type requires not-null element to detect its text attributes"
        );

        if (!HighlightInfoImpl.isAcceptedByFilters(info, psiElement)) {
            return null;
        }

        return info;
    }

    @Nullable
    private static String nullizeValue(@Nonnull LocalizeValue localizeValue) {
        return localizeValue == UNSET ? null : localizeValue.get();
    }

    @Nonnull
    @Override
    public HighlightInfoImpl createUnconditionally() {
        assertNotCreated();
        created = true;

        if (severity == null) {
            severity = type.getSeverity(psiElement);
        }

        HighlightInfoImpl info = new HighlightInfoImpl(
            forcedTextAttributes,
            forcedTextAttributesKey,
            type,
            startOffset,
            endOffset,
            nullizeValue(escapedDescription),
            nullizeValue(escapedToolTip),
            severity,
            isAfterEndOfLine,
            myNeedsUpdateOnTyping,
            isFileLevelAnnotation,
            navigationShift,
            problemGroup,
            inspectionToolId,
            gutterIconRenderer,
            group
        );
        for (FixInfo fix : fixes) {
            info.registerFix(fix.action(), fix.options(), fix.displayName(), fix.fixRange(), fix.key());
        }
        return info;
    }
}
