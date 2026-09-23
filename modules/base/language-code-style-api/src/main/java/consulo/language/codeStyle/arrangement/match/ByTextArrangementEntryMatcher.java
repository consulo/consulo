/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.language.codeStyle.arrangement.match;

import consulo.language.codeStyle.arrangement.ArrangementEntry;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * @author Svetlana.Zemlyanskaya
 */
public class ByTextArrangementEntryMatcher implements ArrangementEntryMatcher {
    private final String myText;

    public ByTextArrangementEntryMatcher(String text) {
        myText = text;
    }

    @Override
    public boolean isMatched(ArrangementEntry entry) {
        return entry instanceof TextAwareArrangementEntry arrangementEntry
            && StringUtil.equals(arrangementEntry.getText(), myText);
    }

    @Override
    public int hashCode() {
        return myText.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o
            || o instanceof ByTextArrangementEntryMatcher that && myText.equals(that.myText);
    }

    @Override
    public String toString() {
        return "with text " + myText;
    }
}
