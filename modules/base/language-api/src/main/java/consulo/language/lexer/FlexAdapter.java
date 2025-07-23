// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.lexer;

import consulo.component.ProcessCanceledException;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenType;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;

public class FlexAdapter extends LexerBase {

    private static final Logger LOG = Logger.getInstance(FlexAdapter.class);

    private final FlexLexer myFlex;

    private IElementType myTokenType;
    private CharSequence myText;

    private int myTokenStart;
    private int myTokenEnd;

    private int myBufferEnd;
    private int myState;

    private boolean myFailed;

    public FlexAdapter(@Nonnull FlexLexer flex) {
        myFlex = flex;
    }

    public FlexLexer getFlex() {
        return myFlex;
    }

    @Override
    public void start(@Nonnull CharSequence buffer, int startOffset, int endOffset, int initialState) {
        myText = buffer;
        myTokenStart = myTokenEnd = startOffset;
        myBufferEnd = endOffset;
        myFlex.reset(myText, startOffset, endOffset, initialState);
        myTokenType = null;
    }

    @Override
    public int getState() {
        locateToken();
        return myState;
    }

    @Override
    public IElementType getTokenType() {
        locateToken();
        return myTokenType;
    }

    @Override
    public int getTokenStart() {
        locateToken();
        return myTokenStart;
    }

    @Override
    public int getTokenEnd() {
        locateToken();
        return myTokenEnd;
    }

    @Override
    public void advance() {
        locateToken();
        myTokenType = null;
    }

    @Override
    @Nonnull
    public CharSequence getBufferSequence() {
        return myText;
    }

    @Override
    public int getBufferEnd() {
        return myBufferEnd;
    }

    protected void locateToken() {
        if (myTokenType != null) {
            return;
        }

        myTokenStart = myTokenEnd;
        if (myFailed) {
            return;
        }

        try {
            myState = myFlex.yystate();
            myTokenType = myFlex.advance();
            myTokenEnd = myFlex.getTokenEnd();
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            myFailed = true;
            myTokenType = TokenType.BAD_CHARACTER;
            myTokenEnd = myBufferEnd;
            LOG.warn(myFlex.getClass().getName(), e);
        }
    }

    @Override
    public String toString() {
        return "FlexAdapter for " + myFlex.getClass().getName();
    }
}
