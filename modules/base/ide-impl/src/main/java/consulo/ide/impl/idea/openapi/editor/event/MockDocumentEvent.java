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
package consulo.ide.impl.idea.openapi.editor.event;

import consulo.document.Document;
import consulo.document.event.DocumentEvent;

import jakarta.annotation.Nonnull;

public class MockDocumentEvent extends DocumentEvent {
  private final int myOffset;
  private final long myTimestamp;

  public MockDocumentEvent(@Nonnull Document document, int offset) {
    super(document);
    myOffset = offset;
    myTimestamp = document.getModificationStamp();
  }

  @Nonnull
  public Document getDocument() {
    return (Document)getSource();
  }

  public int getOffset() {
    return myOffset;
  }

  public int getOldLength() {
    return 0;
  }

  public int getNewLength() {
    return 0;
  }

  @Nonnull
  public CharSequence getOldFragment() {
    return "";
  }

  @Nonnull
  public CharSequence getNewFragment() {
    return "";
  }

  public long getOldTimeStamp() {
    return myTimestamp;
  }
}
