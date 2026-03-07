// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.commits;

import kotlin.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Utility class for commit message operations.
 */
public final class CommitsUtil {

    private CommitsUtil() {
    }

    /**
     * Splits full commit message into subject and description:
     * First line becomes subject, everything after first line becomes description.
     * Also supports empty line that separates subject and description.
     *
     * @param commitMessage full commit message
     * @return pair of subject and description based on full commit message
     */
    public static @Nonnull Pair<String, String> splitCommitMessage(@Nullable String commitMessage) {
        // Trim original
        String message = commitMessage != null ? commitMessage.trim() : "";
        if (message.isEmpty()) {
            return new Pair<>("", "");
        }
        int firstLineEnd = message.indexOf("\n");
        String subject;
        String description;
        if (firstLineEnd > -1) {
            // Subject is always first line
            subject = message.substring(0, firstLineEnd).trim();
            // Description is all text after first line, we also trim it to remove empty lines on start of description
            description = message.substring(firstLineEnd + 1).trim();
        }
        else {
            // If we don't have any line separators and cannot detect description,
            // we just assume that it is one-line commit and use full message as subject with empty description
            subject = message;
            description = "";
        }

        return new Pair<>(subject, description);
    }
}
