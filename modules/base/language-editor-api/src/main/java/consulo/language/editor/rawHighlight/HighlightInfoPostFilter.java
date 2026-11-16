// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.rawHighlight;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

/**
 * Post filter for highlight infos which is used in BackgroundUpdateHighlightersUtil
 * to filter out not relevant infos
 *
 * @see consulo.language.editor.highlight.BackgroundUpdateHighlightersUtil
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface HighlightInfoPostFilter {
    /**
     * Return true if highlighting should be added to markup
     */
    boolean accept(HighlightInfo highlightInfo);
}
