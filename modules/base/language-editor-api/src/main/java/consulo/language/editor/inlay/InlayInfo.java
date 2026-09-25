/*
 * Copyright 2013-2017 consulo.io
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.language.editor.inlay;

import org.jspecify.annotations.Nullable;

/**
 * Represents a single inlay hint instance.
 */
public class InlayInfo {
    private final String text;
    private final int offset;
    private final boolean isShowOnlyIfExistedBefore;
    private final boolean isFilterByExcludeList;
    private final boolean relatesToPrecedingText;
    private final HintWidthAdjustment widthAdjustment;

    public InlayInfo(
        String text,
        int offset,
        boolean isShowOnlyIfExistedBefore,
        boolean isFilterByExcludeList,
        boolean relatesToPrecedingText,
        HintWidthAdjustment widthAdjustment
    ) {
        this.text = text;
        this.offset = offset;
        this.isShowOnlyIfExistedBefore = isShowOnlyIfExistedBefore;
        this.isFilterByExcludeList = isFilterByExcludeList;
        this.relatesToPrecedingText = relatesToPrecedingText;
        this.widthAdjustment = widthAdjustment;
    }

    public InlayInfo(
        String text,
        int offset,
        boolean isShowOnlyIfExistedBefore,
        boolean isFilterByExcludeList,
        boolean relatesToPrecedingText
    ) {
        this(text, offset, isShowOnlyIfExistedBefore, isFilterByExcludeList, relatesToPrecedingText, null);
    }

    public InlayInfo(String text, int offset, boolean isShowOnlyIfExistedBefore) {
        this(text, offset, isShowOnlyIfExistedBefore, true, false, null);
    }

    public InlayInfo(String text, int offset) {
        this(text, offset, false, true, false, null);
    }

    public String getText() {
        return text;
    }

    public int getOffset() {
        return offset;
    }

    public boolean isShowOnlyIfExistedBefore() {
        return isShowOnlyIfExistedBefore;
    }

    public boolean isFilterByExcludeList() {
        return isFilterByExcludeList;
    }

    public boolean isRelatesToPrecedingText() {
        return relatesToPrecedingText;
    }

    public HintWidthAdjustment getWidthAdjustment() {
        return widthAdjustment;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        InlayInfo that = (InlayInfo) obj;
        return offset == that.offset
            && text.equals(that.text);
    }

    @Override
    public int hashCode() {
        return 31 * text.hashCode() + offset;
    }
}