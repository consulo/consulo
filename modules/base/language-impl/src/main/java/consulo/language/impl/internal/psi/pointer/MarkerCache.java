// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.impl.internal.psi.pointer;

import consulo.document.event.DocumentEvent;
import consulo.document.impl.FrozenDocument;
import consulo.document.impl.ManualRangeMarker;
import consulo.document.impl.event.DocumentEventImpl;
import consulo.document.util.ProperTextRange;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.document.util.UnfairTextRange;
import consulo.language.Language;
import consulo.language.impl.psi.pointer.Identikit;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class MarkerCache {
    static final Comparator<SelfElementInfo> INFO_COMPARATOR = (info1, info2) -> {
        int o1 = info1.getPsiStartOffset();
        int o2 = info2.getPsiStartOffset();
        if (o1 < 0 || o2 < 0) {
            return o1 >= 0 ? -1 : o2 >= 0 ? 1 : 0; // infos without range go after infos with range
        }
        if (o1 != o2) {
            return o1 > o2 ? 1 : -1;
        }

        o1 = info1.getPsiEndOffset();
        o2 = info2.getPsiEndOffset();
        if (o1 != o2) {
            return o1 > o2 ? 1 : -1;
        }

        return (info1.isGreedy() ? 1 : 0) - (info2.isGreedy() ? 1 : 0);
    };
    private final SmartPointerTracker myPointers;
    private UpdatedRanges myUpdatedRanges;

    MarkerCache(@Nonnull SmartPointerTracker pointers) {
        myPointers = pointers;
    }

    private @Nonnull UpdatedRanges getUpdatedMarkers(@Nonnull FrozenDocument frozen, @Nonnull List<? extends DocumentEvent> events) {
        int eventCount = events.size();
        assert eventCount > 0;

        UpdatedRanges cache = myUpdatedRanges;
        if (cache != null && cache.myEventCount == eventCount) {
            return cache;
        }

        UpdatedRanges answer;
        if (cache != null && cache.myEventCount < eventCount) {
            // apply only the new events
            answer = applyEvents(events.subList(cache.myEventCount, eventCount), cache);
        }
        else {
            List<SelfElementInfo> infos = myPointers.getSortedInfos();
            ManualRangeMarker[] markers = createMarkers(infos);
            answer = applyEvents(events, new UpdatedRanges(0, frozen, infos, markers));
        }

        myUpdatedRanges = answer;
        return answer;
    }

    @Nonnull
    private static ManualRangeMarker[] createMarkers(@Nonnull List<? extends SelfElementInfo> infos) {
        ManualRangeMarker[] markers = new ManualRangeMarker[infos.size()];
        int i = 0;
        while (i < markers.length) {
            SelfElementInfo info = infos.get(i);
            boolean greedy = info.isGreedy();
            int start = info.getPsiStartOffset();
            int end = info.getPsiEndOffset();
            markers[i] = new ManualRangeMarker(start, end, greedy, greedy, !greedy, null);

            i++;
            while (i < markers.length && rangeEquals(infos.get(i), start, end, greedy)) {
                markers[i] = markers[i - 1];
                i++;
            }
        }
        return markers;
    }

    private static boolean rangeEquals(@Nonnull SelfElementInfo info, int start, int end, boolean greedy) {
        return start == info.getPsiStartOffset() && end == info.getPsiEndOffset() && greedy == info.isGreedy();
    }

    private static @Nonnull UpdatedRanges applyEvents(@Nonnull List<? extends DocumentEvent> events, @Nonnull UpdatedRanges struct) {
        FrozenDocument frozen = struct.myResultDocument;
        ManualRangeMarker[] resultMarkers = struct.myMarkers.clone();
        for (DocumentEvent event : events) {
            FrozenDocument before = frozen;
            frozen = frozen.applyEvent(event, 0);
            DocumentEvent corrected = new DocumentEventImpl(frozen, event.getOffset(), event.getOldFragment(), event.getNewFragment(),
                event.getOldTimeStamp(), event.isWholeTextReplaced(),
                ((DocumentEventImpl) event).getInitialStartOffset(),
                ((DocumentEventImpl) event).getInitialOldLength(),
                event.getMoveOffset());

            int i = 0;
            while (i < resultMarkers.length) {
                ManualRangeMarker currentRange = resultMarkers[i];

                int sameMarkersEnd = i + 1;
                while (sameMarkersEnd < resultMarkers.length && resultMarkers[sameMarkersEnd] == currentRange) {
                    sameMarkersEnd++;
                }

                ManualRangeMarker updatedRange = currentRange == null ? null : currentRange.getUpdatedRange(corrected, before);
                while (i < sameMarkersEnd) {
                    resultMarkers[i] = updatedRange;
                    i++;
                }
            }
        }
        return new UpdatedRanges(struct.myEventCount + events.size(), frozen, struct.mySortedInfos, resultMarkers);
    }

    boolean updateMarkers(@Nonnull FrozenDocument frozen, @Nonnull List<? extends DocumentEvent> events) {
        UpdatedRanges updated = getUpdatedMarkers(frozen, events);

        boolean sorted = true;
        for (int i = 0; i < updated.myMarkers.length; i++) {
            SelfElementInfo info = updated.mySortedInfos.get(i);
            info.setRange(updated.myMarkers[i]);
            if (sorted && i > 0 && INFO_COMPARATOR.compare(updated.mySortedInfos.get(i - 1), info) > 0) {
                sorted = false;
            }
        }

        myUpdatedRanges = null;
        return sorted;
    }

    @Nullable
    TextRange getUpdatedRange(@Nonnull SelfElementInfo info, @Nonnull FrozenDocument frozen, @Nonnull List<? extends DocumentEvent> events) {
        UpdatedRanges struct = getUpdatedMarkers(frozen, events);
        int i = Collections.binarySearch(struct.mySortedInfos, info, INFO_COMPARATOR);
        ManualRangeMarker updated = i >= 0 ? struct.myMarkers[i] : null;
        return updated == null ? null : new UnfairTextRange(updated.getStartOffset(), updated.getEndOffset());
    }

    static @Nullable Segment getUpdatedRange(@Nonnull PsiFile containingFile,
                                             @Nonnull Segment segment,
                                             boolean isSegmentGreedy,
                                             @Nonnull FrozenDocument frozen,
                                             @Nonnull List<? extends DocumentEvent> events) {
        SelfElementInfo info = new SelfElementInfo(ProperTextRange.create(segment), new Identikit() {
            @Override
            public @Nullable PsiElement findPsiElement(@Nonnull PsiFile file, int startOffset, int endOffset) {
                return null;
            }

            @Override
            public @Nonnull Language getFileLanguage() {
                throw new IllegalStateException();
            }

            @Override
            public boolean isForPsiFile() {
                return false;
            }
        }, containingFile, isSegmentGreedy);
        List<SelfElementInfo> infos = Collections.singletonList(info);

        boolean greedy = info.isGreedy();
        int start = info.getPsiStartOffset();
        int end = info.getPsiEndOffset();
        boolean surviveOnExternalChange = events.stream().anyMatch(event -> isWholeDocumentReplace(frozen, (DocumentEventImpl) event));
        ManualRangeMarker marker = new ManualRangeMarker(start, end, greedy, greedy, surviveOnExternalChange, null);

        UpdatedRanges ranges = new UpdatedRanges(0, frozen, infos, new ManualRangeMarker[]{marker});
        // NB: convert events from completion to whole doc change event to more precise translation
        List<DocumentEvent> newEvents =
            ContainerUtil.map(events, event -> isWholeDocumentReplace(frozen, (DocumentEventImpl) event)
                ? new DocumentEventImpl(event.getDocument(), event.getOffset(), event.getOldFragment(),
                event.getNewFragment(), event.getOldTimeStamp(), true,
                ((DocumentEventImpl) event).getInitialStartOffset(),
                ((DocumentEventImpl) event).getInitialOldLength(),
                event.getMoveOffset()) : event);
        UpdatedRanges updated = applyEvents(newEvents, ranges);
        return updated.myMarkers[0];
    }

    private static boolean isWholeDocumentReplace(@Nonnull FrozenDocument frozen, @Nonnull DocumentEventImpl event) {
        return event.getInitialStartOffset() == 0 && event.getInitialOldLength() == frozen.getTextLength();
    }

    void rangeChanged() {
        myUpdatedRanges = null;
    }

    private static class UpdatedRanges {
        private final int myEventCount;
        private final FrozenDocument myResultDocument;
        private final List<SelfElementInfo> mySortedInfos;
        private final ManualRangeMarker[] myMarkers;

        UpdatedRanges(int eventCount,
                      @Nonnull FrozenDocument resultDocument,
                      @Nonnull List<SelfElementInfo> sortedInfos,
                      @Nonnull ManualRangeMarker[] markers) {
            myEventCount = eventCount;
            myResultDocument = resultDocument;
            mySortedInfos = sortedInfos;
            myMarkers = markers;
        }
    }
}
