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

import consulo.language.ast.IElementType;

import jakarta.annotation.Nullable;

/**
 * @author max
 */
public class DelegateLexer extends LexerBase {
    protected final Lexer myDelegate;

    public DelegateLexer(Lexer delegate) {
        myDelegate = delegate;
    }

    public final Lexer getDelegate() {
        return myDelegate;
    }

    @Override
    public void start(CharSequence buffer, int startOffset, int endOffset, int initialState) {
        myDelegate.start(buffer, startOffset, endOffset, initialState);
    }

    @Override
    public int getState() {
        return myDelegate.getState();
    }

    @Override
    @Nullable
    public IElementType getTokenType() {
        return myDelegate.getTokenType();
    }

    @Override
    public int getTokenStart() {
        return myDelegate.getTokenStart();
    }

    @Override
    public int getTokenEnd() {
        return myDelegate.getTokenEnd();
    }

    @Override
    public void advance() {
        myDelegate.advance();
    }

    @Override
    public final CharSequence getBufferSequence() {
        return myDelegate.getBufferSequence();
    }

    @Override
    public int getBufferEnd() {
        return myDelegate.getBufferEnd();
    }
}
