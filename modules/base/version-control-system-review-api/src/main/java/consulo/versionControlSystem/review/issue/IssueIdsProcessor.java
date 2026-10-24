// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.review.issue;

import consulo.application.util.HtmlChunk;
import consulo.project.Project;
import consulo.util.io.URLUtil;
import consulo.versionControlSystem.IssueNavigationConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public final class IssueIdsProcessor {
    private IssueIdsProcessor() {
    }

    public static String markdownLink(String text, String target) {
        return "[" + text + "](" + target + ")";
    }

    public static String processIssueIdsMarkdown(Project project, String markdown) {
        return processIssueIds(project, markdown, IssueIdsProcessor::markdownLink);
    }

    public static String processIssueIdsHtml(Project project, String htmlText) {
        return processIssueIds(project, htmlText, (text, target) -> HtmlChunk.link(target, text).toString());
    }

    private static String processIssueIds(Project project, String textToProcess, BiFunction<String, String, String> linkConverter) {
        StringBuilder markdownWithIssueLinksBuilder = new StringBuilder();
        IssueNavigationConfiguration.processTextWithLinks(
            textToProcess,
            parseIssuesAndLinks(project, textToProcess),
            markdownWithIssueLinksBuilder::append,
            (text, target) -> markdownWithIssueLinksBuilder.append(linkConverter.apply(text, target))
        );
        return markdownWithIssueLinksBuilder.toString();
    }

    // We need to parse issues AND links since issue id can be located inside link and we shouldn't change the link
    private static List<IssueNavigationConfiguration.LinkMatch> parseIssuesAndLinks(Project project, String textToProcess) {
        List<IssueNavigationConfiguration.LinkMatch> result = new ArrayList<>();
        for (IssueNavigationConfiguration.LinkMatch match : IssueNavigationConfiguration.getInstance(project).findIssueLinks(textToProcess)) {
            if (!URLUtil.canContainUrl(match.getRange().substring(textToProcess))) {
                result.add(match);
            }
        }
        return result;
    }
}
