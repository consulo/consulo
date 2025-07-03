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
package consulo.compiler;

import consulo.compiler.localize.CompilerLocalize;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * A set of constants describing possible message categories.
 *
 * @see CompilerMessage#getCategory()
 * @see CompileContext#addMessage(CompilerMessageCategory, String, String, int, int)
 */
public enum CompilerMessageCategory {
    ERROR(CompilerLocalize.messageCategoryError()),
    WARNING(CompilerLocalize.messageCategoryWarning()),
    INFORMATION(CompilerLocalize.messageCategoryInformation()),
    STATISTICS(CompilerLocalize.messageCategoryStatistics());

    @Nonnull
    private final LocalizeValue myPresentableText;

    CompilerMessageCategory(@Nonnull LocalizeValue presentableText) {
        myPresentableText = presentableText;
    }

    @Nonnull
    public LocalizeValue getPresentableText() {
        return myPresentableText;
    }
}
