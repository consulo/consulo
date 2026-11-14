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
package consulo.ai.editor.impl.internal.review;

import consulo.review.list.UserPresentation;
import consulo.review.timeline.CodeReviewCommentViewModel;
import consulo.util.concurrent.coroutine.ObservableValue;
import org.jspecify.annotations.Nullable;

import java.util.Date;

/**
 * A comment the user wrote on a plan - a draft until it goes to the agent with the others.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class AIReviewComment implements CodeReviewCommentViewModel {
    private final String myId;
    private final String myText;
    private final Date myCreatedAt = new Date();
    private final Runnable myDelete;

    AIReviewComment(String id, String text, Runnable delete) {
        myId = id;
        myText = text;
        myDelete = delete;
    }

    String getText() {
        return myText;
    }

    @Override
    public String getId() {
        return myId;
    }

    @Override
    public UserPresentation getAuthor() {
        return AIReviewUser.INSTANCE;
    }

    @Override
    public @Nullable Date getCreatedAt() {
        return myCreatedAt;
    }

    @Override
    public ObservableValue<String> getBody() {
        return ObservableValue.of(myText);
    }

    @Override
    public boolean isOwn() {
        return true;
    }

    @Override
    public boolean canDelete() {
        return true;
    }

    @Override
    public void delete() {
        myDelete.run();
    }
}
