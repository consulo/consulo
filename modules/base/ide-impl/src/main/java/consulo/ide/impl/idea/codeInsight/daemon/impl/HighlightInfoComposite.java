// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.xml.util.XmlStringUtil;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.internal.intention.IntentionActionDescriptor;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

class HighlightInfoComposite extends HighlightInfoImpl {
  private static final String LINE_BREAK = "<hr size=1 noshade>";

  static HighlightInfoComposite create(@Nonnull List<? extends HighlightInfoImpl> infos) {
    // derive composite's offsets from an info with tooltip, if present
    HighlightInfoImpl anchorInfo = ContainerUtil.find(infos, info -> info.getToolTip() != null);
    if (anchorInfo == null) anchorInfo = infos.get(0);
    return new HighlightInfoComposite(infos, anchorInfo);
  }

  private HighlightInfoComposite(@Nonnull List<? extends HighlightInfoImpl> infos, @Nonnull HighlightInfoImpl anchorInfo) {
    super(null, null, anchorInfo.type, anchorInfo.startOffset, anchorInfo.endOffset, createCompositeDescription(infos), createCompositeTooltip(infos), anchorInfo.type.getSeverity(null), false, null,
          false, 0, anchorInfo.getProblemGroup(), null, anchorInfo.getGutterIconRenderer(), anchorInfo.getGroup());
    highlighter = anchorInfo.getHighlighter();
    setGroup(anchorInfo.getGroup());
    List<Pair<IntentionActionDescriptor, RangeMarker>> markers = List.of();
    List<Pair<IntentionActionDescriptor, TextRange>> ranges = List.of();
    for (HighlightInfoImpl info : infos) {
      if (info.quickFixActionMarkers != null) {
        if (markers == List.<Pair<IntentionActionDescriptor, RangeMarker>>of()) markers = new ArrayList<>();
        markers.addAll(info.quickFixActionMarkers);
      }
      if (info.quickFixActionRanges != null) {
        if (ranges == List.<Pair<IntentionActionDescriptor, TextRange>>of()) ranges = new ArrayList<>();
        ranges.addAll(info.quickFixActionRanges);
      }
    }
    quickFixActionMarkers = Lists.newLockFreeCopyOnWriteList(markers);
    quickFixActionRanges = Lists.newLockFreeCopyOnWriteList(ranges);
  }

  @Nullable
  private static String createCompositeDescription(List<? extends HighlightInfoImpl> infos) {
    StringBuilder description = new StringBuilder();
    boolean isNull = true;
    for (HighlightInfoImpl info : infos) {
      String itemDescription = info.getDescription();
      if (itemDescription != null) {
        itemDescription = itemDescription.trim();
        description.append(itemDescription);
        if (!itemDescription.endsWith(".")) {
          description.append('.');
        }
        description.append(' ');

        isNull = false;
      }
    }
    return isNull ? null : description.toString();
  }

  @Nullable
  private static String createCompositeTooltip(@Nonnull List<? extends HighlightInfoImpl> infos) {
    StringBuilder result = new StringBuilder();
    for (HighlightInfoImpl info : infos) {
      String toolTip = info.getToolTip();
      if (toolTip != null) {
        if (result.length() != 0) {
          result.append(LINE_BREAK);
        }
        toolTip = XmlStringUtil.stripHtml(toolTip);
        result.append(toolTip);
      }
    }
    if (result.length() == 0) {
      return null;
    }
    return XmlStringUtil.wrapInHtml(result);
  }
}
