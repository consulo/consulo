// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.internal;

import consulo.application.progress.ProgressIndicatorProvider;
import consulo.language.ast.IElementType;
import consulo.language.lexer.Lexer;
import consulo.language.lexer.TokenList;
import consulo.logging.Logger;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class TokenSequence implements TokenList {
    private static final Logger LOG = Logger.getInstance(TokenSequence.class);

    @Nonnull
    public static TokenSequence performLexing(@Nonnull CharSequence text, @Nonnull Lexer lexer) {
        if (lexer instanceof WrappingLexer) {
            TokenList existing = ((WrappingLexer) lexer).getTokens();
            if (existing instanceof TokenSequence && Comparing.equal(text, ((TokenSequence) existing).myText)) {
                // prevent clients like PsiBuilder from modifying shared token types
                return new TokenSequence(((TokenSequence) existing).lexStarts,
                    ((TokenSequence) existing).lexTypes.clone(),
                    ((TokenSequence) existing).lexemeCount, text);
            }
        }
        return new Builder(text, lexer).performLexing();
    }

    private final CharSequence myText;
    public final int[] lexStarts;
    public final IElementType[] lexTypes;

    public final int lexemeCount;

    public TokenSequence(int[] lexStarts, IElementType[] lexTypes, int lexemeCount, CharSequence text) {
        this.lexStarts = lexStarts;
        this.lexTypes = lexTypes;
        this.lexemeCount = lexemeCount;
        myText = text;
        assert lexemeCount < lexStarts.length;
        assert lexemeCount < lexTypes.length;
    }

    public void assertMatches(@Nonnull CharSequence text, @Nonnull Lexer lexer) {
        TokenSequence sequence = new Builder(text, lexer).performLexing();
        assert lexemeCount == sequence.lexemeCount;
        for (int j = 0; j <= lexemeCount; ++j) {
            if (sequence.lexStarts[j] != lexStarts[j] || sequence.lexTypes[j] != lexTypes[j]) {
                assert false;
            }
        }
    }

    @Override
    public int getTokenCount() {
        return lexemeCount;
    }

    @Override
    @Nullable
    public IElementType getTokenType(int index) {
        if (index < 0 || index >= getTokenCount()) {
            return null;
        }
        return lexTypes[index];
    }

    @Override
    public int getTokenStart(int index) {
        return lexStarts[index];
    }

    @Override
    public int getTokenEnd(int index) {
        return lexStarts[index + 1];
    }

    @Override
    public @Nonnull CharSequence getTokenizedText() {
        return myText;
    }

    private static class Builder {
        private final CharSequence myText;
        private final Lexer myLexer;
        private int[] myLexStarts;
        private IElementType[] myLexTypes;

        Builder(@Nonnull CharSequence text, @Nonnull Lexer lexer) {
            myText = text;
            myLexer = lexer;

            int approxLexCount = Math.max(10, myText.length() / 5);

            myLexStarts = new int[approxLexCount];
            myLexTypes = new IElementType[approxLexCount];
        }

        @Nonnull
        TokenSequence performLexing() {
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
                    reportDescendingOffsets(i, offset, tokenStart);
                }
                myLexStarts[i] = offset = tokenStart;
                myLexTypes[i] = type;
                i++;
                myLexer.advance();
            }

            myLexStarts[i] = myText.length();

            return new TokenSequence(myLexStarts, myLexTypes, i, myText);
        }

        private void reportDescendingOffsets(int tokenIndex, int offset, int tokenStart) {
            StringBuilder sb = new StringBuilder();
            IElementType tokenType = myLexer.getTokenType();
            sb.append("Token sequence broken")
                .append("\n  this: '").append(myLexer.getTokenText()).append("' (").append(tokenType).append(':')
                .append(tokenType != null ? tokenType.getLanguage() : null).append(") ").append(tokenStart).append(":")
                .append(myLexer.getTokenEnd());
            if (tokenIndex > 0) {
                int prevStart = myLexStarts[tokenIndex - 1];
                sb.append("\n  prev: '").append(myText.subSequence(prevStart, offset)).append("' (").append(myLexTypes[tokenIndex - 1]).append(':')
                    .append(myLexTypes[tokenIndex - 1].getLanguage()).append(") ").append(prevStart).append(":").append(offset);
            }
            int quoteStart = Math.max(tokenStart - 256, 0);
            int quoteEnd = Math.min(tokenStart + 256, myText.length());
            sb.append("\n  quote: [").append(quoteStart).append(':').append(quoteEnd)
                .append("] '").append(myText.subSequence(quoteStart, quoteEnd)).append('\'');
            LOG.error(sb.toString());
        }

        private void resizeLexemes(final int newSize) {
            myLexStarts = ArrayUtil.realloc(myLexStarts, newSize);
            myLexTypes = ArrayUtil.realloc(myLexTypes, newSize, IElementType.ARRAY_FACTORY);
        }

    }
}
