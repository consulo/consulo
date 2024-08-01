/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.parser;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.LighterASTNode;
import consulo.language.ast.TokenSet;
import consulo.language.lexer.Lexer;
import consulo.language.psi.PsiFile;
import consulo.language.util.FlyweightCapableTreeStructure;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class PsiBuilderAdapter implements PsiBuilder {
    protected final PsiBuilder myDelegate;

    public PsiBuilderAdapter(final PsiBuilder delegate) {
        myDelegate = delegate;
    }

    public PsiBuilder getDelegate() {
        return myDelegate;
    }

    @Override
    public Project getProject() {
        return myDelegate.getProject();
    }

    @Nonnull
    @Override
    public CharSequence getOriginalText() {
        return myDelegate.getOriginalText();
    }

    @Override
    public void advanceLexer() {
        myDelegate.advanceLexer();
    }

    @Override
    @Nullable
    public IElementType getTokenType() {
        return myDelegate.getTokenType();
    }

    @Override
    public void setTokenTypeRemapper(final ITokenTypeRemapper remapper) {
        myDelegate.setTokenTypeRemapper(remapper);
    }

    @Override
    public void setWhitespaceSkippedCallback(@Nullable final WhitespaceSkippedCallback callback) {
        myDelegate.setWhitespaceSkippedCallback(callback);
    }

    @Override
    public boolean isWhitespaceOrCommentType(@Nonnull IElementType elementType) {
        return myDelegate.isWhitespaceOrCommentType(elementType);
    }

    @Override
    public void remapCurrentToken(IElementType type) {
        myDelegate.remapCurrentToken(type);
    }

    @Override
    public IElementType lookAhead(int steps) {
        return myDelegate.lookAhead(steps);
    }

    @Override
    public IElementType rawLookup(int steps) {
        return myDelegate.rawLookup(steps);
    }

    @Override
    public int rawTokenTypeStart(int steps) {
        return myDelegate.rawTokenTypeStart(steps);
    }

    @Override
    public int rawTokenIndex() {
        return myDelegate.rawTokenIndex();
    }

    @Override
    @Nullable
    public String getTokenText() {
        return myDelegate.getTokenText();
    }

    @Nullable
    @Override
    public CharSequence getTokenSequence() {
        return myDelegate.getTokenSequence();
    }

    @Override
    public int getCurrentOffset() {
        return myDelegate.getCurrentOffset();
    }

    @Override
    public Marker mark() {
        return myDelegate.mark();
    }

    @Override
    public void error(@Nonnull LocalizeValue messageText) {
        myDelegate.error(messageText);
    }

    @Override
    public boolean eof() {
        return myDelegate.eof();
    }

    @Nonnull
    @Override
    public ASTNode getTreeBuilt() {
        return myDelegate.getTreeBuilt();
    }

    @Nonnull
    @Override
    public FlyweightCapableTreeStructure<LighterASTNode> getLightTree() {
        return myDelegate.getLightTree();
    }

    @Override
    public void setDebugMode(final boolean dbgMode) {
        myDelegate.setDebugMode(dbgMode);
    }

    @Override
    public void enforceCommentTokens(final TokenSet tokens) {
        myDelegate.enforceCommentTokens(tokens);
    }

    @Override
    @Nullable
    public LighterASTNode getLatestDoneMarker() {
        return myDelegate.getLatestDoneMarker();
    }

    @Nonnull
    @Override
    public LocalizeValue getErrorMessage(@Nonnull LighterASTNode node) {
        return myDelegate.getErrorMessage(node);
    }

    @Override
    public void setReparseMergeCustomComparator(@Nonnull ReparseMergeCustomComparator comparator) {
        myDelegate.setReparseMergeCustomComparator(comparator);
    }

    @Override
    @Nullable
    public <T> T getUserData(@Nonnull final Key<T> key) {
        return myDelegate.getUserData(key);
    }

    @Override
    public <T> void putUserData(@Nonnull final Key<T> key, @Nullable final T value) {
        myDelegate.putUserData(key, value);
    }

    @Nullable
    @Override
    public PsiFile getContainingFile() {
        return myDelegate.getContainingFile();
    }

    @Nonnull
    @Override
    public Lexer getLexer() {
        return myDelegate.getLexer();
    }

    @Override
    public void registerWhitespaceToken(@Nonnull IElementType type) {
        myDelegate.registerWhitespaceToken(type);
    }

    @Override
    public void setContainingFile(@Nonnull PsiFile containingFile) {
        myDelegate.setContainingFile(containingFile);
    }
}
