// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.spellcheker.text.util;

import consulo.application.progress.ProgressManager;
import consulo.document.util.TextRange;
import consulo.language.spellcheker.text.TextContent;
import consulo.language.spellcheker.text.TextContent.Exclusion;
import consulo.language.spellcheker.text.TextContent.ExclusionKind;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HtmlUtils {
    private static final Pattern anyTag = Pattern.compile("</?(\\w+)[^>]*>");
    private static final Pattern closingTag = Pattern.compile("</\\w+\\s*>");

    public static final Set<String> commonBlockElements =
        Set.of("body", "p", "br", "td", "li", "title", "h1", "h2", "h3", "h4", "h5", "h6", "hr", "table", "ol", "ul", "pre");

    private static final Set<String> commonMarkupElements = Set.of("span", "i", "em", "b", "u", "font", "a", "s", "strong", "sub", "sup");
    private static final Pattern htmlEntityPattern = Pattern.compile("&([a-z]+|#[0-9]+|#[xX][0-9a-fA-F]+);");

    private static final Pattern nbsp = Pattern.compile("&nbsp;");
    private static final Pattern tab = Pattern.compile("&#9;");

    private HtmlUtils() {
    }

    /**
     * Remove HTML markup from a text, splitting it at block elements (like {@code <p>}),
     * marking common HTML markup tags (like {@code <i>}) as markup offsets,
     * replacing all other tags with unknown fragments,
     * converting {@code &nbsp;} to a non-breaking space,
     * and marking all other HTML entities as unknown fragments.
     */
    @SuppressWarnings("deprecation")
    public static List<TextContent> excludeHtml(@Nullable TextContent content) {
        if (content == null) {
            return List.of();
        }

        List<TextContent> components = new ArrayList<>();
        int lastComponentStart = 0;
        int matchEnd = 0;
        Matcher matcher = anyTag.matcher(content);
        while (matcher.find(matchEnd)) {
            matchEnd = matcher.end();
            ProgressManager.checkCanceled();

            String tagName = matcher.group(1);
            if (commonBlockElements.contains(tagName)) {
                ContainerUtil.addIfNotNull(components, content.subText(new TextRange(lastComponentStart, matcher.start())));
                lastComponentStart = matcher.end();
            }
        }
        ContainerUtil.addIfNotNull(components, content.subText(new TextRange(lastComponentStart, content.length())));

        return ContainerUtil.mapNotNull(components, component -> {
            TextContent removed = removeHtml(component);
            TextContent noTags = removed == null ? null : removed.trimWhitespace();
            if (noTags == null) {
                return null;
            }
            TextContent noNbsp = nbspToSpace(noTags);
            if (noNbsp == null) {
                return null;
            }
            List<TextRange> entityRanges = Text.allOccurrences(htmlEntityPattern, noNbsp);
            if (entityRanges.isEmpty()) {
                return noNbsp;
            }
            return noNbsp.excludeRanges(ContainerUtil.map(entityRanges, Exclusion::markUnknown));
        });
    }

    /**
     * Remove HTML markup from a text, replacing it with unknown or markup (for some common HTML tags) offsets.
     *
     * @deprecated use {@link #excludeHtml}
     */
    @Deprecated(forRemoval = true)
    public static @Nullable TextContent removeHtml(@Nullable TextContent _content) {
        if (_content == null) {
            return null;
        }
        TextContent content = _content;

        while (StringUtil.startsWith(content, "<html>") || StringUtil.startsWith(content, "<body>")) {
            content = content.excludeRange(new TextRange(0, 6));
        }
        while (StringUtil.endsWith(content, "</html>") || StringUtil.endsWith(content, "</body>")) {
            content = content.excludeRange(TextRange.from(content.length() - 7, 7));
        }

        List<Exclusion> exclusions = new ArrayList<>();

        int matchEnd = 0;
        Matcher matcher = anyTag.matcher(content);
        while (matcher.find(matchEnd)) {
            matchEnd = matcher.end();
            ProgressManager.checkCanceled();
            int matchStart = matcher.start();
            String tagName = matcher.group(1);
            if (!Character.isLetterOrDigit(tagName.charAt(0))) {
                continue;
            }

            ExclusionKind exclusionKind = commonMarkupElements.contains(tagName) ? ExclusionKind.markup : ExclusionKind.unknown;
            if (closingTag.matcher(content.subSequence(matchStart, matchEnd)).matches()) {
                exclusions.add(new Exclusion(matchStart, matchEnd, exclusionKind));
                tagClosed(content, exclusions, content.subSequence(matchStart + 2, matchEnd - 1).toString().strip());
            }
            else {
                exclusions.add(new Exclusion(matchStart, matchEnd, exclusionKind));
            }
        }
        return content.excludeRanges(exclusions);
    }

    private static @Nullable String openingTagName(TextContent content, int tagRangeStart, int tagRangeEnd) {
        if (Character.isLetter(content.charAt(tagRangeStart + 1))) {
            return takeNonWhitespaces(content.subSequence(tagRangeStart + 1, tagRangeEnd - 1).toString());
        }
        return null;
    }

    private static void tagClosed(TextContent content, List<Exclusion> exclusions, String tagName) {
        if (commonMarkupElements.contains(tagName)) {
            return;
        }

        int openingIndex = -1;
        for (int i = exclusions.size() - 1; i >= 0; i--) {
            Exclusion it = exclusions.get(i);
            if (Objects.equals(openingTagName(content, it.start, it.end), tagName) && content.charAt(it.end - 2) != '/') {
                openingIndex = i;
                break;
            }
        }
        if (openingIndex >= 0) {
            exclusions.set(
                openingIndex,
                Exclusion.markUnknown(new TextRange(exclusions.get(openingIndex).start, exclusions.get(exclusions.size() - 1).end))
            );
            exclusions.subList(openingIndex + 1, exclusions.size()).clear();
        }
    }

    private static String takeNonWhitespaces(String text) {
        int i = 0;
        while (i < text.length() && !Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        return text.substring(0, i);
    }

    public static boolean isSpaceEntity(String text) {
        return text.equals(nbsp.pattern()) || text.equals(tab.pattern());
    }

    public static boolean isShyEntity(String text) {
        return text.equals("&shy;");
    }

    private static @Nullable TextContent inlineEntity(@Nullable TextContent content, Pattern pattern, char space) {
        if (content == null) {
            return null;
        }

        List<TextRange> spaces = Text.allOccurrences(pattern, content);
        if (spaces.isEmpty()) {
            return content.trimWhitespace();
        }

        List<TextContent> components = new ArrayList<>();
        for (int i = 0; i < spaces.size(); i++) {
            int prevEnd = i == 0 ? 0 : spaces.get(i - 1).getEndOffset();
            TextContent sub = content.subText(new TextRange(prevEnd, spaces.get(i).getStartOffset()));
            ContainerUtil.addIfNotNull(components, sub == null ? null : sub.trimWhitespace());
        }
        TextContent last = content.subText(new TextRange(spaces.get(spaces.size() - 1).getEndOffset(), content.length()));
        ContainerUtil.addIfNotNull(components, last == null ? null : last.trimWhitespace());
        return TextContent.joinWithWhitespace(space, components);
    }

    public static @Nullable TextContent inlineSpaceEntities(@Nullable TextContent content) {
        return inlineEntity(nbspToSpace(content), tab, '\t');
    }

    public static @Nullable TextContent nbspToSpace(@Nullable TextContent content) {
        return inlineEntity(content, nbsp, ' ');
    }
}
