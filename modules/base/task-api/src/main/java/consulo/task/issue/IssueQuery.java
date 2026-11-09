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

/**
 * @author VISTALL
 * @since 2026-10-31
 */
public final class IssueQuery {
    private final @Nullable String myText;
    private final boolean myIncludeClosed;

    public IssueQuery(@Nullable String text, boolean includeClosed) {
        myText = text;
        myIncludeClosed = includeClosed;
    }

    public @Nullable String getText() {
        return myText;
    }

    public boolean isIncludeClosed() {
        return myIncludeClosed;
    }
}
