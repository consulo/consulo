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

import consulo.review.details.data.ReviewRequestState;
import consulo.review.list.search.ReviewListSearchValue;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * The search of the sand review list: the text, the state, the author and the labels a review title has to mention.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
public record SandReviewSearchValue(@Nullable String searchQuery,
                                    @Nullable ReviewRequestState state,
                                    @Nullable String author,
                                    List<String> labels) implements ReviewListSearchValue {
    public static final SandReviewSearchValue EMPTY = new SandReviewSearchValue(null, null, null, List.of());
    public static final SandReviewSearchValue DEFAULT = new SandReviewSearchValue(null, ReviewRequestState.OPENED, null, List.of());

    @Override
    public @Nullable String getSearchQuery() {
        return searchQuery;
    }

    @Override
    public int getFilterCount() {
        int count = searchQuery != null ? 1 : 0;
        if (state != null) {
            count++;
        }
        if (author != null) {
            count++;
        }
        if (!labels.isEmpty()) {
            count++;
        }
        return count;
    }

    public SandReviewSearchValue withQuery(@Nullable String query) {
        return new SandReviewSearchValue(query, state, author, labels);
    }

    public SandReviewSearchValue withState(@Nullable ReviewRequestState newState) {
        return new SandReviewSearchValue(searchQuery, newState, author, labels);
    }

    public SandReviewSearchValue withAuthor(@Nullable String newAuthor) {
        return new SandReviewSearchValue(searchQuery, state, newAuthor, labels);
    }

    public SandReviewSearchValue withLabels(@Nullable List<String> newLabels) {
        return new SandReviewSearchValue(searchQuery, state, author, newLabels == null ? List.of() : List.copyOf(newLabels));
    }

    public boolean matches(SandReviewViewModel review) {
        String title = review.getTitle().get().toLowerCase(Locale.ROOT);
        if (searchQuery != null && !title.contains(searchQuery.toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (state != null && review.getReviewRequestState().get() != state) {
            return false;
        }
        if (author != null && !review.getAuthor().getUsername().equals(author)) {
            return false;
        }
        for (String label : labels) {
            if (!title.contains(label.toLowerCase(Locale.ROOT))) {
                return false;
            }
        }
        return true;
    }

    public String getShortText() {
        StringBuilder builder = new StringBuilder();
        if (searchQuery != null) {
            builder.append('"').append(searchQuery).append("\" ");
        }
        if (state != null) {
            builder.append("state:").append(state.name().toLowerCase(Locale.ROOT)).append(' ');
        }
        if (author != null) {
            builder.append("author:").append(author).append(' ');
        }
        for (String label : labels) {
            builder.append("label:").append(label).append(' ');
        }
        return builder.toString().trim();
    }
}
