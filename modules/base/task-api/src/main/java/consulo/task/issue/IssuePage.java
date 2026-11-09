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

import consulo.task.Task;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-31
 */
public final class IssuePage {
    private final List<Task> myIssues;
    private final boolean myHasMore;

    public IssuePage(List<Task> issues, boolean hasMore) {
        myIssues = List.copyOf(issues);
        myHasMore = hasMore;
    }

    public List<Task> getIssues() {
        return myIssues;
    }

    public boolean hasMore() {
        return myHasMore;
    }
}
