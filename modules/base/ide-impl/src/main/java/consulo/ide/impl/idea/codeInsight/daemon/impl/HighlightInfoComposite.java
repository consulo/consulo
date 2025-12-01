// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.internal.intention.IntentionActionDescriptor;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.lang.Pair;
import consulo.util.lang.xml.CommonXmlStrings;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

class HighlightInfoComposite extends HighlightInfoImpl {
    private static final LocalizeValue HTML_START = LocalizeValue.of(CommonXmlStrings.HTML_START);
    private static final LocalizeValue LINE_BREAK = LocalizeValue.of("<hr size=1 noshade>");
    private static final LocalizeValue HTML_END = LocalizeValue.of(CommonXmlStrings.HTML_END);

    static HighlightInfoComposite create(@Nonnull List<? extends HighlightInfoImpl> infos) {
        // derive composite's offsets from an info with tooltip, if present
        HighlightInfoImpl anchorInfo =
            ContainerUtil.find(infos, info -> info.getToolTip() != LocalizeValue.empty());
        if (anchorInfo == null) {
            anchorInfo = infos.get(0);
        }
        return new HighlightInfoComposite(infos, anchorInfo);
    }

    private HighlightInfoComposite(@Nonnull List<? extends HighlightInfoImpl> infos, @Nonnull HighlightInfoImpl anchorInfo) {
        super(
            null,
            null,
            anchorInfo.getType(),
            anchorInfo.getStartOffset(),
            anchorInfo.getEndOffset(),
            createCompositeDescription(infos),
            createCompositeTooltip(infos),
            anchorInfo.getType().getSeverity(null),
            false,
            null,
            false,
            0,
            anchorInfo.getProblemGroup(),
            null,
            anchorInfo.getGutterIconRenderer(),
            anchorInfo.getGroup()
        );
        myHighlighter = anchorInfo.getHighlighter();
        setGroup(anchorInfo.getGroup());
        List<Pair<IntentionActionDescriptor, RangeMarker>> markers = List.of();
        List<Pair<IntentionActionDescriptor, TextRange>> ranges = List.of();
        for (HighlightInfoImpl info : infos) {
            if (info.myQuickFixActionMarkers != null) {
                if (markers == List.<Pair<IntentionActionDescriptor, RangeMarker>>of()) {
                    markers = new ArrayList<>();
                }
                markers.addAll(info.myQuickFixActionMarkers);
            }
            if (info.myQuickFixActionRanges != null) {
                if (ranges == List.<Pair<IntentionActionDescriptor, TextRange>>of()) {
                    ranges = new ArrayList<>();
                }
                ranges.addAll(info.myQuickFixActionRanges);
            }
        }
        myQuickFixActionMarkers = Lists.newLockFreeCopyOnWriteList(markers);
        myQuickFixActionRanges = Lists.newLockFreeCopyOnWriteList(ranges);
    }

    @Nonnull
    private static LocalizeValue createCompositeDescription(List<? extends HighlightInfoImpl> infos) {
        List<LocalizeValue> result = new ArrayList<>();
        for (HighlightInfoImpl info : infos) {
            LocalizeValue itemDescription = info.getDescription();
            if (itemDescription == LocalizeValue.empty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.add(LocalizeValue.space());
            }
            result.add(itemDescription.map((localizeManager, s) -> {
                s = s.trim();
                return !s.endsWith(".") ? s + '.' : s;
            }));
        }
        return result.isEmpty() ? LocalizeValue.empty() : LocalizeValue.join(result.toArray(LocalizeValue[]::new));
    }

    @Nonnull
    private static LocalizeValue createCompositeTooltip(@Nonnull List<? extends HighlightInfoImpl> infos) {
        List<LocalizeValue> result = new ArrayList<>(infos.size() * 2 + 1);
        result.add(HTML_START);
        boolean empty = true;
        for (HighlightInfoImpl info : infos) {
            LocalizeValue tooltip = info.getToolTip();
            if (tooltip == LocalizeValue.empty()) {
                continue;
            }
            if (!empty) {
                result.add(LINE_BREAK);
            }
            empty = false;
            result.add(tooltip.map((localizeManager, s) -> XmlStringUtil.stripHtml(s)));
        }
        result.add(HTML_END);
        if (empty) {
            return LocalizeValue.empty();
        }
        return LocalizeValue.join(result.toArray(LocalizeValue[]::new));
    }
}
