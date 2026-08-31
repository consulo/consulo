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
package consulo.language.ast;

import consulo.language.internal.TokenTypeInternal;

public sealed interface TokenType permits TokenTypeInternal, StandardTokenTypes {
    @Deprecated
    IElementType WHITE_SPACE = StandardTokenTypes.WHITE_SPACE;
    @Deprecated
    IElementType BAD_CHARACTER = StandardTokenTypes.BAD_CHARACTER;
    @Deprecated
    IElementType ERROR_ELEMENT = StandardTokenTypes.ERROR_ELEMENT;
    @Deprecated
    IElementType CODE_FRAGMENT = StandardTokenTypes.CODE_FRAGMENT;
    @Deprecated
    IElementType DUMMY_HOLDER = StandardTokenTypes.DUMMY_HOLDER;
}
