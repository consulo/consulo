// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.distributed.ui;

import consulo.ui.TextItemPresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.ui.image.canvas.Canvas2D;
import consulo.ui.style.ComponentColors;
import consulo.versionControlSystem.distributed.branch.DvcsInOutCountersInProject;
import consulo.versionControlSystem.distributed.icon.DistributedVersionControlIconGroup;
import org.jspecify.annotations.Nullable;

/**
 * Shows incoming and outgoing commit counters next to a branch: an arrow icon followed by the coloured number of commits
 * in a branch row, the arrow icons after the branch name in a branch widget.
 */
public final class DvcsIncomingOutgoingUi {
    private static final int COUNTER_HEIGHT = 16;
    private static final int COUNT_GAP = 1;
    private static final int COUNT_FONT_SIZE = 11;
    private static final int COUNT_CHAR_WIDTH = 7;
    private static final int COUNT_TOP = 2;
    private static final String COUNT_FONT_FAMILY = "sans-serif";

    private DvcsIncomingOutgoingUi() {
    }

    public static boolean isIncomingVisible(DvcsInOutCountersInProject state) {
        return !state.isEmpty() && (state.totalIncoming() > 0 || state.hasUnfetched());
    }

    public static boolean isOutgoingVisible(DvcsInOutCountersInProject state) {
        return !state.isEmpty() && state.totalOutgoing() > 0;
    }

    public static String getIncomingText(DvcsInOutCountersInProject state) {
        int totalIncoming = state.totalIncoming();
        return totalIncoming > 0 ? shrinkTo99(totalIncoming) : "";
    }

    public static String getOutgoingText(DvcsInOutCountersInProject state) {
        return shrinkTo99(state.totalOutgoing());
    }

    public static void appendCounters(TextItemPresentation presentation, DvcsInOutCountersInProject state) {
        if (isIncomingVisible(state)) {
            presentation.append("  ");
            presentation.appendIcon(counterImage(DistributedVersionControlIconGroup.incoming(), getIncomingText(state), ComponentColors.VCS_INCOMING_FOREGROUND));
        }
        if (isOutgoingVisible(state)) {
            presentation.append(isIncomingVisible(state) ? " " : "  ");
            presentation.appendIcon(counterImage(DistributedVersionControlIconGroup.outgoing(), getOutgoingText(state), ComponentColors.VCS_OUTGOING_FOREGROUND));
        }
    }

    private static Image counterImage(Image arrow, String count, ColorValue color) {
        if (count.isEmpty()) {
            return arrow;
        }

        int width = arrow.getWidth() + COUNT_GAP + COUNT_CHAR_WIDTH * count.length();
        return ImageEffects.canvas(width, COUNTER_HEIGHT, canvas -> {
            canvas.drawImage(arrow, 0, COUNTER_HEIGHT - arrow.getHeight());

            canvas.setFillStyle(color);
            canvas.setFont(FontManager.get().createFont(COUNT_FONT_FAMILY, COUNT_FONT_SIZE, Font.PLAIN));
            canvas.setTextAlign(Canvas2D.TextAlign.left);
            canvas.setTextBaseline(Canvas2D.TextBaseline.top);
            canvas.fillText(count, arrow.getWidth() + COUNT_GAP, COUNT_TOP);
        });
    }

    /**
     * @return the incoming and outgoing arrows a branch widget shows after the branch name, or {@code null} if the
     * branch is in sync with its remote
     */
    public static @Nullable Image getInAndOutIcons(DvcsInOutCountersInProject state) {
        Image incoming = state.hasIncoming() ? DistributedVersionControlIconGroup.incoming() : null;
        Image outgoing = state.hasOutgoing() ? DistributedVersionControlIconGroup.outgoing() : null;
        if (incoming != null && outgoing != null) {
            return ImageEffects.appendRight(incoming, outgoing);
        }
        return incoming != null ? incoming : outgoing;
    }

    private static String shrinkTo99(int commits) {
        if (commits > 99) {
            return "99+";
        }
        return Integer.toString(commits);
    }
}
