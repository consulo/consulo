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

import consulo.navigation.Navigatable;
import consulo.ui.ex.errorTreeView.ErrorTreeElementKind;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Eugene Zhuravlev
 * @since 2004-11-12
 */
public class NavigatableMessageElement extends ErrorTreeElement {
  private final GroupingElement myParent;
  private final String[] myMessage;
  private final Navigatable myNavigatable;
  private final String myExportText;
  private final String myRendererTextPrefix;

  public NavigatableMessageElement(@Nonnull ErrorTreeElementKind kind,
                                   @Nullable GroupingElement parent,
                                   String[] message,
                                   Navigatable navigatable,
                                   String exportText,
                                   String rendererTextPrefix) {
    super(kind);
    myParent = parent;
    myMessage = message;
    myNavigatable = navigatable;
    myExportText = exportText;
    myRendererTextPrefix = rendererTextPrefix;
  }

  public Navigatable getNavigatable() {
    return myNavigatable;
  }

  public String[] getText() {
    return myMessage;
  }

  public Object getData() {
    return myParent.getData();
  }

  @Nullable
  public GroupingElement getParent() {
    return myParent;
  }

  public String getExportTextPrefix() {
    return getKind().getPresentableText() + myExportText;
  }

  public String getRendererTextPrefix() {
    return myRendererTextPrefix;
  }
}
