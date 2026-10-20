/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2018 hsz Jakub Chrzanowski <jakub@hsz.mobi>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package consulo.language.ignore.psi;

import consulo.language.ast.IElementType;
import consulo.language.ignore.IgnoreLanguage;
import consulo.language.ignore.localize.LanguageIgnoreLocalize;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Token type definition.
 */
public class IgnoreTokenType extends IElementType {
    private static final Map<String, LocalizeValue> LOCALIZED_DEBUG_NAMES = Map.of(
        "/", LanguageIgnoreLocalize.tokenTypeSlash(),
        "BRACKET_LEFT", LanguageIgnoreLocalize.tokenTypeBracketLeft(),
        "BRACKET_RIGHT", LanguageIgnoreLocalize.tokenTypeBracketRight(),
        "COMMENT", LanguageIgnoreLocalize.tokenTypeComment(),
        "CRLF", LanguageIgnoreLocalize.tokenTypeEmptyLine(),
        "HEADER", LanguageIgnoreLocalize.tokenTypeHeader(),
        "SECTION", LanguageIgnoreLocalize.tokenTypeSection(),
        "VALUE", LanguageIgnoreLocalize.tokenTypeValue(),
        "syntax:", LanguageIgnoreLocalize.tokenTypeSyntax()
    );

    /**
     * Token debug name.
     */
    private final @Nullable LocalizeValue myLocalizedDebugName;

    /**
     * Builds a new instance of {@link IgnoreTokenType}.
     */
    public IgnoreTokenType(String debugName) {
        super(debugName, IgnoreLanguage.INSTANCE);
        myLocalizedDebugName = LOCALIZED_DEBUG_NAMES.get(debugName);
    }

    /**
     * String interpretation of the token type.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return myLocalizedDebugName != null ? myLocalizedDebugName.get() : "IgnoreTokenType." + super.toString();
    }
}
