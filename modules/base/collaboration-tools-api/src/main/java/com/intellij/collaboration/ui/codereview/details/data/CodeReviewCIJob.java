// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.details.data;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public final class CodeReviewCIJob {
    private final @Nonnull String name;
    private final @Nonnull CodeReviewCIJobState status;
    private final boolean isRequired;
    private final @Nullable String detailsUrl;

    public CodeReviewCIJob(
        @Nonnull String name,
        @Nonnull CodeReviewCIJobState status,
        boolean isRequired,
        @Nullable String detailsUrl
    ) {
        this.name = name;
        this.status = status;
        this.isRequired = isRequired;
        this.detailsUrl = detailsUrl;
    }

    public @Nonnull String getName() {
        return name;
    }

    public @Nonnull CodeReviewCIJobState getStatus() {
        return status;
    }

    public boolean isRequired() {
        return isRequired;
    }

    public @Nullable String getDetailsUrl() {
        return detailsUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CodeReviewCIJob that = (CodeReviewCIJob) o;
        return isRequired == that.isRequired &&
            Objects.equals(name, that.name) &&
            status == that.status &&
            Objects.equals(detailsUrl, that.detailsUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, status, isRequired, detailsUrl);
    }

    @Override
    public String toString() {
        return "CodeReviewCIJob(" +
            "name='" + name + '\'' +
            ", status=" + status +
            ", isRequired=" + isRequired +
            ", detailsUrl='" + detailsUrl + '\'' +
            ')';
    }
}
