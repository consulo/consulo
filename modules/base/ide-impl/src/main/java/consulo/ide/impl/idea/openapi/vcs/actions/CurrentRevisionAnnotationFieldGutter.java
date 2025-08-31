/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.actions;

import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorKey;
import consulo.ide.impl.idea.openapi.vcs.annotate.TextAnnotationPresentation;
import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.annotate.AnnotationSource;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.annotate.LineAnnotationAspect;
import consulo.versionControlSystem.history.VcsRevisionNumber;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.function.Consumer;

/**
 * shown additionally only when merge
 *
 * @author Konstantin Bulenkov
 */
class CurrentRevisionAnnotationFieldGutter extends AspectAnnotationFieldGutter implements Consumer<AnnotationSource> {
    // merge source showing is turned on
    private boolean myTurnedOn;

    CurrentRevisionAnnotationFieldGutter(
        FileAnnotation annotation,
        LineAnnotationAspect aspect,
        TextAnnotationPresentation highlighting,
        Couple<Map<VcsRevisionNumber, ColorValue>> colorScheme
    ) {
        super(annotation, aspect, highlighting, colorScheme);
    }

    @Override
    public EditorColorKey getColor(int line, Editor editor) {
        return AnnotationSource.LOCAL.getColor();
    }

    @Override
    public String getLineText(int line, Editor editor) {
        String value = myAspect.getValue(line);
        if (String.valueOf(myAnnotation.getLineRevisionNumber(line)).equals(value)) {
            return "";
        }
        // shown in merge sources mode
        return myTurnedOn ? value : "";
    }

    @Nonnull
    @Override
    public LocalizeValue getToolTipValue(int line, Editor editor) {
        LocalizeValue aspectTooltip = myAspect.getTooltipValue(line);
        if (aspectTooltip != LocalizeValue.empty()) {
            return aspectTooltip;
        }
        String text = getLineText(line, editor);
        return StringUtil.isEmpty(text) ? LocalizeValue.empty() : VcsLocalize.annotationOriginalRevisionText(text);
    }

    @Override
    public void accept(AnnotationSource annotationSource) {
        myTurnedOn = annotationSource.showMerged();
    }
}
