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
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2024-03-09
 */
class HighlightInfoBuilder implements HighlightInfo.Builder {
    class MyFixBuilder extends AbstractHighlightInfoFixBuilder<FixBuilder> implements FixBuilder {
        public MyFixBuilder(IntentionAction action) {
            super(action);
        }

        
        @Override
        public HighlightInfo.Builder register() {
            return registerFix(myAction, myOptions, myDisplayName, myFixRange, myKey);
        }
    }

    private static final Logger LOG = Logger.getInstance(HighlightInfoBuilder.class);

    private record FixInfo(
        IntentionAction action,
        @Nullable List<IntentionAction> options,
        LocalizeValue displayName,
        @Nullable TextRange fixRange,
        @Nullable HighlightDisplayKey key
    ) {
    }

    private Boolean myNeedsUpdateOnTyping;
    private TextAttributes myForcedTextAttributes;
    private TextAttributesKey myForcedTextAttributesKey;

    private final HighlightInfoType myType;
    private int myStartOffset = -1;
    private int myEndOffset = -1;

    private LocalizeValue myEscapedDescription = LocalizeValue.empty();
    private LocalizeValue myEscapedToolTip = LocalizeValue.empty();
    private HighlightSeverity mySeverity;

    private boolean myIsAfterEndOfLine;
    private boolean myIsFileLevelAnnotation;
    private int myNavigationShift;

    private GutterIconRenderer myGutterIconRenderer;
    private ProblemGroup myProblemGroup;
    private String myInspectionToolId;
    private PsiElement myPsiElement;
    private int myGroup;
    private final List<FixInfo> myFixes = new ArrayList<>();
    private boolean myCreated;

    HighlightInfoBuilder(HighlightInfoType type) {
        this.myType = type;
    }

    private void assertNotCreated() {
        assert !myCreated : "Must not call this method after Builder.create() was called";
    }

    
    @Override
    public HighlightInfo.Builder gutterIconRenderer(GutterIconRenderer gutterIconRenderer) {
        assertNotCreated();
        assert this.myGutterIconRenderer == null : "gutterIconRenderer already set";
        this.myGutterIconRenderer = gutterIconRenderer;
        return this;
    }

    
    @Override
    public HighlightInfo.Builder problemGroup(ProblemGroup problemGroup) {
        assertNotCreated();
        assert this.myProblemGroup == null : "problemGroup already set";
        this.myProblemGroup = problemGroup;
        return this;
    }

    
    @Override
    public HighlightInfo.Builder inspectionToolId(String inspectionToolId) {
        assertNotCreated();
        assert this.myInspectionToolId == null : "inspectionToolId already set";
        this.myInspectionToolId = inspectionToolId;
        return this;
    }

    
    @Override
    public HighlightInfo.Builder description(LocalizeValue description) {
        assertNotCreated();
        assert myEscapedDescription.isEmpty() : "description already set";
        myEscapedDescription = description;
        return this;
    }

    
    @Override
    public HighlightInfo.Builder textAttributes(TextAttributes attributes) {
        assertNotCreated();
        assert myForcedTextAttributes == null : "textAttributes already set";
        myForcedTextAttributes = attributes;
        return this;
    }

    
    @Override
    public HighlightInfo.Builder textAttributes(TextAttributesKey attributesKey) {
        assertNotCreated();
        assert myForcedTextAttributesKey == null : "textAttributesKey already set";
        myForcedTextAttributesKey = attributesKey;
        return this;
    }

    
    @Override
    public HighlightInfo.Builder unescapedToolTip(LocalizeValue unescapedToolTip) {
        assertNotCreated();
        assert myEscapedToolTip.isEmpty() : "Tooltip was already set";
        myEscapedToolTip = unescapedToolTip.map(HighlightInfoImpl::htmlEscapeToolTip);
        return this;
    }

    
    @Override
    public HighlightInfo.Builder escapedToolTip(LocalizeValue escapedToolTip) {
        assertNotCreated();
        assert this.myEscapedToolTip.isEmpty(): "Tooltip was already set";
        this.myEscapedToolTip = escapedToolTip;
        return this;
    }

    
    @Override
    public HighlightInfo.Builder range(int start, int end) {
        assertNotCreated();

        assert myStartOffset == -1 && myEndOffset == -1 : "Offsets already set";

        myStartOffset = start;
        myEndOffset = end;
        return this;
    }

    
    @Override
    @RequiredReadAction
    public HighlightInfo.Builder range(PsiElement element) {
        assertNotCreated();
        assert myPsiElement == null : " psiElement already set";
        myPsiElement = element;
        return range(element.getTextRange());
    }

    
    @Override
    public HighlightInfo.Builder range(PsiElement element, int start, int end) {
        assertNotCreated();
        assert myPsiElement == null : " psiElement already set";
        myPsiElement = element;
        return range(start, end);
    }

    
    @Override
    public HighlightInfo.Builder endOfLine() {
        assertNotCreated();
        myIsAfterEndOfLine = true;
        return this;
    }

    
    @Override
    public HighlightInfo.Builder needsUpdateOnTyping(boolean update) {
        assertNotCreated();
        assert myNeedsUpdateOnTyping == null : " needsUpdateOnTyping already set";
        myNeedsUpdateOnTyping = update;
        return this;
    }

    
    @Override
    public HighlightInfo.Builder severity(HighlightSeverity severity) {
        assertNotCreated();
        assert this.mySeverity == null : " severity already set";
        this.mySeverity = severity;
        return this;
    }

    
    @Override
    public HighlightInfo.Builder fileLevelAnnotation() {
        assertNotCreated();
        myIsFileLevelAnnotation = true;
        return this;
    }

    
    @Override
    public HighlightInfo.Builder navigationShift(int navigationShift) {
        assertNotCreated();
        this.myNavigationShift = navigationShift;
        return this;
    }

    
    @Override
    public HighlightInfo.Builder group(int group) {
        assertNotCreated();
        this.myGroup = group;
        return this;
    }

    
    @Override
    public HighlightInfo.Builder registerFix(
        IntentionAction action,
        @Nullable List<IntentionAction> options,
        LocalizeValue displayName,
        @Nullable TextRange fixRange,
        @Nullable HighlightDisplayKey key
    ) {
        assertNotCreated();
        myFixes.add(new FixInfo(action, options, displayName, fixRange, key));
        return this;
    }

    
    @Override
    public FixBuilder newFix(IntentionAction action) {
        assertNotCreated();
        return new MyFixBuilder(action);
    }

    @Nullable
    @Override
    public HighlightInfoImpl create() {
        HighlightInfoImpl info = createUnconditionally();
        LOG.assertTrue(
            myPsiElement != null
                || mySeverity == HighlightInfoType.SYMBOL_TYPE_SEVERITY
                || mySeverity == HighlightInfoType.INJECTED_FRAGMENT_SEVERITY
                || ArrayUtil.find(HighlightSeverity.DEFAULT_SEVERITIES, mySeverity) != -1,
            "Custom type requires not-null element to detect its text attributes"
        );

        if (!HighlightInfoImpl.isAcceptedByFilters(info, myPsiElement)) {
            return null;
        }

        return info;
    }

    
    @Override
    public HighlightInfoImpl createUnconditionally() {
        assertNotCreated();
        myCreated = true;

        if (mySeverity == null) {
            mySeverity = myType.getSeverity(myPsiElement);
        }

        HighlightInfoImpl info = new HighlightInfoImpl(
            myForcedTextAttributes,
            myForcedTextAttributesKey,
            myType,
            myStartOffset,
            myEndOffset,
            myEscapedDescription,
            myEscapedToolTip,
            mySeverity,
            myIsAfterEndOfLine,
            myNeedsUpdateOnTyping,
            myIsFileLevelAnnotation,
            myNavigationShift,
            myProblemGroup,
            myInspectionToolId,
            myGutterIconRenderer,
            myGroup
        );
        for (FixInfo fix : myFixes) {
            info.registerFix(fix.action(), fix.options(), fix.displayName(), fix.fixRange(), fix.key());
        }
        return info;
    }
}
