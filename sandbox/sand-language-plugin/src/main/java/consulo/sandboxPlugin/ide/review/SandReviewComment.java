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
package consulo.sandboxPlugin.ide.review;

import consulo.review.list.UserPresentation;
import consulo.review.timeline.CodeReviewCommentViewModel;
import consulo.util.concurrent.coroutine.ObservableValue;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public record SandReviewComment(String id, SandReviewUser author, String text, Instant time) implements CodeReviewCommentViewModel {
    public SandReviewComment(SandReviewUser author, String text, Instant time) {
        this(UUID.randomUUID().toString(), author, text, time);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public UserPresentation getAuthor() {
        return author;
    }

    @Override
    public Date getCreatedAt() {
        return Date.from(time);
    }

    @Override
    public ObservableValue<String> getBody() {
        return ObservableValue.of(text);
    }

    @Override
    public boolean isOwn() {
        return author.equals(SandReviewService.CURRENT_USER);
    }
}
