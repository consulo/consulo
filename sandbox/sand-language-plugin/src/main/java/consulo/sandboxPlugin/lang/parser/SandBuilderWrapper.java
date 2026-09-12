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
package consulo.sandboxPlugin.lang.parser;

import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;
import consulo.language.parser.PsiBuilderAdapter;
import consulo.sandboxPlugin.lang.psi.SandTokens;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

/**
 * The consulo-csharp {@code CSharpBuilderWrapper} mechanism for sand: preprocessor directives are
 * evaluated during the build against the seeded flag variables; inside a false branch every
 * non-directive token is remapped to {@link SandTokens#NON_ACTIVE_SYMBOL} and consumed before the
 * parser ever sees it. Disabled content therefore produces no PSI, no stubs and no errors — its
 * text survives only as inert comment-category leaves.
 */
public class SandBuilderWrapper extends PsiBuilderAdapter {
    public static final Key<Set<String>> SEED_VARIABLES = Key.create("sand.seed.variables");

    private final SandPreprocessorState myState = new SandPreprocessorState();
    private final Set<String> myVariables;
    private int myLastProcessedOffset = -1;
    private int myProtectedOffset = -1;
    private boolean myCurrentDirectiveDisabled;

    public SandBuilderWrapper(PsiBuilder delegate) {
        super(delegate);
        Set<String> seed = delegate.getUserData(SEED_VARIABLES);
        myVariables = seed == null ? new HashSet<>() : new HashSet<>(seed);
    }

    /**
     * True while the parser is on a directive that sits inside a disabled region — its structural
     * expectations must not produce error elements there.
     */
    public boolean isCurrentDirectiveDisabled() {
        return myCurrentDirectiveDisabled;
    }

    @Override
    public @Nullable IElementType getTokenType() {
        skipDisabled();
        return super.getTokenType();
    }

    @Override
    public boolean eof() {
        skipDisabled();
        return super.eof();
    }

    private void skipDisabled() {
        while (!super.eof()) {
            IElementType type = super.getTokenType();
            int offset = getCurrentOffset();

            if (isDirectiveKeyword(type)) {
                if (offset > myLastProcessedOffset) {
                    processDirective(type, offset);
                }
                return;
            }
            if (offset == myProtectedOffset) {
                return;
            }
            if (myState.isDisabled(false)) {
                remapCurrentToken(SandTokens.NON_ACTIVE_SYMBOL);
                super.advanceLexer();
                continue;
            }
            return;
        }
    }

    private void processDirective(IElementType type, int offset) {
        myLastProcessedOffset = offset;
        boolean disabled = myState.isDisabled(false);
        myCurrentDirectiveDisabled = disabled;

        String name = null;
        int step = 1;
        IElementType lookAhead;
        while ((lookAhead = rawLookup(step)) == SandTokens.WHITE_SPACE) {
            step++;
        }
        if (lookAhead == SandTokens.IDENTIFIER) {
            int start = rawTokenTypeStart(step);
            int end = rawTokenTypeStart(step + 1);
            name = getOriginalText().subSequence(start, end).toString();
            myProtectedOffset = start;
        }

        if (type == SandTokens.FLAG_KEYWORD) {
            if (!disabled && name != null) {
                myVariables.add(name);
            }
        }
        else if (type == SandTokens.UNDEF_KEYWORD) {
            if (!disabled && name != null) {
                myVariables.remove(name);
            }
        }
        else if (type == SandTokens.IF_KEYWORD || type == SandTokens.IFNDEF_KEYWORD) {
            boolean defined = name != null && myVariables.contains(name);
            boolean evaluate = name != null && ((type == SandTokens.IF_KEYWORD) == defined);
            myState.newState(disabled ? Boolean.FALSE : evaluate);
        }
        else if (type == SandTokens.ELIF_KEYWORD) {
            SandPreprocessorState.SubState state = myState.last();
            boolean outerDisabled = myState.isDisabled(true);
            boolean evaluate = name != null && myVariables.contains(name);
            if (state == null) {
                myState.newState(outerDisabled ? Boolean.FALSE : evaluate);
            }
            else if (state.haveActive()) {
                state.addSegment(Boolean.FALSE);
            }
            else {
                state.addSegment(outerDisabled ? Boolean.FALSE : evaluate);
            }
        }
        else if (type == SandTokens.ELSE_KEYWORD) {
            SandPreprocessorState.SubState state = myState.last();
            boolean outerDisabled = myState.isDisabled(true);
            if (state == null) {
                myState.newState(Boolean.FALSE);
            }
            else if (state.haveActive()) {
                state.addSegment(Boolean.FALSE);
            }
            else {
                state.addSegment(outerDisabled ? Boolean.FALSE : Boolean.TRUE);
            }
        }
        else if (type == SandTokens.END_KEYWORD) {
            myState.removeLast();
        }
    }

    private static boolean isDirectiveKeyword(@Nullable IElementType type) {
        return type == SandTokens.FLAG_KEYWORD
            || type == SandTokens.UNDEF_KEYWORD
            || type == SandTokens.IF_KEYWORD
            || type == SandTokens.IFNDEF_KEYWORD
            || type == SandTokens.ELIF_KEYWORD
            || type == SandTokens.ELSE_KEYWORD
            || type == SandTokens.END_KEYWORD
            || type == SandTokens.INCLUDE_KEYWORD;
    }
}
