/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.language.impl.internal.parser;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.language.ast.*;
import consulo.language.impl.ast.*;
import consulo.language.impl.internal.psi.diff.*;
import consulo.language.impl.psi.ForeignLeafPsiElement;
import consulo.language.impl.psi.PsiWhiteSpaceImpl;
import consulo.language.lexer.Lexer;
import consulo.language.parser.*;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.CharTable;
import consulo.language.util.FlyweightCapableTreeStructure;
import consulo.language.version.LanguageVersion;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.LimitedPool;
import consulo.util.collection.Stack;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UnprotectedUserDataHolder;
import consulo.util.lang.*;
import consulo.util.lang.ref.SimpleReference;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * @author max
 */
public class PsiBuilderImpl extends UnprotectedUserDataHolder implements PsiBuilder {
    private static final Logger LOG = Logger.getInstance(PsiBuilderImpl.class);

    private static final Key<LazyParseableTokensCache> LAZY_PARSEABLE_TOKENS = Key.create("LAZY_PARSEABLE_TOKENS");

    private TokenSet myAnyLanguageWhitespaceTokens = TokenSet.EMPTY;

    private final Project myProject;
    private final LanguageVersion myLanguageVersion;
    private PsiFile myFile;

    private int[] myLexStarts;
    private IElementType[] myLexTypes;
    private int myCurrentLexeme;

    private final MyList myProduction = new MyList();

    private final Lexer myLexer;
    private final TokenSet myWhitespaces;
    private TokenSet myComments;

    private CharTable myCharTable;
    private final CharSequence myText;
    private final CharSequence myLastCommittedText;
    private final char[] myTextArray;
    private boolean myDebugMode;
    private int myLexemeCount;
    private boolean myTokenTypeChecked;
    private ITokenTypeRemapper myRemapper;
    private WhitespaceSkippedCallback myWhitespaceSkippedCallback;
    private ReparseMergeCustomComparator myReparseMergeCustomComparator;

    private final ASTNode myOriginalTree;
    private final MyTreeStructure myParentLightTree;
    private final int myOffset;

    private IElementType myCachedTokenType;

    private final IntObjectMap<LazyParseableToken> myChameleonCache = IntMaps.newIntObjectHashMap();

    private final LimitedPool<StartMarker> START_MARKERS = new LimitedPool<>(2000, new LimitedPool.ObjectFactory<StartMarker>() {
        @Nonnull
        @Override
        public StartMarker create() {
            return new StartMarker();
        }

        @Override
        public void cleanup(@Nonnull final StartMarker startMarker) {
            startMarker.clean();
        }
    });

    private final LimitedPool<DoneMarker> DONE_MARKERS = new LimitedPool<>(2000, new LimitedPool.ObjectFactory<DoneMarker>() {
        @Nonnull
        @Override
        public DoneMarker create() {
            return new DoneMarker();
        }

        @Override
        public void cleanup(@Nonnull final DoneMarker doneMarker) {
            doneMarker.clean();
        }
    });

    public PsiBuilderImpl(
        @Nullable Project project,
        @Nullable PsiFile containingFile,
        @Nonnull ParserDefinition parserDefinition,
        @Nonnull Lexer lexer,
        @Nonnull LanguageVersion languageVersion,
        @Nullable CharTable charTable,
        @Nonnull final CharSequence text,
        @Nullable ASTNode originalTree,
        @Nullable MyTreeStructure parentLightTree
    ) {
        this(
            project,
            containingFile,
            languageVersion,
            parserDefinition,
            lexer,
            charTable,
            text,
            originalTree,
            originalTree == null ? null : originalTree.getText(),
            parentLightTree,
            null
        );
    }

    public PsiBuilderImpl(
        @Nonnull Project project,
        @Nonnull ParserDefinition parserDefinition,
        @Nonnull LanguageVersion languageVersion,
        @Nonnull Lexer lexer,
        @Nonnull ASTNode chameleon,
        @Nonnull CharSequence text
    ) {
        this(
            project,
            SharedImplUtil.getContainingFile(chameleon),
            languageVersion,
            parserDefinition,
            lexer,
            SharedImplUtil.findCharTableByTree(chameleon),
            text,
            Pair.getFirst(chameleon.getUserData(BlockSupport.TREE_TO_BE_REPARSED)),
            Pair.getSecond(chameleon.getUserData(BlockSupport.TREE_TO_BE_REPARSED)),
            null,
            chameleon
        );
    }

    public PsiBuilderImpl(
        @Nonnull Project project,
        @Nonnull ParserDefinition parserDefinition,
        @Nonnull LanguageVersion languageVersion,
        @Nonnull Lexer lexer,
        @Nonnull LighterLazyParseableNode chameleon,
        @Nonnull CharSequence text
    ) {
        this(
            project,
            chameleon.getContainingFile(),
            languageVersion,
            parserDefinition,
            lexer,
            chameleon.getCharTable(),
            text,
            null,
            null,
            ((LazyParseableToken)chameleon).myParentStructure,
            chameleon
        );
    }

    private PsiBuilderImpl(
        @Nullable Project project,
        @Nullable PsiFile containingFile,
        @Nonnull LanguageVersion languageVersion,
        @Nonnull ParserDefinition parserDefinition,
        @Nonnull Lexer lexer,
        @Nullable CharTable charTable,
        @Nonnull CharSequence text,
        @Nullable ASTNode originalTree,
        @Nullable CharSequence lastCommittedText,
        @Nullable MyTreeStructure parentLightTree,
        @Nullable Object parentCachingNode
    ) {
        myProject = project;
        myFile = containingFile;
        myLanguageVersion = languageVersion;
        myText = text;
        myTextArray = CharArrayUtil.fromSequenceWithoutCopying(text);
        myLexer = lexer;

        myWhitespaces = parserDefinition.getWhitespaceTokens(languageVersion);
        myComments = parserDefinition.getCommentTokens(languageVersion);
        myCharTable = charTable;
        myOriginalTree = originalTree;
        myLastCommittedText = lastCommittedText;
        if ((originalTree == null) != (lastCommittedText == null)) {
            throw new IllegalArgumentException(
                "originalTree and lastCommittedText must be null/notnull together but got: originalTree=" + originalTree +
                "; lastCommittedText=" +
                (lastCommittedText == null ? null : "'" + StringUtil.first(lastCommittedText, 80, true) + "'")
            );
        }
        myParentLightTree = parentLightTree;
        myOffset = parentCachingNode instanceof LazyParseableToken lazyParseableToken ? lazyParseableToken.getStartOffset() : 0;

        cacheLexemes(parentCachingNode);
    }

    private void cacheLexemes(@Nullable Object parentCachingNode) {
        int[] lexStarts = null;
        IElementType[] lexTypes = null;
        int lexemeCount = -1;
        // set this to true to check that re-lexing of lazy parseables produces the same sequence as cached one
        boolean doLexingOptimizationCorrectionCheck = false;

        if (parentCachingNode instanceof LazyParseableToken parentToken) {
            // there are two types of lazy parseable tokens out there: collapsed out of individual tokens
            // or single token that needs to be expanded
            // in first case parent PsiBuilder has all our text lexed so no need to do it again
            int tokenCount = parentToken.myEndIndex - parentToken.myStartIndex;
            if (tokenCount != 1) { // not expand single lazy parseable token case
                lexStarts = new int[tokenCount + 1];
                System.arraycopy(parentToken.myBuilder.myLexStarts, parentToken.myStartIndex, lexStarts, 0, tokenCount);
                int diff = parentToken.myBuilder.myLexStarts[parentToken.myStartIndex];
                for (int i = 0; i < tokenCount; ++i) lexStarts[i] -= diff;
                lexStarts[tokenCount] = myText.length();

                lexTypes = new IElementType[tokenCount];
                System.arraycopy(parentToken.myBuilder.myLexTypes, parentToken.myStartIndex, lexTypes, 0, tokenCount);
                lexemeCount = tokenCount;
            }
            ProgressIndicatorProvider.checkCanceled();

            //noinspection ConstantConditions
            if (!doLexingOptimizationCorrectionCheck && lexemeCount != -1) {
                myLexStarts = lexStarts;
                myLexTypes = lexTypes;
                myLexemeCount = lexemeCount;
                return;
            }
        }
        else if (parentCachingNode instanceof LazyParseableElement parentElement) {
            final LazyParseableTokensCache cachedTokens = parentElement.getUserData(LAZY_PARSEABLE_TOKENS);
            parentElement.putUserData(LAZY_PARSEABLE_TOKENS, null);
            //noinspection ConstantConditions
            if (!doLexingOptimizationCorrectionCheck && cachedTokens != null) {
                myLexStarts = cachedTokens.myLexStarts;
                myLexTypes = cachedTokens.myLexTypes;
                myLexemeCount = myLexTypes.length;
                return;
            }
        }

        int approxLexCount = Math.max(10, myText.length() / 5);

        myLexStarts = new int[approxLexCount];
        myLexTypes = new IElementType[approxLexCount];

        myLexer.start(myText);
        int i = 0;
        int offset = 0;
        while (true) {
            IElementType type = myLexer.getTokenType();
            if (type == null) {
                break;
            }

            if (i % 20 == 0) {
                ProgressIndicatorProvider.checkCanceled();
            }

            if (i >= myLexTypes.length - 1) {
                resizeLexemes(i * 3 / 2);
            }
            int tokenStart = myLexer.getTokenStart();
            if (tokenStart < offset) {
                final StringBuilder sb = new StringBuilder();
                final IElementType tokenType = myLexer.getTokenType();
                sb.append("Token sequence broken")
                    .append("\n  this: '")
                    .append(myLexer.getTokenText())
                    .append("' (")
                    .append(tokenType)
                    .append(':')
                    .append(tokenType != null ? tokenType.getLanguage() : null)
                    .append(") ")
                    .append(tokenStart)
                    .append(":")
                    .append(myLexer.getTokenEnd());
                if (i > 0) {
                    final int prevStart = myLexStarts[i - 1];
                    sb.append("\n  prev: '")
                        .append(myText.subSequence(prevStart, offset))
                        .append("' (")
                        .append(myLexTypes[i - 1])
                        .append(':')
                        .append(myLexTypes[i - 1].getLanguage())
                        .append(") ")
                        .append(prevStart)
                        .append(":")
                        .append(offset);
                }
                final int quoteStart = Math.max(tokenStart - 256, 0);
                final int quoteEnd = Math.min(tokenStart + 256, myText.length());
                sb.append("\n  quote: [")
                    .append(quoteStart)
                    .append(':')
                    .append(quoteEnd)
                    .append("] '")
                    .append(myText.subSequence(quoteStart, quoteEnd))
                    .append('\'');
                LOG.error(sb);
            }
            myLexStarts[i] = offset = tokenStart;
            myLexTypes[i] = type;
            i++;
            myLexer.advance();
        }

        myLexStarts[i] = myText.length();

        myLexemeCount = i;
        clearCachedTokenType();

        //noinspection ConstantConditions
        if (doLexingOptimizationCorrectionCheck && lexemeCount != -1) {
            assert lexemeCount == myLexemeCount;
            for (int j = 0; j < lexemeCount; ++j) {
                if (myLexStarts[j] != lexStarts[j] || myLexTypes[j] != lexTypes[j]) {
                    assert false;
                }
            }
            assert myLexStarts[lexemeCount] == lexStarts[lexemeCount];
        }
    }

    @Override
    public Project getProject() {
        return myProject;
    }

    @Override
    public void enforceCommentTokens(@Nonnull TokenSet tokens) {
        myComments = tokens;
    }

    @Override
    @Nullable
    public LighterASTNode getLatestDoneMarker() {
        int index = myProduction.size() - 1;
        while (index >= 0) {
            ProductionMarker marker = myProduction.get(index);
            if (marker instanceof DoneMarker doneMarker) {
                return doneMarker.myStart;
            }
            --index;
        }
        return null;
    }

    private abstract static class Node implements LighterASTNode {
        public abstract int hc();
    }

    public abstract static class ProductionMarker extends Node {
        protected int myLexemeIndex;
        protected WhitespacesAndCommentsBinder myEdgeTokenBinder;
        protected ProductionMarker myParent;
        protected ProductionMarker myNext;

        public void clean() {
            myLexemeIndex = 0;
            myParent = myNext = null;
        }

        public void remapTokenType(@Nonnull IElementType type) {
            throw new UnsupportedOperationException("Shall not be called on this kind of markers");
        }

        public int getStartIndex() {
            return myLexemeIndex;
        }

        public int getEndIndex() {
            throw new UnsupportedOperationException("Shall not be called on this kind of markers");
        }
    }

    private static class StartMarker extends ProductionMarker implements Marker {
        private PsiBuilderImpl myBuilder;
        private IElementType myType;
        private DoneMarker myDoneMarker;
        private Throwable myDebugAllocationPosition;
        private ProductionMarker myFirstChild;
        private ProductionMarker myLastChild;
        private int myHC = -1;

        private StartMarker() {
            myEdgeTokenBinder = WhitespacesBinders.DEFAULT_LEFT_BINDER;
        }

        @Override
        public void clean() {
            super.clean();
            myBuilder = null;
            myType = null;
            myDoneMarker = null;
            myDebugAllocationPosition = null;
            myFirstChild = myLastChild = null;
            myHC = -1;
            myEdgeTokenBinder = WhitespacesBinders.DEFAULT_LEFT_BINDER;
        }

        @Override
        public int hc() {
            if (myHC == -1) {
                PsiBuilderImpl builder = myBuilder;
                int hc = 0;
                final CharSequence buf = builder.myText;
                final char[] bufArray = builder.myTextArray;
                ProductionMarker child = myFirstChild;
                int lexIdx = myLexemeIndex;

                while (child != null) {
                    int lastLeaf = child.myLexemeIndex;
                    for (int i = builder.myLexStarts[lexIdx]; i < builder.myLexStarts[lastLeaf]; i++) {
                        hc += bufArray != null ? bufArray[i] : buf.charAt(i);
                    }
                    lexIdx = lastLeaf;
                    hc += child.hc();
                    if (child instanceof StartMarker startMarker) {
                        lexIdx = startMarker.myDoneMarker.myLexemeIndex;
                    }
                    child = child.myNext;
                }

                for (int i = builder.myLexStarts[lexIdx]; i < builder.myLexStarts[myDoneMarker.myLexemeIndex]; i++) {
                    hc += bufArray != null ? bufArray[i] : buf.charAt(i);
                }

                myHC = hc;
            }

            return myHC;
        }

        @Override
        public int getStartOffset() {
            return myBuilder.myLexStarts[myLexemeIndex] + myBuilder.myOffset;
        }

        @Override
        public int getEndOffset() {
            return myBuilder.myLexStarts[myDoneMarker.myLexemeIndex] + myBuilder.myOffset;
        }

        @Override
        public int getEndIndex() {
            return myDoneMarker.myLexemeIndex;
        }

        public void addChild(@Nonnull ProductionMarker node) {
            if (myFirstChild == null) {
                myFirstChild = node;
                myLastChild = node;
            }
            else {
                myLastChild.myNext = node;
                myLastChild = node;
            }
        }

        @Nonnull
        @Override
        public Marker precede() {
            return myBuilder.precede(this);
        }

        @Override
        public void drop() {
            myBuilder.drop(this);
        }

        @Override
        public void rollbackTo() {
            myBuilder.rollbackTo(this);
        }

        @Override
        public void done(@Nonnull IElementType type) {
            myType = type;
            myBuilder.done(this);
        }

        @Override
        public void collapse(@Nonnull IElementType type) {
            myType = type;
            myBuilder.collapse(this);
        }

        @Override
        public void doneBefore(@Nonnull IElementType type, @Nonnull Marker before) {
            myType = type;
            myBuilder.doneBefore(this, before);
        }

        @Override
        public void doneBefore(@Nonnull final IElementType type, @Nonnull final Marker before, @Nonnull final LocalizeValue errorMessage) {
            StartMarker marker = (StartMarker)before;
            myBuilder.myProduction.add(
                myBuilder.myProduction.lastIndexOf(marker),
                new ErrorItem(myBuilder, errorMessage, marker.myLexemeIndex)
            );
            doneBefore(type, before);
        }

        @Override
        public void error(@Nonnull LocalizeValue message) {
            myType = TokenType.ERROR_ELEMENT;
            myBuilder.error(this, message);
        }

        @Override
        public void errorBefore(@Nonnull final LocalizeValue message, @Nonnull final Marker before) {
            myType = TokenType.ERROR_ELEMENT;
            myBuilder.errorBefore(this, message, before);
        }

        @Override
        public IElementType getTokenType() {
            return myType;
        }

        @Override
        public void remapTokenType(@Nonnull IElementType type) {
            //assert myType != null && type != null;
            myType = type;
        }

        @Override
        public void setCustomEdgeTokenBinders(final WhitespacesAndCommentsBinder left, final WhitespacesAndCommentsBinder right) {
            if (left != null) {
                myEdgeTokenBinder = left;
            }

            if (right != null) {
                if (myDoneMarker == null) {
                    throw new IllegalArgumentException("Cannot set right-edge processor for unclosed marker");
                }
                myDoneMarker.myEdgeTokenBinder = right;
            }
        }

        @Override
        public String toString() {
            if (myBuilder == null) {
                return "<dropped>";
            }
            boolean isDone = myDoneMarker != null;
            CharSequence originalText = myBuilder.getOriginalText();
            int startOffset = getStartOffset() - myBuilder.myOffset;
            int endOffset = isDone ? getEndOffset() - myBuilder.myOffset : myBuilder.getCurrentOffset();
            CharSequence text = originalText.subSequence(startOffset, endOffset);
            return isDone ? text.toString() : text + "\u2026";
        }
    }

    @Nonnull
    private Marker precede(final StartMarker marker) {
        int idx = myProduction.lastIndexOf(marker);
        if (idx < 0) {
            LOG.error("Cannot precede dropped or rolled-back marker");
        }
        StartMarker pre = createMarker(marker.myLexemeIndex);
        myProduction.add(idx, pre);
        return pre;
    }

    private abstract static class Token extends Node {
        protected PsiBuilderImpl myBuilder;
        private IElementType myTokenType;
        private int myTokenStart;
        private int myTokenEnd;
        private int myHC = -1;
        private StartMarker myParentNode;

        public void clean() {
            myBuilder = null;
            myHC = -1;
            myParentNode = null;
        }

        @Override
        public int hc() {
            if (myHC == -1) {
                int hc = 0;
                if (myTokenType instanceof TokenWrapper tokenWrapper) {
                    final CharSequence value = tokenWrapper.getValue();
                    for (int i = 0; i < value.length(); i++) {
                        hc += value.charAt(i);
                    }
                }
                else {
                    final int start = myTokenStart;
                    final int end = myTokenEnd;
                    final CharSequence buf = myBuilder.myText;
                    final char[] bufArray = myBuilder.myTextArray;

                    for (int i = start; i < end; i++) {
                        hc += bufArray != null ? bufArray[i] : buf.charAt(i);
                    }
                }

                myHC = hc;
            }

            return myHC;
        }

        @Override
        public int getEndOffset() {
            return myTokenEnd + myBuilder.myOffset;
        }

        @Override
        public int getStartOffset() {
            return myTokenStart + myBuilder.myOffset;
        }

        @Nonnull
        public CharSequence getText() {
            if (myTokenType instanceof TokenWrapper tokenWrapper) {
                return tokenWrapper.getValue();
            }

            return myBuilder.myText.subSequence(myTokenStart, myTokenEnd);
        }

        @Nonnull
        @Override
        public IElementType getTokenType() {
            return myTokenType;
        }

        void initToken(@Nonnull IElementType type, @Nonnull PsiBuilderImpl builder, StartMarker parent, int start, int end) {
            myParentNode = parent;
            myBuilder = builder;
            myTokenType = type;
            myTokenStart = start;
            myTokenEnd = end;
        }
    }

    private static class TokenNode extends Token implements LighterASTTokenNode {
        @Override
        public String toString() {
            return getText().toString();
        }
    }

    private static class LazyParseableToken extends Token implements LighterLazyParseableNode {
        private MyTreeStructure myParentStructure;
        private FlyweightCapableTreeStructure<LighterASTNode> myParsed;
        private int myStartIndex;
        private int myEndIndex;

        @Override
        public void clean() {
            myBuilder.myChameleonCache.remove(getStartOffset());
            super.clean();
            myParentStructure = null;
            myParsed = null;
        }

        @Override
        public PsiFile getContainingFile() {
            return myBuilder.myFile;
        }

        @Override
        public CharTable getCharTable() {
            return myBuilder.myCharTable;
        }

        public FlyweightCapableTreeStructure<LighterASTNode> parseContents() {
            if (myParsed == null) {
                myParsed = ((ILightLazyParseableElementType)getTokenType()).parseContents(this);
            }
            return myParsed;
        }

        @Override
        public boolean accept(@Nonnull Visitor visitor) {
            for (int i = myStartIndex; i < myEndIndex; i++) {
                IElementType type = myBuilder.myLexTypes[i];
                if (!visitor.visit(type)) {
                    return false;
                }
            }

            return true;
        }
    }

    private static class DoneMarker extends ProductionMarker {
        private StartMarker myStart;
        private boolean myCollapse;

        DoneMarker() {
            myEdgeTokenBinder = WhitespacesBinders.DEFAULT_RIGHT_BINDER;
        }

        DoneMarker(final StartMarker marker, final int currentLexeme) {
            this();
            myLexemeIndex = currentLexeme;
            myStart = marker;
        }

        @Override
        public void clean() {
            super.clean();
            myStart = null;
            myEdgeTokenBinder = WhitespacesBinders.DEFAULT_RIGHT_BINDER;
        }

        @Override
        public int hc() {
            throw new UnsupportedOperationException("Shall not be called on this kind of markers");
        }

        @Nonnull
        @Override
        public IElementType getTokenType() {
            throw new UnsupportedOperationException("Shall not be called on this kind of markers");
        }

        @Override
        public int getEndOffset() {
            throw new UnsupportedOperationException("Shall not be called on this kind of markers");
        }

        @Override
        public int getStartOffset() {
            throw new UnsupportedOperationException("Shall not be called on this kind of markers");
        }
    }

    private static class DoneWithErrorMarker extends DoneMarker {
        private final LocalizeValue myMessage;

        private DoneWithErrorMarker(@Nonnull StartMarker marker, final int currentLexeme, @Nonnull LocalizeValue message) {
            super(marker, currentLexeme);
            myMessage = message;
        }
    }

    private static class ErrorItem extends ProductionMarker {
        private final PsiBuilderImpl myBuilder;
        private final LocalizeValue myMessage;

        ErrorItem(final PsiBuilderImpl builder, final LocalizeValue message, final int idx) {
            myBuilder = builder;
            myMessage = message;
            myLexemeIndex = idx;
            myEdgeTokenBinder = WhitespacesBinders.DEFAULT_RIGHT_BINDER;
        }

        @Override
        public int hc() {
            return 0;
        }

        @Override
        public int getEndOffset() {
            return myBuilder.myLexStarts[myLexemeIndex] + myBuilder.myOffset;
        }

        @Override
        public int getStartOffset() {
            return myBuilder.myLexStarts[myLexemeIndex] + myBuilder.myOffset;
        }

        @Nonnull
        @Override
        public IElementType getTokenType() {
            return TokenType.ERROR_ELEMENT;
        }
    }

    @Nonnull
    @Override
    public CharSequence getOriginalText() {
        return myText;
    }

    @Override
    @Nullable
    public IElementType getTokenType() {
        IElementType cached = myCachedTokenType;
        if (cached == null) {
            myCachedTokenType = cached = calcTokenType();
        }
        return cached;
    }

    private void clearCachedTokenType() {
        myCachedTokenType = null;
    }

    private IElementType remapCurrentToken() {
        if (myCachedTokenType != null) {
            return myCachedTokenType;
        }
        if (myRemapper != null) {
            remapCurrentToken(myRemapper.filter(
                myLexTypes[myCurrentLexeme],
                myLexStarts[myCurrentLexeme],
                myLexStarts[myCurrentLexeme + 1],
                myLexer.getBufferSequence()
            ));
        }
        return myLexTypes[myCurrentLexeme];
    }

    private IElementType calcTokenType() {
        if (eof()) {
            return null;
        }

        if (myRemapper != null) {
            //remaps current token, and following, which remaps to spaces and comments
            skipWhitespace();
        }
        return myLexTypes[myCurrentLexeme];
    }

    @Override
    public void setTokenTypeRemapper(ITokenTypeRemapper remapper) {
        myRemapper = remapper;
        myTokenTypeChecked = false;
        clearCachedTokenType();
    }

    @Override
    public void remapCurrentToken(IElementType type) {
        myLexTypes[myCurrentLexeme] = type;
        clearCachedTokenType();
    }

    @Nullable
    @Override
    public IElementType lookAhead(int steps) {
        if (eof()) {    // ensure we skip over whitespace if it's needed
            return null;
        }
        int cur = myCurrentLexeme;

        while (steps > 0) {
            ++cur;
            while (cur < myLexemeCount && whitespaceOrComment(myLexTypes[cur])) {
                cur++;
            }

            steps--;
        }

        return cur < myLexemeCount ? myLexTypes[cur] : null;
    }

    @Override
    public IElementType rawLookup(int steps) {
        int cur = myCurrentLexeme + steps;
        return cur < myLexemeCount && cur >= 0 ? myLexTypes[cur] : null;
    }

    @Override
    public int rawTokenTypeStart(int steps) {
        int cur = myCurrentLexeme + steps;
        if (cur < 0) {
            return -1;
        }
        if (cur >= myLexemeCount) {
            return getOriginalText().length();
        }
        return myLexStarts[cur];
    }

    @Override
    public int rawTokenIndex() {
        return myCurrentLexeme;
    }

    @Override
    public void setWhitespaceSkippedCallback(@Nullable final WhitespaceSkippedCallback callback) {
        myWhitespaceSkippedCallback = callback;
    }

    @Override
    public boolean isWhitespaceOrCommentType(@Nonnull IElementType elementType) {
        return whitespaceOrComment(elementType);
    }

    @Override
    public void advanceLexer() {
        ProgressIndicatorProvider.checkCanceled();

        if (eof()) {
            return;
        }

        if (!myTokenTypeChecked) {
            LOG.error("Probably a bug: eating token without its type checking");
        }

        myTokenTypeChecked = false;
        myCurrentLexeme++;
        clearCachedTokenType();
    }

    private void skipWhitespace() {
        while (myCurrentLexeme < myLexemeCount && whitespaceOrComment(remapCurrentToken())) {
            onSkip(
                myLexTypes[myCurrentLexeme],
                myLexStarts[myCurrentLexeme],
                myCurrentLexeme + 1 < myLexemeCount ? myLexStarts[myCurrentLexeme + 1] : myText.length()
            );
            myCurrentLexeme++;
            clearCachedTokenType();
        }
    }

    private void onSkip(IElementType type, int start, int end) {
        if (myWhitespaceSkippedCallback != null) {
            myWhitespaceSkippedCallback.onSkip(type, start, end);
        }
    }

    @Override
    public int getCurrentOffset() {
        if (eof()) {
            return getOriginalText().length();
        }
        return myLexStarts[myCurrentLexeme];
    }

    @Override
    @Nullable
    public String getTokenText() {
        CharSequence tokenSequence = getTokenSequence();
        return tokenSequence == null ? null : tokenSequence.toString();
    }

    @Nullable
    @Override
    public CharSequence getTokenSequence() {
        if (eof()) {
            return null;
        }
        return getTokenType() instanceof TokenWrapper tokenWrapper
            ? tokenWrapper.getValue()
            : myText.subSequence(myLexStarts[myCurrentLexeme], myLexStarts[myCurrentLexeme + 1]);
    }

    private void resizeLexemes(final int newSize) {
        myLexStarts = ArrayUtil.realloc(myLexStarts, newSize + 1);
        myLexTypes = ArrayUtil.realloc(myLexTypes, newSize, IElementType.ARRAY_FACTORY);
        clearCachedTokenType();
    }

    public boolean whitespaceOrComment(IElementType token) {
        return myWhitespaces.contains(token) || myComments.contains(token);
    }

    @Nonnull
    @Override
    public Marker mark() {
        if (!myProduction.isEmpty()) {
            skipWhitespace();
        }
        StartMarker marker = createMarker(myCurrentLexeme);

        myProduction.add(marker);
        return marker;
    }

    @Nonnull
    private StartMarker createMarker(final int lexemeIndex) {
        StartMarker marker = START_MARKERS.alloc();
        marker.myLexemeIndex = lexemeIndex;
        marker.myBuilder = this;

        if (myDebugMode) {
            marker.myDebugAllocationPosition = new Throwable("Created at the following trace.");
        }
        return marker;
    }

    @Override
    public final boolean eof() {
        if (!myTokenTypeChecked) {
            myTokenTypeChecked = true;
            skipWhitespace();
        }
        return myCurrentLexeme >= myLexemeCount;
    }

    private void rollbackTo(@Nonnull Marker marker) {
        myCurrentLexeme = ((StartMarker)marker).myLexemeIndex;
        myTokenTypeChecked = true;
        int idx = myProduction.lastIndexOf(marker);
        if (idx < 0) {
            LOG.error("The marker must be added before rolled back to.");
        }
        myProduction.removeRange(idx, myProduction.size());
        START_MARKERS.recycle((StartMarker)marker);
        clearCachedTokenType();
    }

    /**
     * @return true if there are error elements created and not dropped after marker was created
     */
    public boolean hasErrorsAfter(@Nonnull Marker marker) {
        assert marker instanceof StartMarker;
        int idx = myProduction.lastIndexOf(marker);
        if (idx < 0) {
            LOG.error("The marker must be added before checked for errors.");
        }
        for (int i = idx + 1; i < myProduction.size(); ++i) {
            ProductionMarker m = myProduction.get(i);
            if (m instanceof ErrorItem || m instanceof DoneWithErrorMarker) {
                return true;
            }
        }
        return false;
    }

    public void drop(@Nonnull Marker marker) {
        final DoneMarker doneMarker = ((StartMarker)marker).myDoneMarker;
        if (doneMarker != null) {
            myProduction.remove(myProduction.lastIndexOf(doneMarker));
            DONE_MARKERS.recycle(doneMarker);
        }
        final boolean removed = myProduction.remove(myProduction.lastIndexOf(marker)) == marker;
        if (!removed) {
            LOG.error("The marker must be added before it is dropped.");
        }
        START_MARKERS.recycle((StartMarker)marker);
    }

    public void error(@Nonnull Marker marker, LocalizeValue message) {
        doValidityChecks(marker, null);

        StartMarker startMarker = (StartMarker)marker;
        DoneWithErrorMarker doneMarker = new DoneWithErrorMarker(startMarker, myCurrentLexeme, message);
        boolean tieToTheLeft = isEmpty(startMarker.myLexemeIndex, myCurrentLexeme);
        if (tieToTheLeft) {
            startMarker.myEdgeTokenBinder = WhitespacesBinders.DEFAULT_RIGHT_BINDER;
        }

        startMarker.myDoneMarker = doneMarker;
        myProduction.add(doneMarker);
    }

    private void errorBefore(@Nonnull Marker marker, @Nonnull LocalizeValue message, @Nonnull Marker before) {
        doValidityChecks(marker, before);

        @SuppressWarnings("SuspiciousMethodCalls") int beforeIndex = myProduction.lastIndexOf(before);

        StartMarker startMarker = (StartMarker)marker, beforeStartMarker = (StartMarker)before;
        DoneWithErrorMarker doneMarker = new DoneWithErrorMarker(startMarker, beforeStartMarker.myLexemeIndex, message);
        boolean tieToTheLeft = isEmpty(startMarker.myLexemeIndex, beforeStartMarker.myLexemeIndex);
        if (tieToTheLeft) {
            startMarker.myEdgeTokenBinder = WhitespacesBinders.DEFAULT_RIGHT_BINDER;
        }

        startMarker.myDoneMarker = doneMarker;
        myProduction.add(beforeIndex, doneMarker);
    }

    public void done(@Nonnull Marker marker) {
        doValidityChecks(marker, null);

        StartMarker startMarker = (StartMarker)marker;
        DoneMarker doneMarker = DONE_MARKERS.alloc();
        doneMarker.myStart = startMarker;
        doneMarker.myLexemeIndex = myCurrentLexeme;
        boolean tieToTheLeft = doneMarker.myStart.myType.isLeftBound() && isEmpty(startMarker.myLexemeIndex, myCurrentLexeme);
        if (tieToTheLeft) {
            startMarker.myEdgeTokenBinder = WhitespacesBinders.DEFAULT_RIGHT_BINDER;
        }

        startMarker.myDoneMarker = doneMarker;
        myProduction.add(doneMarker);
    }

    public void doneBefore(@Nonnull Marker marker, @Nonnull Marker before) {
        doValidityChecks(marker, before);

        @SuppressWarnings("SuspiciousMethodCalls") int beforeIndex = myProduction.lastIndexOf(before);

        StartMarker startMarker = (StartMarker)marker, beforeStartMarker = (StartMarker)before;
        DoneMarker doneMarker = DONE_MARKERS.alloc();
        doneMarker.myLexemeIndex = beforeStartMarker.myLexemeIndex;
        doneMarker.myStart = startMarker;
        boolean tieToTheLeft =
            doneMarker.myStart.myType.isLeftBound() && isEmpty(startMarker.myLexemeIndex, beforeStartMarker.myLexemeIndex);
        if (tieToTheLeft) {
            startMarker.myEdgeTokenBinder = WhitespacesBinders.DEFAULT_RIGHT_BINDER;
        }

        startMarker.myDoneMarker = doneMarker;
        myProduction.add(beforeIndex, doneMarker);
    }

    private boolean isEmpty(final int startIdx, final int endIdx) {
        for (int i = startIdx; i < endIdx; i++) {
            final IElementType token = myLexTypes[i];
            if (!whitespaceOrComment(token)) {
                return false;
            }
        }
        return true;
    }

    public void collapse(@Nonnull Marker marker) {
        done(marker);
        ((StartMarker)marker).myDoneMarker.myCollapse = true;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    private void doValidityChecks(@Nonnull Marker marker, @Nullable final Marker before) {
        StartMarker startMarker = (StartMarker)marker;
        final DoneMarker doneMarker = startMarker.myDoneMarker;
        if (doneMarker != null) {
            LOG.error("Marker already done.");
        }

        if (!myDebugMode) {
            return;
        }

        int idx = myProduction.lastIndexOf(marker);
        if (idx < 0) {
            LOG.error("Marker has never been added.");
        }

        int endIdx = myProduction.size();
        if (before != null) {
            //noinspection SuspiciousMethodCalls
            endIdx = myProduction.lastIndexOf(before);
            if (endIdx < 0) {
                LOG.error("'Before' marker has never been added.");
            }
            if (idx > endIdx) {
                LOG.error("'Before' marker precedes this one.");
            }
        }

        for (int i = endIdx - 1; i > idx; i--) {
            Object item = myProduction.get(i);
            if (item instanceof StartMarker otherMarker && otherMarker.myDoneMarker == null) {
                final Throwable debugAllocOther = otherMarker.myDebugAllocationPosition;
                final Throwable debugAllocThis = startMarker.myDebugAllocationPosition;
                if (debugAllocOther != null) {
                    Throwable currentTrace = new Throwable();
                    ExceptionUtil.makeStackTraceRelative(debugAllocThis, currentTrace).printStackTrace(System.err);
                    ExceptionUtil.makeStackTraceRelative(debugAllocOther, currentTrace).printStackTrace(System.err);
                }
                LOG.error("Another not done marker added after this one. Must be done before this.");
            }
        }
    }

    @Override
    public void error(@Nonnull LocalizeValue messageText) {
        final ProductionMarker lastMarker = myProduction.get(myProduction.size() - 1);
        if (lastMarker instanceof ErrorItem && lastMarker.myLexemeIndex == myCurrentLexeme) {
            return;
        }
        myProduction.add(new ErrorItem(this, messageText, myCurrentLexeme));
    }

    @Override
    @Nonnull
    public ASTNode getTreeBuilt() {
        return buildTree();
    }

    @Nonnull
    private ASTNode buildTree() {
        final StartMarker rootMarker = prepareLightTree();
        final boolean isTooDeep = myFile != null && BlockSupport.isTooDeep(myFile.getOriginalFile());

        if (myOriginalTree != null && !isTooDeep) {
            DiffLog diffLog = merge(myOriginalTree, rootMarker, myLastCommittedText);
            throw new BlockSupport.ReparsedSuccessfullyException(diffLog);
        }

        final TreeElement rootNode = createRootAST(rootMarker);
        bind(rootMarker, (CompositeElement)rootNode);

        if (isTooDeep && !(rootNode instanceof FileElement)) {
            final ASTNode childNode = rootNode.getFirstChildNode();
            childNode.putUserData(BlockSupport.TREE_DEPTH_LIMIT_EXCEEDED, Boolean.TRUE);
        }

        assert rootNode.getTextLength() == myText.length() : rootNode.getElementType();

        return rootNode;
    }

    @Override
    @Nonnull
    public FlyweightCapableTreeStructure<LighterASTNode> getLightTree() {
        final StartMarker rootMarker = prepareLightTree();
        return new MyTreeStructure(rootMarker, myParentLightTree);
    }

    @Nonnull
    private TreeElement createRootAST(@Nonnull StartMarker rootMarker) {
        final IElementType type = rootMarker.getTokenType();
        @SuppressWarnings("NullableProblems")
        final TreeElement rootNode = type instanceof ILazyParseableElementType lazyParseableElementType
            ? ASTFactory.lazy(lazyParseableElementType, null)
            : createComposite(rootMarker);
        if (myCharTable == null) {
            myCharTable = rootNode instanceof FileElement fileElement ? fileElement.getCharTable() : new CharTableImpl();
        }
        if (!(rootNode instanceof FileElement)) {
            rootNode.putUserData(CharTable.CHAR_TABLE_KEY, myCharTable);
        }
        return rootNode;
    }

    private static class ConvertFromTokensToASTBuilder implements DiffTreeChangeBuilder<ASTNode, LighterASTNode> {
        private final DiffTreeChangeBuilder<ASTNode, ASTNode> myDelegate;
        private final ASTConverter myConverter;

        private ConvertFromTokensToASTBuilder(@Nonnull StartMarker rootNode, @Nonnull DiffTreeChangeBuilder<ASTNode, ASTNode> delegate) {
            myDelegate = delegate;
            myConverter = new ASTConverter(rootNode);
        }

        @Override
        public void nodeDeleted(@Nonnull final ASTNode oldParent, @Nonnull final ASTNode oldNode) {
            myDelegate.nodeDeleted(oldParent, oldNode);
        }

        @Override
        public void nodeInserted(@Nonnull final ASTNode oldParent, @Nonnull final LighterASTNode newNode, final int pos) {
            myDelegate.nodeInserted(oldParent, myConverter.apply((Node)newNode), pos);
        }

        @Override
        public void nodeReplaced(@Nonnull final ASTNode oldChild, @Nonnull final LighterASTNode newChild) {
            ASTNode converted = myConverter.apply((Node)newChild);
            myDelegate.nodeReplaced(oldChild, converted);
        }
    }

    private static final String UNBALANCED_MESSAGE =
        "Unbalanced tree. Most probably caused by unbalanced markers. " +
            "Try calling setDebugMode(true) against PsiBuilder passed to identify exact location of the problem";

    @Nonnull
    private DiffLog merge(@Nonnull final ASTNode oldRoot, @Nonnull StartMarker newRoot, @Nonnull CharSequence lastCommittedText) {
        DiffLog diffLog = new DiffLog();
        DiffTreeChangeBuilder<ASTNode, LighterASTNode> builder = new ConvertFromTokensToASTBuilder(newRoot, diffLog);
        MyTreeStructure treeStructure = new MyTreeStructure(newRoot, null);
        ShallowNodeComparator<ASTNode, LighterASTNode> comparator =
            new MyComparator(myAnyLanguageWhitespaceTokens, myReparseMergeCustomComparator, treeStructure);

        ProgressIndicator indicator = ProgressIndicatorProvider.getGlobalProgressIndicator();
        BlockSupportImpl.diffTrees(
            oldRoot,
            builder,
            comparator,
            treeStructure,
            indicator == null ? new EmptyProgressIndicator() : indicator,
            lastCommittedText
        );
        return diffLog;
    }

    @Nonnull
    private StartMarker prepareLightTree() {
        if (myProduction.isEmpty()) {
            LOG.error("Parser produced no markers. Text:\n" + myText);
        }
        // build tree only once to avoid threading issues in read-only PSI
        StartMarker rootMarker = (StartMarker)myProduction.get(0);
        if (rootMarker.myFirstChild != null) {
            return rootMarker;
        }

        myTokenTypeChecked = true;
        balanceWhiteSpaces();

        rootMarker.myParent = rootMarker.myFirstChild = rootMarker.myLastChild = rootMarker.myNext = null;
        StartMarker curNode = rootMarker;
        final Stack<StartMarker> nodes = new Stack<>();
        nodes.push(rootMarker);

        int lastErrorIndex = -1;
        int maxDepth = 0;
        int curDepth = 0;
        for (int i = 1; i < myProduction.size(); i++) {
            final ProductionMarker item = myProduction.get(i);

            if (curNode == null) {
                LOG.error("Unexpected end of the production");
            }

            item.myParent = curNode;
            if (item instanceof StartMarker marker) {
                marker.myFirstChild = marker.myLastChild = marker.myNext = null;
                curNode.addChild(marker);
                nodes.push(curNode);
                curNode = marker;
                curDepth++;
                if (curDepth > maxDepth) {
                    maxDepth = curDepth;
                }
            }
            else if (item instanceof DoneMarker doneMarker) {
                assertMarkersBalanced(doneMarker.myStart == curNode, item);
                curNode = nodes.pop();
                curDepth--;
            }
            else if (item instanceof ErrorItem) {
                int curToken = item.myLexemeIndex;
                if (curToken == lastErrorIndex) {
                    continue;
                }
                lastErrorIndex = curToken;
                curNode.addChild(item);
            }
        }

        if (myCurrentLexeme < myLexemeCount) {
            final List<IElementType> missed = ContainerUtil.newArrayList(myLexTypes, myCurrentLexeme, myLexemeCount);
            LOG.error(
                "Tokens " + missed + " were not inserted into the tree. " +
                    (myFile != null ? myFile.getLanguage() + ", " : "") + "Text:\n" + myText
            );
        }

        if (rootMarker.myDoneMarker.myLexemeIndex < myLexemeCount) {
            final List<IElementType> missed = ContainerUtil.newArrayList(myLexTypes, rootMarker.myDoneMarker.myLexemeIndex, myLexemeCount);
            LOG.error("Tokens " + missed + " are outside of root element \"" + rootMarker.myType + "\". Text:\n" + myText);
        }

        if (myLexStarts.length <= myCurrentLexeme + 1) {
            resizeLexemes(myCurrentLexeme + 1);
        }

        myLexStarts[myCurrentLexeme] = myText.length(); // $ terminating token.;
        myLexStarts[myCurrentLexeme + 1] = 0;
        myLexTypes[myCurrentLexeme] = null;

        assertMarkersBalanced(curNode == rootMarker, curNode);

        checkTreeDepth(maxDepth, rootMarker.getTokenType() instanceof IFileElementType);

        clearCachedTokenType();
        return rootMarker;
    }

    private void assertMarkersBalanced(boolean condition, @Nullable ProductionMarker marker) {
        if (condition) {
            return;
        }

        int index = marker != null ? marker.getStartIndex() + 1 : myLexStarts.length;
        CharSequence context =
            index < myLexStarts.length ? myText.subSequence(Math.max(0, myLexStarts[index] - 1000), myLexStarts[index]) : "<none>";
        String language = myFile != null ? myFile.getLanguage() + ", " : "";
        LOG.error(UNBALANCED_MESSAGE + "\n" + "language: " + language + "\n" + "context: '" + context + "'");
    }

    private void balanceWhiteSpaces() {
        RelativeTokenTypesView wsTokens = new RelativeTokenTypesView();
        RelativeTokenTextView tokenTextGetter = new RelativeTokenTextView();
        int lastIndex = 0;

        for (int i = 1, size = myProduction.size() - 1; i < size; i++) {
            ProductionMarker item = myProduction.get(i);
            if (item instanceof StartMarker startMarker) {
                assertMarkersBalanced(startMarker.myDoneMarker != null, item);
            }

            boolean recursive = item.myEdgeTokenBinder instanceof WhitespacesAndCommentsBinder.RecursiveBinder;
            int prevProductionLexIndex = recursive ? 0 : myProduction.get(i - 1).myLexemeIndex;
            int wsStartIndex = Math.max(item.myLexemeIndex, lastIndex);
            while (wsStartIndex > prevProductionLexIndex && whitespaceOrComment(myLexTypes[wsStartIndex - 1])) wsStartIndex--;
            int wsEndIndex = item.myLexemeIndex;
            while (wsEndIndex < myLexemeCount && whitespaceOrComment(myLexTypes[wsEndIndex])) wsEndIndex++;

            if (wsStartIndex != wsEndIndex) {
                wsTokens.configure(wsStartIndex, wsEndIndex);
                tokenTextGetter.configure(wsStartIndex);
                boolean atEnd = wsStartIndex == 0 || wsEndIndex == myLexemeCount;
                item.myLexemeIndex = wsStartIndex + item.myEdgeTokenBinder.getEdgePosition(wsTokens, atEnd, tokenTextGetter);
                if (recursive) {
                    for (int k = i - 1; k > 1; k--) {
                        ProductionMarker prev = myProduction.get(k);
                        if (prev.myLexemeIndex >= item.myLexemeIndex) {
                            prev.myLexemeIndex = item.myLexemeIndex;
                        }
                        else {
                            break;
                        }
                    }
                }
            }
            else if (item.myLexemeIndex < wsStartIndex) {
                item.myLexemeIndex = wsStartIndex;
            }

            lastIndex = item.myLexemeIndex;
        }
    }

    private final class RelativeTokenTypesView extends AbstractList<IElementType> {
        private int myStart;
        private int mySize;

        private void configure(int start, int end) {
            myStart = start;
            mySize = end - start;
        }

        @Override
        public IElementType get(int index) {
            return myLexTypes[myStart + index];
        }

        @Override
        public int size() {
            return mySize;
        }
    }

    private final class RelativeTokenTextView implements WhitespacesAndCommentsBinder.TokenTextGetter {
        private int myStart;

        private void configure(int start) {
            myStart = start;
        }

        @Override
        @Nonnull
        public CharSequence get(int i) {
            return myText.subSequence(myLexStarts[myStart + i], myLexStarts[myStart + i + 1]);
        }
    }

    private void checkTreeDepth(final int maxDepth, final boolean isFileRoot) {
        if (myFile == null) {
            return;
        }
        final PsiFile file = myFile.getOriginalFile();
        final Boolean flag = file.getUserData(BlockSupport.TREE_DEPTH_LIMIT_EXCEEDED);
        if (maxDepth > BlockSupport.INCREMENTAL_REPARSE_DEPTH_LIMIT) {
            if (!Boolean.TRUE.equals(flag)) {
                file.putUserData(BlockSupport.TREE_DEPTH_LIMIT_EXCEEDED, Boolean.TRUE);
            }
        }
        else if (isFileRoot && flag != null) {
            file.putUserData(BlockSupport.TREE_DEPTH_LIMIT_EXCEEDED, null);
        }
    }

    private void bind(@Nonnull StartMarker rootMarker, @Nonnull CompositeElement rootNode) {
        StartMarker curMarker = rootMarker;
        CompositeElement curNode = rootNode;

        int lexIndex = rootMarker.myLexemeIndex;
        ProductionMarker item = rootMarker.myFirstChild != null ? rootMarker.myFirstChild : rootMarker.myDoneMarker;
        while (true) {
            lexIndex = insertLeaves(lexIndex, item.myLexemeIndex, curNode);

            if (item == rootMarker.myDoneMarker) {
                break;
            }

            if (item instanceof StartMarker marker) {
                if (!marker.myDoneMarker.myCollapse) {
                    curMarker = marker;

                    final CompositeElement childNode = createComposite(marker);
                    curNode.rawAddChildrenWithoutNotifications(childNode);
                    curNode = childNode;

                    item = marker.myFirstChild != null ? marker.myFirstChild : marker.myDoneMarker;
                    continue;
                }
                else {
                    lexIndex = collapseLeaves(curNode, marker);
                }
            }
            else if (item instanceof ErrorItem errorItem) {
                final CompositeElement errorElement = Factory.createErrorElement(errorItem.myMessage);
                curNode.rawAddChildrenWithoutNotifications(errorElement);
            }
            else if (item instanceof DoneMarker doneMarker) {
                curMarker = (StartMarker)doneMarker.myStart.myParent;
                curNode = curNode.getTreeParent();
                item = doneMarker.myStart;
            }

            item = item.myNext != null ? item.myNext : curMarker.myDoneMarker;
        }
    }

    private int insertLeaves(int curToken, int lastIdx, final CompositeElement curNode) {
        lastIdx = Math.min(lastIdx, myLexemeCount);
        while (curToken < lastIdx) {
            ProgressIndicatorProvider.checkCanceled();
            final int start = myLexStarts[curToken];
            final int end = myLexStarts[curToken + 1];
            if (start < end || myLexTypes[curToken] instanceof ILeafElementType) {
                // Empty token. Most probably a parser directive like indent/dedent in Python
                final IElementType type = myLexTypes[curToken];
                final TreeElement leaf = createLeaf(type, start, end);
                curNode.rawAddChildrenWithoutNotifications(leaf);
            }
            curToken++;
        }

        return curToken;
    }

    private int collapseLeaves(@Nonnull CompositeElement ast, @Nonnull StartMarker startMarker) {
        final int start = myLexStarts[startMarker.myLexemeIndex];
        final int end = myLexStarts[startMarker.myDoneMarker.myLexemeIndex];
        final IElementType markerType = startMarker.myType;
        final TreeElement leaf = createLeaf(markerType, start, end);
        if (markerType instanceof ILazyParseableElementType lazyParseableElementType && lazyParseableElementType.reuseCollapsedTokens()
            && startMarker.myLexemeIndex < startMarker.myDoneMarker.myLexemeIndex) {
            final int length = startMarker.myDoneMarker.myLexemeIndex - startMarker.myLexemeIndex;
            final int[] relativeStarts = new int[length + 1];
            final IElementType[] types = new IElementType[length];
            for (int i = startMarker.myLexemeIndex; i < startMarker.myDoneMarker.myLexemeIndex; i++) {
                relativeStarts[i - startMarker.myLexemeIndex] = myLexStarts[i] - start;
                types[i - startMarker.myLexemeIndex] = myLexTypes[i];
            }
            relativeStarts[length] = end - start;
            leaf.putUserData(LAZY_PARSEABLE_TOKENS, new LazyParseableTokensCache(relativeStarts, types));
        }
        ast.rawAddChildrenWithoutNotifications(leaf);
        return startMarker.myDoneMarker.myLexemeIndex;
    }

    @Nonnull
    private static CompositeElement createComposite(@Nonnull StartMarker marker) {
        final IElementType type = marker.myType;
        if (type == TokenType.ERROR_ELEMENT) {
            LocalizeValue message = marker.myDoneMarker instanceof DoneWithErrorMarker doneErrorMarker ? doneErrorMarker.myMessage : null;
            return Factory.createErrorElement(message == null ? LocalizeValue.empty() : message);
        }

        if (type == null) {
            throw new RuntimeException(UNBALANCED_MESSAGE);
        }

        return ASTFactory.composite(type);
    }

    @Override
    @Nonnull
    public LocalizeValue getErrorMessage(@Nonnull LighterASTNode node) {
        return getErrorMessageImpl(node);
    }

    @Override
    public void setReparseMergeCustomComparator(@Nonnull ReparseMergeCustomComparator reparseMergeCustomComparator) {
        myReparseMergeCustomComparator = reparseMergeCustomComparator;
    }

    @Nonnull
    public static LocalizeValue getErrorMessageImpl(@Nonnull LighterASTNode node) {
        if (node instanceof ErrorItem errorItem) {
            return errorItem.myMessage;
        }
        if (node instanceof StartMarker marker
            && marker.myType == TokenType.ERROR_ELEMENT
            && marker.myDoneMarker instanceof DoneWithErrorMarker doneMarker) {
            return doneMarker.myMessage;
        }

        return LocalizeValue.empty();
    }

    @Override
    public void registerWhitespaceToken(@Nonnull IElementType type) {
        myAnyLanguageWhitespaceTokens = TokenSet.orSet(myAnyLanguageWhitespaceTokens, TokenSet.create(type));
    }

    private static class MyComparator implements ShallowNodeComparator<ASTNode, LighterASTNode> {
        private final ReparseMergeCustomComparator myComparator;
        private final MyTreeStructure myTreeStructure;
        private final TokenSet myAnyLanguageWhitespaceTokens;

        private MyComparator(
            TokenSet anyLanguageWhitespaceTokens,
            ReparseMergeCustomComparator comparator,
            @Nonnull MyTreeStructure treeStructure
        ) {
            myAnyLanguageWhitespaceTokens = anyLanguageWhitespaceTokens;
            myComparator = comparator;
            myTreeStructure = treeStructure;
        }

        @Nonnull
        @Override
        public ThreeState deepEqual(@Nonnull final ASTNode oldNode, @Nonnull final LighterASTNode newNode) {
            ProgressIndicatorProvider.checkCanceled();

            boolean oldIsErrorElement = oldNode instanceof PsiErrorElement;
            boolean newIsErrorElement = newNode.getTokenType() == TokenType.ERROR_ELEMENT;
            if (oldIsErrorElement != newIsErrorElement) {
                return ThreeState.NO;
            }
            if (oldIsErrorElement) {
                final PsiErrorElement e1 = (PsiErrorElement)oldNode;
                return Objects.equals(e1.getErrorDescriptionValue(), getErrorMessageImpl(newNode)) ? ThreeState.UNSURE : ThreeState.NO;
            }

            if (myComparator != null) {
                ThreeState customResult = myComparator.compare(oldNode, newNode, myTreeStructure);

                if (customResult != ThreeState.UNSURE) {
                    return customResult;
                }
            }
            if (newNode instanceof Token token) {
                final IElementType type = newNode.getTokenType();

                if (oldNode instanceof ForeignLeafPsiElement) {
                    return type instanceof ForeignLeafType foreignLeafType
                        && StringUtil.equals(foreignLeafType.getValue(), oldNode.getText())
                        ? ThreeState.YES
                        : ThreeState.NO;
                }

                if (oldNode instanceof LeafElement oldNodeLeaf) {
                    return type instanceof ForeignLeafType
                        ? ThreeState.NO
                        : oldNodeLeaf.textMatches(token.getText())
                        ? ThreeState.YES
                        : ThreeState.NO;
                }

                if (type instanceof ILightLazyParseableElementType) {
                    return ((TreeElement)oldNode).textMatches(token.getText())
                        ? ThreeState.YES
                        : TreeUtil.isCollapsedChameleon(oldNode)
                        ? ThreeState.NO  // do not dive into collapsed nodes
                        : ThreeState.UNSURE;
                }

                if (oldNode.getElementType() instanceof ILazyParseableElementType && type instanceof ILazyParseableElementType ||
                    oldNode.getElementType() instanceof ICustomParsingType && type instanceof ICustomParsingType) {
                    return ((TreeElement)oldNode).textMatches(token.getText()) ? ThreeState.YES : ThreeState.NO;
                }
            }

            return ThreeState.UNSURE;
        }

        @Override
        public boolean typesEqual(@Nonnull final ASTNode n1, @Nonnull final LighterASTNode n2) {
            if (n1 instanceof PsiWhiteSpaceImpl) {
                return myAnyLanguageWhitespaceTokens.contains(n2.getTokenType())
                    || n2 instanceof Token n2Token && n2Token.myBuilder.myWhitespaces.contains(n2.getTokenType());
            }
            IElementType n1t;
            IElementType n2t;
            if (n1 instanceof ForeignLeafPsiElement n1ForeignLeaf) {
                n1t = n1ForeignLeaf.getForeignType();
                n2t = n2.getTokenType();
            }
            else {
                n1t = dereferenceToken(n1.getElementType());
                n2t = dereferenceToken(n2.getTokenType());
            }

            return Comparing.equal(n1t, n2t);
        }

        private static IElementType dereferenceToken(IElementType probablyWrapper) {
            return probablyWrapper instanceof TokenWrapper tokenWrapper
                ? dereferenceToken(tokenWrapper.getDelegate())
                : probablyWrapper;
        }

        @Override
        public boolean hashCodesEqual(@Nonnull final ASTNode n1, @Nonnull final LighterASTNode n2) {
            if (n1 instanceof LeafElement n1Leaf && n2 instanceof Token n2Token) {
                boolean isForeign1 = n1 instanceof ForeignLeafPsiElement;
                boolean isForeign2 = n2.getTokenType() instanceof ForeignLeafType;
                if (isForeign1 != isForeign2) {
                    return false;
                }

                if (isForeign1) {
                    return StringUtil.equals(n1.getText(), ((ForeignLeafType)n2.getTokenType()).getValue());
                }

                return n1Leaf.textMatches(n2Token.getText());
            }

            if (n1 instanceof PsiErrorElement e1 && n2.getTokenType() == TokenType.ERROR_ELEMENT
                && !Objects.equals(e1.getErrorDescriptionValue(), getErrorMessageImpl(n2))) {
                return false;
            }

            return ((TreeElement)n1).hc() == ((Node)n2).hc();
        }
    }

    private static class MyTreeStructure implements FlyweightCapableTreeStructure<LighterASTNode> {
        private final LimitedPool<Token> myPool;
        private final LimitedPool<LazyParseableToken> myLazyPool;
        private final StartMarker myRoot;

        public MyTreeStructure(@Nonnull StartMarker root, @Nullable final MyTreeStructure parentTree) {
            if (parentTree == null) {
                myPool = new LimitedPool<>(1000, new LimitedPool.ObjectFactory<Token>() {
                    @Override
                    public void cleanup(@Nonnull final Token token) {
                        token.clean();
                    }

                    @Nonnull
                    @Override
                    public Token create() {
                        return new TokenNode();
                    }
                });
                myLazyPool = new LimitedPool<>(200, new LimitedPool.ObjectFactory<LazyParseableToken>() {
                    @Override
                    public void cleanup(@Nonnull final LazyParseableToken token) {
                        token.clean();
                    }

                    @Nonnull
                    @Override
                    public LazyParseableToken create() {
                        return new LazyParseableToken();
                    }
                });
            }
            else {
                myPool = parentTree.myPool;
                myLazyPool = parentTree.myLazyPool;
            }
            myRoot = root;
        }

        @Override
        @Nonnull
        public LighterASTNode getRoot() {
            return myRoot;
        }

        @Override
        public LighterASTNode getParent(@Nonnull final LighterASTNode node) {
            if (node instanceof ProductionMarker productionMarker) {
                return productionMarker.myParent;
            }
            if (node instanceof Token token) {
                return token.myParentNode;
            }
            throw new UnsupportedOperationException("Unknown node type: " + node);
        }

        @Override
        @Nonnull
        public LighterASTNode prepareForGetChildren(@Nonnull final LighterASTNode node) {
            return node;
        }

        private int count;
        private LighterASTNode[] nodes;

        @Override
        public int getChildren(@Nonnull final LighterASTNode item, @Nonnull final SimpleReference<LighterASTNode[]> into) {
            if (item instanceof LazyParseableToken lazyParseableToken) {
                final FlyweightCapableTreeStructure<LighterASTNode> tree = lazyParseableToken.parseContents();
                final LighterASTNode root = tree.getRoot();
                if (root instanceof ProductionMarker rootProductionMarker) {
                    rootProductionMarker.myParent = ((Token)item).myParentNode;
                }
                return tree.getChildren(tree.prepareForGetChildren(root), into);  // todo: set offset shift for kids?
            }

            if (item instanceof Token || item instanceof ErrorItem) {
                return 0;
            }
            StartMarker marker = (StartMarker)item;

            count = 0;
            ProductionMarker child = marker.myFirstChild;
            int lexIndex = marker.myLexemeIndex;
            while (child != null) {
                lexIndex = insertLeaves(lexIndex, child.myLexemeIndex, marker.myBuilder, marker);

                if (child instanceof StartMarker childStartMarker && childStartMarker.myDoneMarker.myCollapse) {
                    int lastIndex = childStartMarker.myDoneMarker.myLexemeIndex;
                    insertLeaf(child.getTokenType(), marker.myBuilder, child.myLexemeIndex, lastIndex, true, marker);
                }
                else {
                    ensureCapacity();
                    nodes[count++] = child;
                }

                if (child instanceof StartMarker childStartMarker) {
                    lexIndex = childStartMarker.myDoneMarker.myLexemeIndex;
                }
                child = child.myNext;
            }

            insertLeaves(lexIndex, marker.myDoneMarker.myLexemeIndex, marker.myBuilder, marker);
            into.set(nodes == null ? LighterASTNode.EMPTY_ARRAY : nodes);
            nodes = null;

            return count;
        }

        @Override
        public void disposeChildren(final LighterASTNode[] nodes, final int count) {
            if (nodes == null) {
                return;
            }
            for (int i = 0; i < count; i++) {
                final LighterASTNode node = nodes[i];
                if (node instanceof LazyParseableToken lazyParseableToken) {
                    myLazyPool.recycle(lazyParseableToken);
                }
                else if (node instanceof Token token) {
                    myPool.recycle(token);
                }
            }
        }

        private void ensureCapacity() {
            LighterASTNode[] old = nodes;
            if (old == null) {
                old = new LighterASTNode[10];
                nodes = old;
            }
            else if (count >= old.length) {
                LighterASTNode[] newStore = new LighterASTNode[count * 3 / 2];
                System.arraycopy(old, 0, newStore, 0, count);
                nodes = newStore;
            }
        }

        private int insertLeaves(int curToken, int lastIdx, PsiBuilderImpl builder, StartMarker parent) {
            lastIdx = Math.min(lastIdx, builder.myLexemeCount);
            while (curToken < lastIdx) {
                insertLeaf(builder.myLexTypes[curToken], builder, curToken, curToken + 1, false, parent);

                curToken++;
            }
            return curToken;
        }

        private void insertLeaf(
            @Nonnull IElementType type,
            @Nonnull PsiBuilderImpl builder,
            int startLexemeIndex,
            int endLexemeIndex,
            boolean forceInsertion,
            StartMarker parent
        ) {
            final int start = builder.myLexStarts[startLexemeIndex];
            final int end = builder.myLexStarts[endLexemeIndex];
            /* Corresponding code for heavy tree is located in {@link consulo.ide.impl.idea.lang.impl.PsiBuilderImpl#insertLeaves}
               and is applied only to plain lexemes */
            if (start > end || !forceInsertion && start == end && !(type instanceof ILeafElementType)) {
                return;
            }

            Token lexeme = obtainToken(type, builder, startLexemeIndex, endLexemeIndex, parent, start, end);
            ensureCapacity();
            nodes[count++] = lexeme;
        }

        @Nonnull
        private Token obtainToken(
            @Nonnull IElementType type,
            @Nonnull PsiBuilderImpl builder,
            int startLexemeIndex,
            int endLexemeIndex,
            StartMarker parent,
            int start,
            int end
        ) {
            if (type instanceof ILightLazyParseableElementType) {
                return obtainLazyToken(type, builder, startLexemeIndex, endLexemeIndex, parent, start, end);
            }

            Token lexeme = myPool.alloc();
            lexeme.initToken(type, builder, parent, start, end);
            return lexeme;
        }

        @Nonnull
        private Token obtainLazyToken(
            @Nonnull IElementType type,
            @Nonnull PsiBuilderImpl builder,
            int startLexemeIndex,
            int endLexemeIndex,
            StartMarker parent,
            int start,
            int end
        ) {
            int startInFile = start + builder.myOffset;
            LazyParseableToken token = builder.myChameleonCache.get(startInFile);
            if (token == null) {
                token = myLazyPool.alloc();
                token.myStartIndex = startLexemeIndex;
                token.myEndIndex = endLexemeIndex;
                token.initToken(type, builder, parent, start, end);
                builder.myChameleonCache.put(startInFile, token);
            }
            else if (token.myBuilder != builder || token.myStartIndex != startLexemeIndex || token.myEndIndex != endLexemeIndex) {
                throw new AssertionError("Wrong chameleon cached");
            }
            token.myParentStructure = this;
            return token;
        }

        @Nonnull
        @Override
        public CharSequence toString(@Nonnull LighterASTNode node) {
            return myRoot.myBuilder.myText.subSequence(node.getStartOffset(), node.getEndOffset());
        }

        @Override
        public int getStartOffset(@Nonnull LighterASTNode node) {
            return node.getStartOffset();
        }

        @Override
        public int getEndOffset(@Nonnull LighterASTNode node) {
            return node.getEndOffset();
        }
    }

    private static class ASTConverter implements Function<Node, ASTNode> {
        @Nonnull
        private final StartMarker myRoot;

        private ASTConverter(@Nonnull StartMarker root) {
            myRoot = root;
        }

        @Override
        public ASTNode apply(final Node n) {
            if (n instanceof Token token) {
                return token.myBuilder.createLeaf(token.getTokenType(), token.myTokenStart, token.myTokenEnd);
            }
            else if (n instanceof ErrorItem errorItem) {
                return Factory.createErrorElement(errorItem.myMessage);
            }
            else {
                final StartMarker startMarker = (StartMarker)n;
                final CompositeElement composite = n == myRoot
                    ? (CompositeElement)myRoot.myBuilder.createRootAST(myRoot)
                    : createComposite(startMarker);
                startMarker.myBuilder.bind(startMarker, composite);
                return composite;
            }
        }
    }

    @Override
    public void setDebugMode(boolean dbgMode) {
        myDebugMode = dbgMode;
    }

    @Nonnull
    public Lexer getLexer() {
        return myLexer;
    }

    @Nonnull
    protected TreeElement createLeaf(@Nonnull IElementType type, final int start, final int end) {
        CharSequence text = myCharTable.intern(myText, start, end);
        if (myWhitespaces.contains(type)) {
            return new PsiWhiteSpaceImpl(text);
        }

        if (type instanceof ICustomParsingType customParsingType) {
            return (TreeElement)customParsingType.parse(text, myCharTable);
        }

        if (type instanceof ILazyParseableElementType lazyParseableElementType) {
            return ASTFactory.lazy(lazyParseableElementType, text);
        }

        return ASTFactory.leaf(type, myLanguageVersion, text);
    }

    @Nullable
    @Override
    public PsiFile getContainingFile() {
        return myFile;
    }

    @Override
    public void setContainingFile(@Nonnull PsiFile containingFile) {
        myFile = containingFile;
    }

    private static class MyList extends ArrayList<ProductionMarker> {
        // make removeRange method available.
        @Override
        protected void removeRange(final int fromIndex, final int toIndex) {
            super.removeRange(fromIndex, toIndex);
        }

        private MyList() {
            super(256);
        }
    }

    private static class LazyParseableTokensCache {
        final int[] myLexStarts;
        final IElementType[] myLexTypes;

        LazyParseableTokensCache(int[] lexStarts, IElementType[] lexTypes) {
            myLexStarts = lexStarts;
            myLexTypes = lexTypes;
        }
    }
}
