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
package consulo.language.ast;

import consulo.language.Language;

/**
 * @author VISTALL
 * @since 2026-06-16
 */
public non-sealed interface StandardTokenTypes extends TokenType {
    /**
     * Token type for a sequence of whitespace characters.
     */
    IElementType WHITE_SPACE = new IElementType("WHITE_SPACE", Language.ANY);

    /**
     * Token type for a character which is not valid in the position where it was encountered,
     * according to the language grammar.
     */
    IElementType BAD_CHARACTER = new IElementType("BAD_CHARACTER", Language.ANY);

    IElementType ERROR_ELEMENT = new IElementType("ERROR_ELEMENT", Language.ANY);

    IElementType CODE_FRAGMENT = new IFileElementType("CODE_FRAGMENT", Language.ANY);

    IElementType DUMMY_HOLDER = new IFileElementType("DUMMY_HOLDER", Language.ANY);
}
