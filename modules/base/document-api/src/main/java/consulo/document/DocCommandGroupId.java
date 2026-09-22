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

import org.jspecify.annotations.Nullable;

public class DocCommandGroupId {
    private final Document myDocument;
    private final Object myGroupId;

    public static DocCommandGroupId noneGroupId(Document doc) {
        return new DocCommandGroupId(doc, new Object());
    }

    public static DocCommandGroupId withGroupId(Document doc, Object groupId) {
        return new DocCommandGroupId(doc, groupId);
    }

    private DocCommandGroupId(Document document, Object groupId) {
        myDocument = document;
        myGroupId = groupId;
    }

    public Document getDocument() {
        return myDocument;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DocCommandGroupId that = (DocCommandGroupId) o;

        return myDocument.equals(that.myDocument)
            && myGroupId.equals(that.myGroupId);
    }

    @Override
    public int hashCode() {
        return 31 * myDocument.hashCode() + myGroupId.hashCode();
    }
}
