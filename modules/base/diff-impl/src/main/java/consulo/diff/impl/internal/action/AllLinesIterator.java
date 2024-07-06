/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.diff.impl.internal.action;

import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.document.Document;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;

import java.util.Iterator;

public class AllLinesIterator implements Iterator<Pair<Integer, CharSequence>> {
  @Nonnull
  private final Document myDocument;
  private int myLine = 0;

  public AllLinesIterator(@Nonnull Document document) {
    myDocument = document;
  }

  @Override
  public boolean hasNext() {
    return myLine < DiffImplUtil.getLineCount(myDocument);
  }

  @Override
  public Pair<Integer, CharSequence> next() {
    int offset1 = myDocument.getLineStartOffset(myLine);
    int offset2 = myDocument.getLineEndOffset(myLine);

    CharSequence text = myDocument.getImmutableCharSequence().subSequence(offset1, offset2);

    Pair<Integer, CharSequence> pair = new Pair<>(myLine, text);
    myLine++;

    return pair;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
