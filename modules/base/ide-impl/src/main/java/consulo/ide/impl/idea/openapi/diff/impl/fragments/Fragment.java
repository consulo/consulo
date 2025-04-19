/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.diff.impl.fragments;

import consulo.ide.impl.idea.openapi.diff.impl.highlighting.FragmentSide;
import consulo.ide.impl.idea.openapi.diff.impl.util.TextDiffTypeEnum;
import consulo.document.util.TextRange;
import consulo.annotation.DeprecationInfo;

import java.util.function.Predicate;

@Deprecated(forRemoval = true)
@DeprecationInfo("Old diff impl, must be removed")
public interface Fragment {
    TextDiffTypeEnum getType();

    TextRange getRange(FragmentSide side);

    Fragment shift(TextRange range1, TextRange range2, int startingLine1, int startingLine2);

    void highlight(FragmentHighlighter fragmentHighlighter);

    Fragment getSubfragmentAt(int offset, FragmentSide side, Predicate<Fragment> condition);
}
