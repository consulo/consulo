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
package consulo.task.issue;

import org.jspecify.annotations.Nullable;

import java.util.Date;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
public final class IssueComment {
    private final String myId;
    private final @Nullable String myAuthor;
    private final @Nullable Date myDate;
    private final String myText;

    public IssueComment(String id, @Nullable String author, @Nullable Date date, String text) {
        myId = id;
        myAuthor = author;
        myDate = date;
        myText = text;
    }

    public String getId() {
        return myId;
    }

    public @Nullable String getAuthor() {
        return myAuthor;
    }

    public @Nullable Date getDate() {
        return myDate;
    }

    public String getText() {
        return myText;
    }
}
