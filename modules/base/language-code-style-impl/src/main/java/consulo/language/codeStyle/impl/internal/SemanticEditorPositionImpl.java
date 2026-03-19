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
package consulo.language.codeStyle.impl.internal;

import consulo.codeEditor.EditorEx;
import consulo.codeEditor.HighlighterIterator;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.lineIndent.SemanticEditorPosition;
import consulo.language.editor.highlight.HighlighterIteratorWrapper;
import consulo.util.lang.CharArrayUtil;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.lang.Integer.min;

/**
 * @author Rustam Vishnyakov
 */
public class SemanticEditorPositionImpl implements SemanticEditorPosition {
    private final EditorEx myEditor;
    private final HighlighterIterator myIterator;
    private final CharSequence myChars;
    private final Function<IElementType, SyntaxElement> myTypeMapper;
    private final BiFunction<EditorEx, Integer, HighlighterIterator> myCreateHighlighterIteratorAtOffset;

    private SemanticEditorPositionImpl(EditorEx editor,
                                       int offset,
                                       BiFunction<EditorEx, Integer, HighlighterIterator> createHighlighterIteratorAtOffset,
                                       Function<IElementType, SyntaxElement> typeMapper) {
        myCreateHighlighterIteratorAtOffset = createHighlighterIteratorAtOffset;
        myEditor = editor;
        myChars = myEditor.getDocument().getCharsSequence();
        myIterator = createHighlighterIteratorAtOffset.apply(editor, offset);
        myTypeMapper = typeMapper;
    }

    @Override
    public void moveBeforeOptional(SyntaxElement syntaxElement) {
        if (!myIterator.atEnd()) {
            if (syntaxElement.equals(map((IElementType) myIterator.getTokenType()))) {
                myIterator.retreat();
            }
        }
    }

    @Override
    public SemanticEditorPositionImpl beforeOptional(SyntaxElement syntaxElement) {
        return copyAnd(position -> position.moveBeforeOptional(syntaxElement));
    }

    @Override
    public void moveBeforeOptionalMix(SyntaxElement... elements) {
        while (isAtAnyOf(elements)) {
            myIterator.retreat();
        }
    }

    @Override
    public SemanticEditorPositionImpl beforeOptionalMix(SyntaxElement... elements) {
        return copyAnd(position -> position.moveBeforeOptionalMix(elements));
    }

    @Override
    public void moveAfterOptionalMix(SyntaxElement... elements) {
        while (isAtAnyOf(elements)) {
            myIterator.advance();
        }
    }

    @Override
    public SemanticEditorPositionImpl afterOptionalMix(SyntaxElement... elements) {
        return copyAnd(position -> position.moveAfterOptionalMix(elements));
    }

    @Override
    public boolean isAtMultiline() {
        return !myIterator.atEnd() && CharArrayUtil.containLineBreaks(myChars, myIterator.getStart(), myIterator.getEnd());
    }

    /**
     * Checks if there are line breaks strictly after the given offset till the end of the current element.
     *
     * @param offset The offset to search line breaks after.
     * @return True if there are line breaks after the given offset.
     */
    @Override
    public boolean hasLineBreaksAfter(int offset) {
        if (!myIterator.atEnd() && offset >= 0) {
            int offsetAfter = offset + 1;
            if (offsetAfter < myIterator.getEnd()) {
                return CharArrayUtil.containLineBreaks(myChars, offsetAfter, myIterator.getEnd());
            }
        }
        return false;
    }

    @Override
    public boolean isAtMultiline(SyntaxElement... elements) {
        return isAtAnyOf(elements) && CharArrayUtil.containLineBreaks(myChars, myIterator.getStart(), myIterator.getEnd());
    }

    @Override
    public void moveBefore() {
        if (!myIterator.atEnd()) {
            myIterator.retreat();
        }
    }

    @Override
    public SemanticEditorPositionImpl before() {
        return copyAnd(SemanticEditorPosition::moveBefore);
    }

    @Override
    public void moveAfterOptional(SyntaxElement syntaxElement) {
        if (!myIterator.atEnd()) {
            if (syntaxElement.equals(map((IElementType) myIterator.getTokenType()))) {
                myIterator.advance();
            }
        }
    }

    @Override
    public SemanticEditorPositionImpl afterOptional(SyntaxElement syntaxElement) {
        return copyAnd(position -> position.moveAfterOptional(syntaxElement));
    }

    @Override
    public void moveAfter() {
        if (!myIterator.atEnd()) {
            myIterator.advance();
        }
    }

    @Override
    public SemanticEditorPositionImpl after() {
        return copyAnd(SemanticEditorPosition::moveAfter);
    }

    @Override
    public void moveBeforeParentheses(SyntaxElement leftParenthesis, SyntaxElement rightParenthesis) {
        int parenLevel = 0;
        while (!myIterator.atEnd()) {
            SyntaxElement currElement = map((IElementType) myIterator.getTokenType());
            myIterator.retreat();
            if (rightParenthesis.equals(currElement)) {
                parenLevel++;
            }
            else if (leftParenthesis.equals(currElement)) {
                parenLevel--;
                if (parenLevel < 1) {
                    break;
                }
            }
        }
    }

    @Override
    public SemanticEditorPositionImpl beforeParentheses(SyntaxElement leftParenthesis, SyntaxElement rightParenthesis) {
        return copyAnd(position -> position.moveBeforeParentheses(leftParenthesis, rightParenthesis));
    }

    @Override
    public void moveToLeftParenthesisBackwardsSkippingNested(SyntaxElement leftParenthesis, SyntaxElement rightParenthesis) {
        moveToLeftParenthesisBackwardsSkippingNestedWithPredicate(leftParenthesis, rightParenthesis, any -> false);
    }

    @Override
    public SemanticEditorPositionImpl findLeftParenthesisBackwardsSkippingNested(SyntaxElement leftParenthesis, SyntaxElement rightParenthesis) {
        return copyAnd(position -> position.moveToLeftParenthesisBackwardsSkippingNested(leftParenthesis, rightParenthesis));
    }

    @Override
    public void moveToLeftParenthesisBackwardsSkippingNestedWithPredicate(SyntaxElement leftParenthesis,
                                                                          SyntaxElement rightParenthesis,
                                                                          Predicate<SemanticEditorPosition> terminationCondition) {
        while (!myIterator.atEnd()) {
            if (terminationCondition.test(this)) {
                break;
            }
            if (rightParenthesis.equals(map((IElementType) myIterator.getTokenType()))) {
                moveBeforeParentheses(leftParenthesis, rightParenthesis);
                continue;
            }
            else if (leftParenthesis.equals(map((IElementType) myIterator.getTokenType()))) {
                break;
            }
            myIterator.retreat();
        }
    }

    @Override
    public SemanticEditorPositionImpl findLeftParenthesisBackwardsSkippingNestedWithPredicate(SyntaxElement leftParenthesis,
                                                                                              SyntaxElement rightParenthesis,
                                                                                              Predicate<SemanticEditorPosition> terminationCondition) {
        return copyAnd(position -> position.moveToLeftParenthesisBackwardsSkippingNestedWithPredicate(leftParenthesis, rightParenthesis, terminationCondition));
    }

    @Override
    public boolean isAfterOnSameLine(SyntaxElement... syntaxElements) {
        return elementAfterOnSameLine(syntaxElements) != null;
    }

    @Override
    public @Nullable SyntaxElement elementAfterOnSameLine(SyntaxElement... syntaxElements) {
        myIterator.retreat();
        while (!myIterator.atEnd() && !isAtMultiline()) {
            SyntaxElement currElement = map((IElementType) myIterator.getTokenType());
            for (SyntaxElement element : syntaxElements) {
                if (element.equals(currElement)) {
                    return element;
                }
            }
            myIterator.retreat();
        }
        return null;
    }

    @Override
    public boolean isAt(SyntaxElement syntaxElement) {
        return !myIterator.atEnd() && syntaxElement.equals(map((IElementType) myIterator.getTokenType()));
    }

    @Override
    public boolean isAt(IElementType elementType) {
        return !myIterator.atEnd() && myIterator.getTokenType() == elementType;
    }

    @Override
    public boolean isAtEnd() {
        return myIterator.atEnd();
    }

    @Override
    public int getStartOffset() {
        return myIterator.getStart();
    }

    @Override
    public boolean isAtAnyOf(SyntaxElement... syntaxElements) {
        if (!myIterator.atEnd()) {
            SyntaxElement currElement = map((IElementType) myIterator.getTokenType());
            for (SyntaxElement element : syntaxElements) {
                if (element.equals(currElement)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public CharSequence getChars() {
        return myChars;
    }

    @Override
    public int findStartOf(SyntaxElement element) {
        while (!myIterator.atEnd()) {
            if (element.equals(map((IElementType) myIterator.getTokenType()))) {
                return myIterator.getStart();
            }
            myIterator.retreat();
        }
        return -1;
    }

    @Override
    public boolean hasEmptyLineAfter(int offset) {
        for (int i = offset + 1; i < myIterator.getEnd(); i++) {
            if (myChars.charAt(i) == '\n') {
                return true;
            }
        }
        return false;
    }

    public EditorEx getEditor() {
        return myEditor;
    }

    @Override
    public @Nullable Language getLanguage() {
        return !myIterator.atEnd() ? ((IElementType) myIterator.getTokenType()).getLanguage() : null;
    }

    @Override
    public boolean isAtLanguage(@Nullable Language language) {
        if (language != null && !myIterator.atEnd()) {
            return language == Language.ANY || ((IElementType) myIterator.getTokenType()).getLanguage().is(language);
        }
        return false;
    }

    @Override
    public @Nullable SyntaxElement getCurrElement() {
        return !myIterator.atEnd() ? map((IElementType) myIterator.getTokenType()) : null;
    }

    @Override
    public boolean matchesRule(Predicate<SemanticEditorPosition> rule) {
        return rule.test(this);
    }

    @Override
    public SyntaxElement map(IElementType elementType) {
        return myTypeMapper.apply(elementType);
    }

    @Override
    public String toString() {
        return myIterator.atEnd() ? "atEnd" : myIterator.getTokenType().toString() + "=>" + getChars().subSequence(getStartOffset(), min(getStartOffset() + 255, getChars().length()));
    }

    @Override
    public SemanticEditorPositionImpl copy() {
        return createEditorPosition(myEditor, isAtEnd() ? -1 : myIterator.getStart(), (editor, offset) -> !isAtEnd()
            ? myCreateHighlighterIteratorAtOffset.apply(editor, offset)
            : new HighlighterIteratorWrapper(myIterator) { // A wrapper around current iterator to make it immutable.
            @Override
            public void advance() {
                // do nothing
            }

            @Override
            public void retreat() {
                // do nothing
            }
        }, myTypeMapper);
    }

    @Override
    public SemanticEditorPositionImpl copyAnd(Consumer<SemanticEditorPosition> modifier) {
        SemanticEditorPositionImpl position = copy();
        modifier.accept(position);
        return position;
    }

    
    public static SemanticEditorPositionImpl createEditorPosition(EditorEx editor,
                                                                  int offset,
                                                                  BiFunction<EditorEx, Integer, HighlighterIterator> createHighlighterIteratorAtOffset,
                                                                  Function<IElementType, SyntaxElement> typeMapper) {
        return new SemanticEditorPositionImpl(editor, offset, createHighlighterIteratorAtOffset, typeMapper);
    }
}
