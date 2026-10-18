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
package consulo.review.timeline;

import consulo.review.list.UserPresentation;
import consulo.util.concurrent.coroutine.ObservableValue;

import java.util.Date;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public interface CodeReviewCommentViewModel extends CodeReviewTimelineItem {
    String getId();

    UserPresentation getAuthor();

    Date getCreatedAt();

    /**
     * Markdown.
     */
    ObservableValue<String> getBody();

    /**
     * Whether the comment was written by the current user.
     */
    default boolean isOwn() {
        return false;
    }
}
