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
package consulo.language.editor.completion.lookup;

import consulo.codeEditor.Editor;
import org.jspecify.annotations.Nullable;

/**
 * @author peter
 */
public class CharTailType extends TailType {
    private final char myChar;
    private final boolean myOverwrite;

    public CharTailType(char aChar) {
        this(aChar, true);
    }

    public CharTailType(char aChar, boolean overwrite) {
        myChar = aChar;
        myOverwrite = overwrite;
    }

    @Override
    public boolean isApplicable(InsertionContext context) {
        return !context.shouldAddCompletionChar() || context.getCompletionChar() != myChar;
    }

    @Override
    public int processTail(Editor editor, int tailOffset) {
        return insertChar(editor, tailOffset, myChar, myOverwrite);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o
            || o instanceof CharTailType that && myChar == that.myChar;
    }

    @Override
    public int hashCode() {
        return (int) myChar;
    }

    @Override
    public String toString() {
        return "CharTailType:\'" + myChar + "\'";
    }
}
