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
import consulo.navigation.Navigatable;
import consulo.ui.ex.errorTreeView.ErrorTreeElementKind;

import org.jspecify.annotations.Nullable;

/**
 * @author Eugene Zhuravlev
 * @since 2004-11-12
 */
public class NavigableMessageElement extends ErrorTreeElement {
    private final GroupingElement myParent;
    private final String[] myMessage;
    private final Navigable myNavigable;
    private final String myExportText;
    private final String myRendererTextPrefix;

    public NavigableMessageElement(
        ErrorTreeElementKind kind,
        @Nullable GroupingElement parent,
        String[] message,
        Navigable navigable,
        String exportText,
        String rendererTextPrefix
    ) {
        super(kind);
        myParent = parent;
        myMessage = message;
        myNavigable = navigable;
        myExportText = exportText;
        myRendererTextPrefix = rendererTextPrefix;
    }

    public Navigable getNavigable() {
        return myNavigable;
    }

    @Deprecated
    @DeprecationInfo("Use #getNavigable(), typo-corrected name")
    @SuppressWarnings({"SpellCheckingInspection", "deprecation"})
    public Navigatable getNavigatable() {
        return (Navigatable) myNavigable;
    }

    @Override
    public String[] getText() {
        return myMessage;
    }

    @Override
    public Object getData() {
        return myParent.getData();
    }

    public @Nullable GroupingElement getParent() {
        return myParent;
    }

    @Override
    public String getExportTextPrefix() {
        return getKind().getPresentableText() + myExportText;
    }

    public String getRendererTextPrefix() {
        return myRendererTextPrefix;
    }
}
