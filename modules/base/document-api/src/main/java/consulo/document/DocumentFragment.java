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
package consulo.document;

import consulo.document.util.TextRange;
import org.jspecify.annotations.Nullable;

public class DocumentFragment {
    private final Document myDocument;
    private final TextRange myTextRange;

    public DocumentFragment(Document document, int startOffset, int endOffset) {
        myDocument = document;
        myTextRange = new TextRange(startOffset, endOffset);
    }

    public Document getDocument() {
        return myDocument;
    }

    public TextRange getTextRange() {
        return myTextRange;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DocumentFragment)) {
            return false;
        }

        DocumentFragment that = (DocumentFragment) o;

        return myDocument.equals(that.myDocument)
            && myTextRange.equals(that.myTextRange);
    }

    @Override
    public int hashCode() {
        return 29 * myDocument.hashCode() + myTextRange.hashCode();
    }
}