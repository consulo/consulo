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

import consulo.ide.impl.idea.ui.CustomizeColoredTreeCellRenderer;
import consulo.ui.ex.errorTreeView.ErrorTreeElementKind;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Eugene Zhuravlev
 * @since 2004-11-12
 */
public abstract class ErrorTreeElement {
  public static final ErrorTreeElement[] EMPTY_ARRAY = new ErrorTreeElement[0];
  
  private final ErrorTreeElementKind myKind;

  protected ErrorTreeElement() {
    this(ErrorTreeElementKind.GENERIC);
  }

  protected ErrorTreeElement(@Nonnull ErrorTreeElementKind kind) {
    myKind = kind;
  }

  @Nonnull
  public ErrorTreeElementKind getKind() {
    return myKind;
  }

  public abstract String[] getText();

  public abstract Object getData();

  public final String toString() {
    String[] text = getText();
    return text != null && text.length > 0? text[0] : "";
  }

  public abstract String getExportTextPrefix();

  @Nullable
  public CustomizeColoredTreeCellRenderer getLeftSelfRenderer() {
    return null;
  }

  @Nullable
  public CustomizeColoredTreeCellRenderer getRightSelfRenderer() {
    return null;
  }
}
