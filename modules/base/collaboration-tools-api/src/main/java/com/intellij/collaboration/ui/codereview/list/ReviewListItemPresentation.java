// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.list;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import java.util.Date;
import java.util.Objects;

public interface ReviewListItemPresentation {
    @Nonnull
    String getTitle();

    @Nonnull
    String getId();

    @Nonnull
    Date getCreatedDate();

    @Nullable
    UserPresentation getAuthor();

    @Nullable
    NamedCollection<TagPresentation> getTagGroup();

    @Nullable
    Status getMergeableStatus();

    @Nullable
    Status getBuildStatus();

    @Nullable
    String getState();

    @Nullable
    NamedCollection<UserPresentation> getUserGroup1();

    @Nullable
    NamedCollection<UserPresentation> getUserGroup2();

    @Nullable
    CommentsCounter getCommentsCounter();

    @Nullable
    Boolean getSeen();

    final class Simple implements ReviewListItemPresentation {
        private final @Nonnull String title;
        private final @Nonnull String id;
        private final @Nonnull Date createdDate;
        private final @Nullable UserPresentation author;
        private final @Nullable NamedCollection<TagPresentation> tagGroup;
        private final @Nullable Status mergeableStatus;
        private final @Nullable Status buildStatus;
        private final @Nullable String state;
        private final @Nullable NamedCollection<UserPresentation> userGroup1;
        private final @Nullable NamedCollection<UserPresentation> userGroup2;
        private final @Nullable CommentsCounter commentsCounter;
        private final @Nullable Boolean seen;

        public Simple(
            @Nonnull String title,
            @Nonnull String id,
            @Nonnull Date createdDate,
            @Nullable UserPresentation author,
            @Nullable NamedCollection<TagPresentation> tagGroup,
            @Nullable Status mergeableStatus,
            @Nullable Status buildStatus,
            @Nullable String state,
            @Nullable NamedCollection<UserPresentation> userGroup1,
            @Nullable NamedCollection<UserPresentation> userGroup2,
            @Nullable CommentsCounter commentsCounter,
            @Nullable Boolean seen
        ) {
            this.title = title;
            this.id = id;
            this.createdDate = createdDate;
            this.author = author;
            this.tagGroup = tagGroup;
            this.mergeableStatus = mergeableStatus;
            this.buildStatus = buildStatus;
            this.state = state;
            this.userGroup1 = userGroup1;
            this.userGroup2 = userGroup2;
            this.commentsCounter = commentsCounter;
            this.seen = seen;
        }

        public Simple(@Nonnull String title, @Nonnull String id, @Nonnull Date createdDate) {
            this(title, id, createdDate, null, null, null, null, null, null, null, null, null);
        }

        @Override
        public @Nonnull String getTitle() {
            return title;
        }

        @Override
        public @Nonnull String getId() {
            return id;
        }

        @Override
        public @Nonnull Date getCreatedDate() {
            return createdDate;
        }

        @Override
        public @Nullable UserPresentation getAuthor() {
            return author;
        }

        @Override
        public @Nullable NamedCollection<TagPresentation> getTagGroup() {
            return tagGroup;
        }

        @Override
        public @Nullable Status getMergeableStatus() {
            return mergeableStatus;
        }

        @Override
        public @Nullable Status getBuildStatus() {
            return buildStatus;
        }

        @Override
        public @Nullable String getState() {
            return state;
        }

        @Override
        public @Nullable NamedCollection<UserPresentation> getUserGroup1() {
            return userGroup1;
        }

        @Override
        public @Nullable NamedCollection<UserPresentation> getUserGroup2() {
            return userGroup2;
        }

        @Override
        public @Nullable CommentsCounter getCommentsCounter() {
            return commentsCounter;
        }

        @Override
        public @Nullable Boolean getSeen() {
            return seen;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Simple simple = (Simple) o;
            return Objects.equals(title, simple.title) &&
                Objects.equals(id, simple.id) &&
                Objects.equals(createdDate, simple.createdDate) &&
                Objects.equals(author, simple.author) &&
                Objects.equals(tagGroup, simple.tagGroup) &&
                Objects.equals(mergeableStatus, simple.mergeableStatus) &&
                Objects.equals(buildStatus, simple.buildStatus) &&
                Objects.equals(state, simple.state) &&
                Objects.equals(userGroup1, simple.userGroup1) &&
                Objects.equals(userGroup2, simple.userGroup2) &&
                Objects.equals(commentsCounter, simple.commentsCounter) &&
                Objects.equals(seen, simple.seen);
        }

        @Override
        public int hashCode() {
            return Objects.hash(title, id, createdDate, author, tagGroup, mergeableStatus, buildStatus, state,
                userGroup1, userGroup2, commentsCounter, seen
            );
        }

        @Override
        public String toString() {
            return "Simple(title=" + title + ", id=" + id + ", createdDate=" + createdDate +
                ", author=" + author + ", tagGroup=" + tagGroup + ", mergeableStatus=" + mergeableStatus +
                ", buildStatus=" + buildStatus + ", state=" + state + ", userGroup1=" + userGroup1 +
                ", userGroup2=" + userGroup2 + ", commentsCounter=" + commentsCounter + ", seen=" + seen + ")";
        }
    }

    final class Status {
        private final @Nonnull Icon icon;
        private final @Nonnull
        @Nls String tooltip;

        public Status(@Nonnull Icon icon, @Nonnull @Nls String tooltip) {
            this.icon = icon;
            this.tooltip = tooltip;
        }

        public @Nonnull Icon getIcon() {
            return icon;
        }

        public @Nonnull @Nls String getTooltip() {
            return tooltip;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Status status = (Status) o;
            return Objects.equals(icon, status.icon) && Objects.equals(tooltip, status.tooltip);
        }

        @Override
        public int hashCode() {
            return Objects.hash(icon, tooltip);
        }

        @Override
        public String toString() {
            return "Status(icon=" + icon + ", tooltip=" + tooltip + ")";
        }
    }

    final class CommentsCounter {
        private final int count;
        private final @Nonnull
        @Nls String tooltip;

        public CommentsCounter(int count, @Nonnull @Nls String tooltip) {
            this.count = count;
            this.tooltip = tooltip;
        }

        public int getCount() {
            return count;
        }

        public @Nonnull @Nls String getTooltip() {
            return tooltip;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CommentsCounter that = (CommentsCounter) o;
            return count == that.count && Objects.equals(tooltip, that.tooltip);
        }

        @Override
        public int hashCode() {
            return Objects.hash(count, tooltip);
        }

        @Override
        public String toString() {
            return "CommentsCounter(count=" + count + ", tooltip=" + tooltip + ")";
        }
    }
}
