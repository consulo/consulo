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
public final class IssueAttachment {
    private final String myId;
    private final String myName;
    private final long mySize;
    private final @Nullable String myUrl;

    public IssueAttachment(String id, String name, long size, @Nullable String url) {
        myId = id;
        myName = name;
        mySize = size;
        myUrl = url;
    }

    public String getId() {
        return myId;
    }

    public String getName() {
        return myName;
    }

    public long getSize() {
        return mySize;
    }

    public @Nullable String getUrl() {
        return myUrl;
    }
}
