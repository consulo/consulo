/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.internal;

import consulo.colorScheme.TextAttributes;
import consulo.document.util.ProperTextRange;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;

public class InjectedHighlightTokenInfo {
    @Nonnull
    public final IElementType type;
    @Nonnull
    public final ProperTextRange rangeInsideInjectionHost;
    public final int shredIndex;
    public final TextAttributes attributes;

    public InjectedHighlightTokenInfo(@Nonnull IElementType type, @Nonnull ProperTextRange rangeInsideInjectionHost, int shredIndex, @Nonnull TextAttributes attributes) {
        this.type = type;
        this.rangeInsideInjectionHost = rangeInsideInjectionHost;
        this.shredIndex = shredIndex;
        this.attributes = attributes;
    }
}
