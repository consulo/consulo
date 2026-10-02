// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.psi.util;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.logging.Logger;
import consulo.util.collection.SmartList;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Represents string which value is only partially known because of some variables concatenated with or interpolated into the string.
 * <p>
 * For UAST languages it could be obtained from {@code UStringConcatenationsFacade.asPartiallyKnownString}.
 * <p>
 * The common use case is a search for a place in partially known content to inject a reference.
 */
public final class PartiallyKnownString {
    private static final Logger LOG = Logger.getInstance(PartiallyKnownString.class);

    public static final PartiallyKnownString EMPTY = new PartiallyKnownString(List.of());

    private final List<StringEntry> mySegments;

    public PartiallyKnownString(List<StringEntry> segments) {
        mySegments = segments;
    }

    public PartiallyKnownString(StringEntry single) {
        this(List.of(single));
    }

    public PartiallyKnownString(String string, @Nullable PsiElement sourcePsi, TextRange textRange) {
        this(new StringEntry.Known(string, sourcePsi, textRange));
    }

    public PartiallyKnownString(String string) {
        this(string, null, TextRange.EMPTY_RANGE);
    }

    public List<StringEntry> getSegments() {
        return mySegments;
    }

    public @Nullable String getValueIfKnown() {
        if (mySegments.size() == 1 && mySegments.get(0) instanceof StringEntry.Known known) {
            return known.getValue();
        }

        StringBuilder stringBuffer = new StringBuilder();
        for (StringEntry segment : mySegments) {
            if (segment instanceof StringEntry.Known known) {
                stringBuffer.append(known.getValue());
            }
            else if (segment instanceof StringEntry.Unknown) {
                return null;
            }
        }
        return stringBuffer.toString();
    }

    public String getConcatenationOfKnown() {
        if (mySegments.size() == 1 && mySegments.get(0) instanceof StringEntry.Known known) {
            return known.getValue();
        }

        StringBuilder stringBuffer = new StringBuilder();
        for (StringEntry segment : mySegments) {
            if (segment instanceof StringEntry.Known known) {
                stringBuffer.append(known.getValue());
            }
        }
        return stringBuffer.toString();
    }

    @Override
    public String toString() {
        return mySegments.stream()
            .map(segment -> segment instanceof StringEntry.Known known ? known.getValue() : "<???>")
            .collect(Collectors.joining(", "));
    }

    public int findIndexOfInKnown(String pattern) {
        return findIndexOfInKnown(pattern, 0);
    }

    public int findIndexOfInKnown(String pattern, int startFrom) {
        int accumulated = 0;
        for (StringEntry segment : mySegments) {
            if (segment instanceof StringEntry.Known known) {
                int i = known.getValue().indexOf(pattern, startFrom - accumulated);
                if (i >= 0) {
                    return accumulated + i;
                }
                accumulated += known.getValue().length();
            }
        }
        return -1;
    }

    @RequiredReadAction
    private TextRange rangeForSubElement(StringEntry parent, TextRange partRange) {
        Pair<PsiElement, TextRange> aligned = parent.getRangeAlignedToHost();
        if (aligned != null) {
            PsiElement host = aligned.getFirst();
            TextRange hostRange = aligned.getSecond();
            TextRange mapped = mapRangeToHostRange(host, hostRange, partRange);
            if (mapped != null) {
                PsiElement sourcePsi = Objects.requireNonNull(parent.getSourcePsi());
                return mapped.shiftLeft(sourcePsi.getTextRange().getStartOffset() - host.getTextRange().getStartOffset());
            }
        }
        return partRange.shiftRight(parent.getRange().getStartOffset());
    }

    @RequiredReadAction
    private StringEntry.Known buildSegmentWithMappedRange(StringEntry parent, String value, TextRange rangeInPks) {
        return new StringEntry.Known(value, parent.getSourcePsi(), rangeForSubElement(parent, rangeInPks));
    }

    @RequiredReadAction
    public Pair<PartiallyKnownString, PartiallyKnownString> splitAtInKnown(int splitAt) {
        int accumulated = 0;
        List<StringEntry> left = new SmartList<>();
        for (int i = 0; i < mySegments.size(); i++) {
            StringEntry segment = mySegments.get(i);
            if (segment instanceof StringEntry.Known known) {
                String value = known.getValue();
                if (accumulated + value.length() < splitAt) {
                    accumulated += value.length();
                    left.add(segment);
                }
                else {
                    String leftPart = value.substring(0, splitAt - accumulated);
                    String rightPart = value.substring(splitAt - accumulated);
                    left.add(buildSegmentWithMappedRange(segment, leftPart, TextRange.from(0, leftPart.length())));

                    List<StringEntry> right = new ArrayList<>(mySegments.size() - i);
                    if (!rightPart.isEmpty()) {
                        right.add(buildSegmentWithMappedRange(segment, rightPart, TextRange.from(leftPart.length(), rightPart.length())));
                    }
                    right.addAll(mySegments.subList(i + 1, mySegments.size()));
                    return Pair.create(new PartiallyKnownString(left), new PartiallyKnownString(right));
                }
            }
            else if (segment instanceof StringEntry.Unknown) {
                left.add(segment);
            }
        }
        return Pair.create(this, EMPTY);
    }

    @RequiredReadAction
    public List<PartiallyKnownString> split(String pattern) {
        return split(pattern, (sequence, p) -> SplitEscaper.ACCEPT_ALL);
    }

    @RequiredReadAction
    public List<PartiallyKnownString> split(String pattern, BiFunction<CharSequence, String, SplitEscaper> escaperFactory) {
        List<PartiallyKnownString> result = new SmartList<>();
        List<StringEntry> pending = new ArrayList<>();
        int index = 0;
        while (true) {
            if (index >= mySegments.size()) {
                result.add(new PartiallyKnownString(pending));
                return result;
            }

            StringEntry head = mySegments.get(index);
            index++;

            if (head instanceof StringEntry.Unknown) {
                pending.add(head);
            }
            else if (head instanceof StringEntry.Known known) {
                String value = known.getValue();

                List<TextRange> stringParts = PartiallyKnownStringUtil.splitToTextRanges(value, pattern, escaperFactory).toList();
                if (stringParts.size() == 1) {
                    pending.add(head);
                }
                else {
                    TextRange first = stringParts.get(0);
                    pending.add(new StringEntry.Known(first.substring(value), head.getSourcePsi(), rangeForSubElement(head, first)));
                    result.add(new PartiallyKnownString(pending));
                    for (TextRange it : stringParts.subList(1, stringParts.size() - 1)) {
                        result.add(new PartiallyKnownString(it.substring(value), head.getSourcePsi(), rangeForSubElement(head, it)));
                    }

                    TextRange last = stringParts.get(stringParts.size() - 1);
                    pending = new ArrayList<>();
                    pending.add(new StringEntry.Known(last.substring(value), head.getSourcePsi(), rangeForSubElement(head, last)));
                }
            }
        }
    }

    @RequiredReadAction
    public @Nullable TextRange mapRangeToHostRange(PsiElement host, TextRange rangeInPks) {
        return mapRangeToHostRange(host, ElementManipulators.getValueTextRange(host), rangeInPks);
    }

    /**
     * @param rangeInHost - range in the {@code host} if only the part of the {@code host} should be considered.
     *                    useful if {@code host} corresponds to multiple {@link PartiallyKnownString}
     * @return the range in the given {@code host} (encoder-aware) that corresponds to the {@code rangeInPks}
     * in the {@link #getValueIfKnown()}
     * <p>
     * NOTE: currently supports only single-segment {@code rangeInPks}
     */
    @RequiredReadAction
    private @Nullable TextRange mapRangeToHostRange(PsiElement host, TextRange rangeInHost, TextRange rangeInPks) {
        int accumulated = 0;
        for (StringEntry segment : mySegments) {
            if (!(segment instanceof StringEntry.Known known)) {
                continue;
            }

            Pair<PsiElement, TextRange> aligned = segment.getRangeAlignedToHost();
            if (aligned == null) {
                continue;
            }
            PsiElement segmentHost = aligned.getFirst();
            TextRange segmentRangeInHost = aligned.getSecond();
            if (!segmentHost.equals(host) || !rangeInHost.contains(segmentRangeInHost)) {
                continue;
            }

            int segmentEnd = accumulated + known.getValue().length();

            if (rangeInPks.getStartOffset() >= accumulated && rangeInPks.getEndOffset() <= segmentEnd) {
                int inSegmentStart = rangeInPks.getStartOffset() - accumulated;
                int inSegmentEnd = rangeInPks.getEndOffset() - accumulated;

                return getHostRangeEscapeAware(host, segmentRangeInHost, inSegmentStart, inSegmentEnd);
            }
            accumulated = segmentEnd;
        }
        return null;
    }

    @RequiredReadAction
    private static TextRange getHostRangeEscapeAware(PsiElement host, TextRange segmentRange, int inSegmentStart, int inSegmentEnd) {
        if (host instanceof PsiLanguageInjectionHost injectionHost) {
            try {
                LiteralTextEscaper<? extends PsiLanguageInjectionHost> escaper = injectionHost.createLiteralTextEscaper();
                StringBuilder decoded = new StringBuilder();
                if (escaper.decode(segmentRange, decoded)) {
                    int decodedLength = decoded.length();
                    int clampedStart = Math.max(0, Math.min(inSegmentStart, decodedLength));
                    int clampedEnd = Math.max(clampedStart, Math.min(inSegmentEnd, decodedLength));
                    int start = escaper.getOffsetInHost(clampedStart, segmentRange);
                    int end = escaper.getOffsetInHost(clampedEnd, segmentRange);
                    if (start != -1 && end != -1 && start <= end) {
                        return new TextRange(start, end);
                    }
                    else {
                        LOG.error(
                            "decoding of " + segmentRange + " failed for " + host + " : [" + start + ", " + end + "] inSegment = [" +
                                inSegmentStart + ", " + inSegmentEnd + "]",
                            PartiallyKnownStringUtil.mkAttachments(host)
                        );
                        return new TextRange(segmentRange.getStartOffset() + inSegmentStart, segmentRange.getStartOffset() + inSegmentEnd);
                    }
                }
            }
            catch (Exception e) {
                if (e instanceof ControlFlowException || e instanceof CancellationException) {
                    throw (RuntimeException) e;
                }
                LOG.error(
                    "decoding of " + segmentRange + " failed for " + host + " inSegment = [" + inSegmentStart + ", " + inSegmentEnd + "]",
                    e,
                    PartiallyKnownStringUtil.mkAttachments(host)
                );
            }
        }

        return new TextRange(segmentRange.getStartOffset() + inSegmentStart, segmentRange.getStartOffset() + inSegmentEnd);
    }

    /**
     * @return the cumulative range in the {@code originalHost} used by this {@link PartiallyKnownString}
     */
    @RequiredReadAction
    public @Nullable TextRange getRangeInHost(PsiElement originalHost) {
        List<TextRange> ranges = new ArrayList<>();
        for (StringEntry segment : mySegments) {
            Pair<PsiElement, TextRange> aligned = segment.getRangeAlignedToHost();
            if (aligned != null && aligned.getFirst().equals(originalHost)) {
                ranges.add(aligned.getSecond());
            }
        }
        if (ranges.isEmpty()) {
            return null;
        }
        TextRange result = ranges.get(0);
        for (int i = 1; i < ranges.size(); i++) {
            result = result.union(ranges.get(i));
        }
        return result;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return mySegments.equals(((PartiallyKnownString) other).mySegments);
    }

    @Override
    public int hashCode() {
        return mySegments.hashCode();
    }
}
