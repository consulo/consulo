/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.lexer;

import consulo.language.Language;
import consulo.language.ast.IElementType;

public class EmptyLexer extends LexerBase {
    private CharSequence myBuffer;
    private int myStartOffset;
    private int myEndOffset;

    private static final IElementType EMPTY_TOKEN_TYPE = new IElementType("empty token", Language.ANY);

    @Override
    public void start(final CharSequence buffer, final int startOffset, final int endOffset, final int initialState) {
        myBuffer = buffer;
        myStartOffset = startOffset;
        myEndOffset = endOffset;
    }

    @Override
    public CharSequence getBufferSequence() {
        return myBuffer;
    }

    @Override
    public int getState() {
        return 0;
    }

    @Override
    public IElementType getTokenType() {
        return (myStartOffset < myEndOffset ? EMPTY_TOKEN_TYPE : null);
    }

    @Override
    public int getTokenStart() {
        return myStartOffset;
    }

    @Override
    public int getTokenEnd() {
        return myEndOffset;
    }

    @Override
    public void advance() {
        myStartOffset = myEndOffset;
    }

    @Override
    public LexerPosition getCurrentPosition() {
        return new LexerPositionImpl(0, getState());
    }

    @Override
    public void restore(LexerPosition position) {
    }

    @Override
    public int getBufferEnd() {
        return myEndOffset;
    }
}
