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

import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.rawHighlight.HighlightInfo;

/**
 * One highlight a test says it expects, as written in the marked up text.
 *
 * @author VISTALL
 */
public final class ExpectedHighlight {
    /**
     * A description that matches whatever the highlight actually says, written as {@code descr="*"} or by
     * leaving {@code descr} out.
     */
    public static final String ANY_TEXT = "*";

    private final String myTag;
    private final HighlightSeverity mySeverity;
    private final boolean myAfterEndOfLine;
    private final int myStartOffset;
    private final int myEndOffset;
    private final String myDescription;

    ExpectedHighlight(
        String tag,
        HighlightSeverity severity,
        boolean afterEndOfLine,
        int startOffset,
        int endOffset,
        String description
    ) {
        myTag = tag;
        mySeverity = severity;
        myAfterEndOfLine = afterEndOfLine;
        myStartOffset = startOffset;
        myEndOffset = endOffset;
        myDescription = description;
    }

    public String getTag() {
        return myTag;
    }

    public HighlightSeverity getSeverity() {
        return mySeverity;
    }

    public int getStartOffset() {
        return myStartOffset;
    }

    public int getEndOffset() {
        return myEndOffset;
    }

    public String getDescription() {
        return myDescription;
    }

    public boolean matches(HighlightInfo info) {
        return mySeverity.equals(info.getSeverity())
            && myStartOffset == info.getStartOffset()
            && myEndOffset == info.getEndOffset()
            && myAfterEndOfLine == info.isAfterEndOfLine()
            && DescriptionMatch.matches(myDescription, info.getDescription() == null ? null : info.getDescription().get());
    }

    @Override
    public String toString() {
        return "<" + myTag + " descr=\"" + myDescription + "\"> at " + myStartOffset + ".." + myEndOffset;
    }
}
