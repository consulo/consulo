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
package consulo.it.daemon;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.rawHighlight.HighlightInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Expected highlights written into the source itself: {@code class <error descr="Test Error">Item</error> {}}. The
 * markup is stripped out of the text before the file is written, so a test marks up a string, writes
 * {@link #getText()} to disk and opens that.
 * <p>
 * Supported tags are {@code error}, {@code warning}, {@code weak_warning}, {@code info}, {@code text_attr},
 * {@code EOLError} and {@code EOLWarning}, each taking an optional {@code descr}, plus {@code lineMarker}, which
 * is collected separately and asserted with {@link #checkLineMarkers}. Any other tag or attribute is rejected
 * rather than ignored, so an expectation never silently checks nothing. {@link #CARET_MARKER} is stripped too
 * and its position handed back by {@link #getCaretOffset()}.
 *
 * @author VISTALL
 */
public final class ExpectedHighlightingData {
    private static final Map<String, HighlightSeverity> SEVERITIES = Map.of(
        "error", HighlightSeverity.ERROR,
        "warning", HighlightSeverity.WARNING,
        "weak_warning", HighlightSeverity.WEAK_WARNING,
        "info", HighlightSeverity.INFORMATION,
        "text_attr", HighlightSeverity.TEXT_ATTRIBUTES,
        "EOLError", HighlightSeverity.ERROR,
        "EOLWarning", HighlightSeverity.WARNING
    );

    private static final String LINE_MARKER = "lineMarker";

    /**
     * Where a caret-driven action or a reference lookup happens.
     */
    public static final String CARET_MARKER = "<caret>";

    private static final String TAG_NAMES = String.join("|", SEVERITIES.keySet()) + "|" + LINE_MARKER;
    private static final Pattern OPENING_TAG = Pattern.compile("<(" + TAG_NAMES + ")(\\s[^>]*)?>");
    private static final Pattern CLOSING_TAG = Pattern.compile("</(" + TAG_NAMES + ")>");
    private static final Pattern ATTRIBUTE = Pattern.compile("(\\w+)\\s*=\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");

    private final String myText;
    private final List<ExpectedHighlight> myExpected;
    private final List<ExpectedLineMarker> myExpectedLineMarkers;
    private final List<Integer> myCaretOffsets;

    private ExpectedHighlightingData(
        String text,
        List<ExpectedHighlight> expected,
        List<ExpectedLineMarker> lineMarkers,
        List<Integer> caretOffsets
    ) {
        myText = text;
        myExpected = expected;
        myExpectedLineMarkers = lineMarkers;
        myCaretOffsets = caretOffsets;
    }

    public static ExpectedHighlightingData parse(String markedUpText) {
        StringBuilder text = new StringBuilder();
        List<ExpectedHighlight> expected = new ArrayList<>();
        List<ExpectedLineMarker> lineMarkers = new ArrayList<>();
        List<Integer> caretOffsets = new ArrayList<>();
        List<Object[]> open = new ArrayList<>();

        int position = 0;
        while (position < markedUpText.length()) {
            if (markedUpText.charAt(position) != '<') {
                text.append(markedUpText.charAt(position));
                position++;
                continue;
            }

            if (markedUpText.startsWith(CARET_MARKER, position)) {
                caretOffsets.add(text.length());
                position += CARET_MARKER.length();
                continue;
            }

            Matcher closing = CLOSING_TAG.matcher(markedUpText);
            if (closing.find(position) && closing.start() == position) {
                String tag = closing.group(1);
                int index = lastIndexOfOpen(open, tag);
                if (index < 0) {
                    throw new IllegalArgumentException("</" + tag + "> at offset " + position + " was never opened");
                }
                Object[] frame = open.remove(index);
                if (LINE_MARKER.equals(tag)) {
                    lineMarkers.add(new ExpectedLineMarker((Integer) frame[1], text.length(), (String) frame[2]));
                }
                else {
                    expected.add(new ExpectedHighlight(
                        tag,
                        SEVERITIES.get(tag),
                        tag.startsWith("EOL"),
                        (Integer) frame[1],
                        text.length(),
                        (String) frame[2]
                    ));
                }
                position = closing.end();
                continue;
            }

            Matcher opening = OPENING_TAG.matcher(markedUpText);
            if (opening.find(position) && opening.start() == position) {
                String tag = opening.group(1);
                open.add(new Object[]{tag, text.length(), parseDescription(tag, opening.group(2))});
                position = opening.end();
                continue;
            }

            text.append('<');
            position++;
        }

        if (!open.isEmpty()) {
            throw new IllegalArgumentException("<" + open.get(open.size() - 1)[0] + "> was never closed");
        }
        return new ExpectedHighlightingData(text.toString(), expected, lineMarkers, caretOffsets);
    }

    private static int lastIndexOfOpen(List<Object[]> open, String tag) {
        for (int i = open.size() - 1; i >= 0; i--) {
            if (tag.equals(open.get(i)[0])) {
                return i;
            }
        }
        return -1;
    }

    private static String parseDescription(String tag, String attributes) {
        if (attributes == null || attributes.isBlank()) {
            return ExpectedHighlight.ANY_TEXT;
        }
        String description = null;
        Matcher matcher = ATTRIBUTE.matcher(attributes);
        int consumed = 0;
        while (matcher.find()) {
            String name = matcher.group(1);
            if (!"descr".equals(name)) {
                throw new IllegalArgumentException(
                    "<" + tag + "> does not support the attribute '" + name + "'; only descr is compared"
                );
            }
            description = unescape(matcher.group(2));
            consumed += matcher.end() - matcher.start();
        }
        if (description == null || consumed != attributes.trim().length()) {
            throw new IllegalArgumentException("<" + tag + "> has attributes that are not understood: " + attributes.trim());
        }
        return description;
    }

    private static String unescape(String value) {
        return value.replace("\\\"", "\"").replace("\\n", "\n").replace("\\\\", "\\");
    }

    /**
     * @return the text with all markup removed, which is what gets written to the file under test
     */
    public String getText() {
        return myText;
    }

    public List<ExpectedHighlight> getExpected() {
        return List.copyOf(myExpected);
    }

    public List<ExpectedLineMarker> getExpectedLineMarkers() {
        return List.copyOf(myExpectedLineMarkers);
    }

    public List<Integer> getCaretOffsets() {
        return List.copyOf(myCaretOffsets);
    }

    /**
     * The single {@link #CARET_MARKER} position, as an offset into {@link #getText()}. It comes straight out of the
     * markup, so a reference lookup needs no editor.
     *
     * @throws IllegalStateException when the markup does not hold exactly one caret
     */
    public int getCaretOffset() {
        if (myCaretOffsets.size() != 1) {
            throw new IllegalStateException(
                "expected exactly one " + CARET_MARKER + " but found " + myCaretOffsets.size()
            );
        }
        return myCaretOffsets.get(0);
    }

    /**
     * Compares severity, offsets, end-of-line placement and description. Everything else a
     * {@link HighlightInfo} carries is not part of the markup and is not compared.
     *
     * @throws AssertionError describing every expectation that was not met and every highlight that was not
     *                        asked for
     */
    public void checkResult(Collection<? extends HighlightInfo> actual) {
        Set<HighlightSeverity> checked = checkedSeverities();
        List<HighlightInfo> unmatchedActual = new ArrayList<>();
        for (HighlightInfo info : actual) {
            if (checked.contains(info.getSeverity())) {
                unmatchedActual.add(info);
            }
        }
        List<ExpectedHighlight> unmatchedExpected = new ArrayList<>();

        for (ExpectedHighlight expected : myExpected) {
            HighlightInfo match = null;
            for (HighlightInfo info : unmatchedActual) {
                if (expected.matches(info)) {
                    match = info;
                    break;
                }
            }
            if (match == null) {
                unmatchedExpected.add(expected);
            }
            else {
                unmatchedActual.remove(match);
            }
        }

        if (unmatchedExpected.isEmpty() && unmatchedActual.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder("highlighting does not match the expected markup");
        if (!unmatchedExpected.isEmpty()) {
            message.append("\n  expected but not produced:");
            for (ExpectedHighlight expected : unmatchedExpected) {
                message.append("\n    ").append(expected).append(" over ").append(fragment(expected.getStartOffset(), expected.getEndOffset()));
            }
        }
        if (!unmatchedActual.isEmpty()) {
            message.append("\n  produced but not expected:");
            for (HighlightInfo info : unmatchedActual) {
                message.append("\n    ").append(describe(info));
            }
        }
        throw new AssertionError(message.toString());
    }

    /**
     * Compares gutter markers by range and tooltip.
     *
     * @throws AssertionError describing every marker that was expected and missing, and every one that was not
     *                        asked for
     */
    @RequiredReadAction
    public void checkLineMarkers(Collection<? extends LineMarkerInfo<?>> actual) {
        List<LineMarkerInfo<?>> unmatchedActual = new ArrayList<>(actual);
        List<ExpectedLineMarker> unmatchedExpected = new ArrayList<>();

        for (ExpectedLineMarker expected : myExpectedLineMarkers) {
            LineMarkerInfo<?> match = null;
            for (LineMarkerInfo<?> marker : unmatchedActual) {
                if (expected.matches(marker)) {
                    match = marker;
                    break;
                }
            }
            if (match == null) {
                unmatchedExpected.add(expected);
            }
            else {
                unmatchedActual.remove(match);
            }
        }

        if (unmatchedExpected.isEmpty() && unmatchedActual.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder("line markers do not match the expected markup");
        if (!unmatchedExpected.isEmpty()) {
            message.append("\n  expected but not produced:");
            for (ExpectedLineMarker expected : unmatchedExpected) {
                message.append("\n    ").append(expected)
                    .append(" over ").append(fragment(expected.getStartOffset(), expected.getEndOffset()));
            }
        }
        if (!unmatchedActual.isEmpty()) {
            message.append("\n  produced but not expected:");
            for (LineMarkerInfo<?> marker : unmatchedActual) {
                message.append("\n    <lineMarker descr=\"").append(marker.getLineMarkerTooltip()).append("\"> at ")
                    .append(marker.startOffset).append("..").append(marker.endOffset)
                    .append(" over ").append(fragment(marker.startOffset, marker.endOffset));
            }
        }
        throw new AssertionError(message.toString());
    }

    /**
     * Only the severities the markup itself mentions take part, plus errors, which are always checked. A
     * highlight of any other severity is neither expected nor unexpected - that is how a test that says nothing
     * about warnings stays unaffected by them.
     */
    private Set<HighlightSeverity> checkedSeverities() {
        Set<HighlightSeverity> checked = new HashSet<>();
        checked.add(HighlightSeverity.ERROR);
        for (ExpectedHighlight expected : myExpected) {
            checked.add(expected.getSeverity());
        }
        return checked;
    }

    private String describe(HighlightInfo info) {
        return "<" + info.getSeverity().getName() + " descr=\""
            + (info.getDescription() == null ? null : info.getDescription().get()) + "\"> at "
            + info.getStartOffset() + ".." + info.getEndOffset()
            + (info.isAfterEndOfLine() ? " (after end of line)" : "")
            + " over " + fragment(info.getStartOffset(), info.getEndOffset());
    }

    private String fragment(int startOffset, int endOffset) {
        int start = Math.max(0, Math.min(startOffset, myText.length()));
        int end = Math.max(start, Math.min(endOffset, myText.length()));
        return "'" + myText.substring(start, end).replace("\n", "\\n") + "'";
    }

    /**
     * @return the expectations grouped by tag, for reporting
     */
    public Map<String, List<ExpectedHighlight>> byTag() {
        Map<String, List<ExpectedHighlight>> result = new LinkedHashMap<>();
        for (ExpectedHighlight expected : myExpected) {
            result.computeIfAbsent(expected.getTag(), tag -> new ArrayList<>()).add(expected);
        }
        return result;
    }
}
