// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.issues;

import consulo.application.util.HtmlChunk;
import consulo.project.Project;
import consulo.util.io.URLUtil;
import consulo.versionControlSystem.IssueNavigationConfiguration;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.function.BiFunction;

public final class IssueIdsProcessor {

    private IssueIdsProcessor() {
    }

    public static @Nonnull String markdownLink(@Nonnull String text, @Nonnull String target) {
        return "[" + text + "](" + target + ")";
    }

    public static @Nonnull String processIssueIdsMarkdown(@Nonnull Project project, @Nonnull String markdown) {
        return processIssueIds(project, markdown, IssueIdsProcessor::markdownLink);
    }

    public static @Nonnull String processIssueIdsHtml(@Nonnull Project project, @Nonnull String htmlText) {
        return processIssueIds(project, htmlText, (text, target) -> HtmlChunk.link(target, text).toString());
    }

    private static @Nonnull String processIssueIds(
        @Nonnull Project project,
        @Nonnull String textToProcess,
        @Nonnull BiFunction<String, String, String> linkConverter
    ) {
        StringBuilder markdownWithIssueLinksBuilder = new StringBuilder();
        List<IssueNavigationConfiguration.LinkMatch> allMatches = parseIssuesAndLinks(project, textToProcess);
        List<IssueNavigationConfiguration.LinkMatch> filteredMatches = allMatches.stream()
            .filter(match -> !isPossiblyLink(match, textToProcess))
            .toList();

        IssueNavigationConfiguration.processTextWithLinks(
            textToProcess,
            filteredMatches,
            text -> markdownWithIssueLinksBuilder.append(text),
            (text, target) -> markdownWithIssueLinksBuilder.append(linkConverter.apply(text, target))
        );
        return markdownWithIssueLinksBuilder.toString();
    }

    // We need to parse issues AND links since issue id can be located inside link and we shouldn't change the link
    private static @Nonnull List<IssueNavigationConfiguration.LinkMatch> parseIssuesAndLinks(
        @Nonnull Project project, @Nonnull String text
    ) {
        return IssueNavigationConfiguration.getInstance(project).findIssueLinks(text);
    }

    private static boolean isPossiblyLink(
        @Nonnull IssueNavigationConfiguration.LinkMatch match,
        @Nonnull String textToProcess
    ) {
        return URLUtil.canContainUrl(match.getRange().subSequence(textToProcess).toString());
    }
}
