// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details;

import jakarta.annotation.Nonnull;
import org.intellij.lang.annotations.Language;

import java.util.Date;
import java.util.Objects;

public final class CommitPresentation {
    private final @Language("HTML")
    @Nonnull String titleHtml;
    private final @Language("HTML")
    @Nonnull String descriptionHtml;
    private final @Nonnull String author;
    private final @Nonnull Date committedDate;

    public CommitPresentation(
        @Language("HTML") @Nonnull String titleHtml,
        @Language("HTML") @Nonnull String descriptionHtml,
        @Nonnull String author,
        @Nonnull Date committedDate
    ) {
        this.titleHtml = titleHtml;
        this.descriptionHtml = descriptionHtml;
        this.author = author;
        this.committedDate = committedDate;
    }

    public @Nonnull String getTitleHtml() {
        return titleHtml;
    }

    public @Nonnull String getDescriptionHtml() {
        return descriptionHtml;
    }

    public @Nonnull String getAuthor() {
        return author;
    }

    public @Nonnull Date getCommittedDate() {
        return committedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CommitPresentation that = (CommitPresentation) o;
        return Objects.equals(titleHtml, that.titleHtml) &&
            Objects.equals(descriptionHtml, that.descriptionHtml) &&
            Objects.equals(author, that.author) &&
            Objects.equals(committedDate, that.committedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(titleHtml, descriptionHtml, author, committedDate);
    }

    @Override
    public String toString() {
        return "CommitPresentation(" +
            "titleHtml='" + titleHtml + '\'' +
            ", descriptionHtml='" + descriptionHtml + '\'' +
            ", author='" + author + '\'' +
            ", committedDate=" + committedDate +
            ')';
    }
}
