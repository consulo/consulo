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
package consulo.document.event;

import consulo.document.Document;
import jakarta.annotation.Nonnull;

import java.util.EventObject;

public abstract class DocumentEvent extends EventObject {
  protected DocumentEvent(@Nonnull Document document) {
    super(document);
  }

  @Nonnull
  public abstract Document getDocument();

  public abstract int getOffset();

  public abstract int getOldLength();

  public abstract int getNewLength();

  @Nonnull
  public abstract CharSequence getOldFragment();

  @Nonnull
  public abstract CharSequence getNewFragment();

  public abstract long getOldTimeStamp();

  public boolean isWholeTextReplaced() {
    return getOffset() == 0 && getNewLength() == getDocument().getTextLength();
  }
}
