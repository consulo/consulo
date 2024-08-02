/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import consulo.annotation.DeprecationInfo;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.LighterASTNode;
import consulo.language.ast.TokenSet;
import consulo.language.lexer.Lexer;
import consulo.language.psi.PsiFile;
import consulo.language.util.FlyweightCapableTreeStructure;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.util.dataholder.UserDataHolder;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * The IDEA side of a custom language parser. Provides lexical analysis results to the
 * plugin and allows the plugin to build the AST tree.
 *
 * @see PsiParser
 * @see ASTNode
 */
public interface PsiBuilder extends UserDataHolder {
    /**
     * Returns a project for which PSI builder was created (see {@link PsiBuilderFactory}).
     *
     * @return project.
     */
    @Nullable
    Project getProject();

    /**
     * Returns the complete text being parsed.
     *
     * @return the text being parsed
     */
    @Nonnull
    CharSequence getOriginalText();

    /**
     * Advances the lexer to the next token, skipping whitespace and comment tokens.
     */
    void advanceLexer();

    /**
     * Returns the type of current token from the lexer.
     *
     * @return the token type, or null when the token stream is over.
     * @see #setTokenTypeRemapper(ITokenTypeRemapper).
     */
    @Nullable
    IElementType getTokenType();

    /**
     * Sets optional remapper that can change the type of tokens.
     * Output of getTokenType() is affected by it.
     *
     * @param remapper the remapper object, or null.
     */
    void setTokenTypeRemapper(@Nullable ITokenTypeRemapper remapper);

    /**
     * Slightly easier way to what {@link ITokenTypeRemapper} does (i.e. it just remaps current token to a given type).
     *
     * @param type new type for the current token.
     */
    void remapCurrentToken(IElementType type);

    /**
     * Subscribe for notification on default whitespace and comments skipped events.
     *
     * @param callback an implementation for the callback
     */
    void setWhitespaceSkippedCallback(@Nullable WhitespaceSkippedCallback callback);

    /**
     * @return true of elementType registered as comment/whitespace in parser definition
     */
    boolean isWhitespaceOrCommentType(@Nonnull IElementType elementType);

    /**
     * See what token type is in <code>steps</code> ahead
     *
     * @param steps 0 is current token (i.e. the same {@link PsiBuilder#getTokenType()} returns)
     * @return type element which getTokenType() will return if we call advance <code>steps</code> times in a row
     */
    @Nullable
    IElementType lookAhead(int steps);

    /**
     * See what token type is in <code>steps</code> ahead / behind
     *
     * @param steps 0 is current token (i.e. the same {@link PsiBuilder#getTokenType()} returns)
     * @return type element ahead or behind, including whitespace / comment tokens
     */
    @Nullable
    IElementType rawLookup(int steps);

    /**
     * See what token type is in <code>steps</code> ahead / behind current position
     *
     * @param steps 0 is current token (i.e. the same {@link PsiBuilder#getTokenType()} returns)
     * @return offset type element ahead or behind, including whitespace / comment tokens, -1 if first token,
     * getOriginalText().getLength() at end
     */
    int rawTokenTypeStart(int steps);

    /**
     * Returns the index of the current token in the original sequence.
     *
     * @return token index
     */
    int rawTokenIndex();

    /**
     * Returns the text of the current token from the lexer.
     *
     * @return the token text, or null when the token stream is over.
     */
    @Nullable
    String getTokenText();

    /**
     * Returns the char sequencr of the current token from the lexer.
     *
     * @return the token char sequencr, or null when the token stream is over.
     */
    @Nullable
    default CharSequence getTokenSequence() {
        return getTokenText();
    }

    /**
     * Returns the start offset of the current token, or the file length when the token stream is over.
     *
     * @return the token offset.
     */
    int getCurrentOffset();

    /**
     * A marker defines a range in the document text which becomes a node in the AST
     * tree. The ranges defined by markers within the text range of the current marker
     * become child nodes of the node defined by the current marker.
     */
    interface Marker {
        /**
         * Creates and returns a new marker starting immediately before the start of
         * this marker and extending after its end. Can be called on a completed or
         * a currently active marker.
         *
         * @return the new marker instance.
         */
        Marker precede();

        /**
         * Drops this marker. Can be called after other markers have been added and completed
         * after this marker. Does not affect lexer position or markers added after this marker.
         */
        void drop();

        /**
         * Drops this marker and all markers added after it, and reverts the lexer position to the
         * position of this marker.
         */
        void rollbackTo();

        /**
         * Completes this marker and labels it with the specified AST node type. Before calling this method,
         * all markers added after the beginning of this marker must be either dropped or completed.
         *
         * @param type the type of the node in the AST tree.
         */
        void done(IElementType type);

        /**
         * Like {@linkplain #done(IElementType)}, but collapses all tokens between start and end markers
         * into single leaf node of given type.
         *
         * @param type the type of the node in the AST tree.
         */
        void collapse(IElementType type);

        /**
         * Like {@linkplain #done(IElementType)}, but the marker is completed (end marker inserted)
         * before specified one. All markers added between start of this marker and the marker specified as end one
         * must be either dropped or completed.
         *
         * @param type   the type of the node in the AST tree.
         * @param before marker to complete this one before.
         */
        void doneBefore(IElementType type, Marker before);

        /**
         * Like {@linkplain #doneBefore(IElementType, Marker)}, but in addition an error element with given text
         * is inserted right before this marker's end.
         *
         * @param type         the type of the node in the AST tree.
         * @param before       marker to complete this one before.
         * @param errorMessage for error element.
         */
        @Deprecated
        @DeprecationInfo("Use #doneBefore(IElementType, Marker, @Nonnull LocalizeValue)")
        default void doneBefore(IElementType type, Marker before, String errorMessage) {
            doneBefore(type, before, LocalizeValue.of(errorMessage));
        }

        /**
         * Like {@linkplain #doneBefore(IElementType, Marker)}, but in addition an error element with given text
         * is inserted right before this marker's end.
         *
         * @param type         the type of the node in the AST tree.
         * @param before       marker to complete this one before.
         * @param errorMessage for error element.
         */
        void doneBefore(IElementType type, Marker before, @Nonnull LocalizeValue errorMessage);

        /**
         * Completes this marker and labels it as error element with specified message. Before calling this method,
         * all markers added after the beginning of this marker must be either dropped or completed.
         *
         * @param message for error element.
         */
        @Deprecated
        @DeprecationInfo("Use #error(LocalizeValue)")
        default void error(String message) {
            error(LocalizeValue.of(message));
        }

        /**
         * Completes this marker and labels it as error element with specified message. Before calling this method,
         * all markers added after the beginning of this marker must be either dropped or completed.
         *
         * @param message for error element.
         */
        void error(@Nonnull LocalizeValue message);

        /**
         * Like {@linkplain #error(String)}, but the marker is completed before specified one.
         *
         * @param message for error element.
         * @param before  marker to complete this one before.
         */
        @Deprecated
        @DeprecationInfo("Use #errorBefore(LocalizeValue, Marker)")
        default void errorBefore(String message, Marker before) {
            errorBefore(LocalizeValue.of(message), before);
        }

        /**
         * Like {@linkplain #error(String)}, but the marker is completed before specified one.
         *
         * @param message for error element.
         * @param before  marker to complete this one before.
         */
        void errorBefore(@Nonnull LocalizeValue message, Marker before);

        /**
         * Allows to define custom edge token binders instead of default ones. If any of parameters is null
         * then corresponding token binder won't be changed (keeping previously set or default token binder).
         * It is an error to set right token binder for not-done marker.
         *
         * @param left  new left edge token binder.
         * @param right new right edge token binder.
         */
        void setCustomEdgeTokenBinders(@Nullable WhitespacesAndCommentsBinder left, @Nullable WhitespacesAndCommentsBinder right);
    }

    /**
     * Creates a marker at the current parsing position.
     *
     * @return the new marker instance.
     */
    Marker mark();

    /**
     * Adds an error marker with the specified message text at the current position in the tree.
     * <br><b>Note</b>: from series of subsequent errors messages only first will be part of resulting tree.
     *
     * @param messageText the text of the error message displayed to the user.
     */
    void error(@Nonnull LocalizeValue messageText);

    /**
     * Adds an error marker with the specified message text at the current position in the tree.
     * <br><b>Note</b>: from series of subsequent errors messages only first will be part of resulting tree.
     *
     * @param messageText the text of the error message displayed to the user.
     */
    @Deprecated
    @DeprecationInfo("Use #error(LocalizeValue")
    default void error(@Nonnull String messageText) {
        error(LocalizeValue.of(messageText));
    }

    /**
     * Checks if the lexer has reached the end of file.
     *
     * @return true if the lexer is at end of file, false otherwise.
     */
    boolean eof();

    /**
     * Returns the result of the parsing. All markers must be completed or dropped before this method is called.
     *
     * @return the built tree.
     */
    @Nonnull
    ASTNode getTreeBuilt();

    /**
     * Same as {@link #getTreeBuilt()} but returns a light tree, which is build faster,
     * produces less garbage but is incapable of creating a PSI over.
     * <br><b>Note</b>: this method shouldn't be called if {@link #getTreeBuilt()} was called before.
     *
     * @return the light tree built.
     */
    @Nonnull
    FlyweightCapableTreeStructure<LighterASTNode> getLightTree();

    /**
     * Enables or disables the builder debug mode. In debug mode, the builder will print stack trace
     * to marker allocation position if one is not done when calling getTreeBuilt().
     *
     * @param dbgMode the debug mode value.
     */
    void setDebugMode(boolean dbgMode);

    void enforceCommentTokens(TokenSet tokens);

    /**
     * @return latest left done node for context dependent parsing.
     */
    @Nullable
    LighterASTNode getLatestDoneMarker();

    /**
     * Return localize value for node, if node is error. If it's not error empty value {@link LocalizeValue#empty()}
     */
    @Nonnull
    LocalizeValue getErrorMessage(@Nonnull LighterASTNode node);

    void setReparseMergeCustomComparator(@Nonnull ReparseMergeCustomComparator comparator);

    void setContainingFile(@Nonnull PsiFile containingFile);

    @Nullable
    PsiFile getContainingFile();

    @Nonnull
    Lexer getLexer();

    void registerWhitespaceToken(@Nonnull IElementType type);
}
