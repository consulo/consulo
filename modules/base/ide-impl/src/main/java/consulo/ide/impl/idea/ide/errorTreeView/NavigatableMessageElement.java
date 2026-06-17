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
package consulo.ide.impl.idea.ide.errorTreeView;

import consulo.annotation.DeprecationInfo;
import consulo.navigation.Navigable;
import consulo.ui.ex.errorTreeView.ErrorTreeElementKind;
import org.jspecify.annotations.Nullable;

/**
 * @author Eugene Zhuravlev
 * @since 2004-11-12
 */
@Deprecated
@DeprecationInfo("Use NavigableMessageElement, typo-corrected name")
@SuppressWarnings({"SpellCheckingInspection", "deprecation"})
public class NavigatableMessageElement extends NavigableMessageElement {
    public NavigatableMessageElement(
        ErrorTreeElementKind kind,
        @Nullable GroupingElement parent,
        String[] message,
        Navigable navigable,
        String exportText,
        String rendererTextPrefix
    ) {
        super(kind, parent, message, navigable, exportText, rendererTextPrefix);
    }
}
