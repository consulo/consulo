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

import consulo.ui.ex.errorTreeView.ErrorTreeElementKind;

import jakarta.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 * @since 2004-11-12
 */
public class SimpleMessageElement extends ErrorTreeElement{
  private final String[] myMessage;
  private final Object myData;

  public SimpleMessageElement(@Nonnull ErrorTreeElementKind kind, String[] text, Object data) {
    super(kind);
    myMessage = text;
    myData = data;
  }

  public String[] getText() {
    return myMessage;
  }

  public Object getData() {
    return myData;
  }

  public String getExportTextPrefix() {
    return "";
  }
}
