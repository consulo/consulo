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
import consulo.language.editor.gutter.LineMarkerInfo;

/**
 * One gutter marker a test says it expects, written as {@code <lineMarker descr="tooltip">code</lineMarker>}.
 * Only the range and the tooltip are compared - the icon is an {@code Image} with no textual form to assert on,
 * so an {@code icon} attribute is rejected rather than quietly ignored.
 *
 * @author VISTALL
 */
public final class ExpectedLineMarker {
    private final int myStartOffset;
    private final int myEndOffset;
    private final String myDescription;

    ExpectedLineMarker(int startOffset, int endOffset, String description) {
        myStartOffset = startOffset;
        myEndOffset = endOffset;
        myDescription = description;
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

    @RequiredReadAction
    public boolean matches(LineMarkerInfo<?> marker) {
        return myStartOffset == marker.startOffset
            && myEndOffset == marker.endOffset
            && DescriptionMatch.matches(myDescription, marker.getLineMarkerTooltip());
    }

    @Override
    public String toString() {
        return "<lineMarker descr=\"" + myDescription + "\"> at " + myStartOffset + ".." + myEndOffset;
    }
}
