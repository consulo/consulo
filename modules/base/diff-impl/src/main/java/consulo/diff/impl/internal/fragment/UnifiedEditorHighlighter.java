/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.impl.internal.fragment;

import consulo.application.ApplicationManager;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.HighlighterClient;
import consulo.codeEditor.HighlighterIterator;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.util.TextRange;
import consulo.language.ast.IElementType;
import consulo.logging.Logger;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class UnifiedEditorHighlighter implements EditorHighlighter {
  public static final Logger LOG = Logger.getInstance(UnifiedEditorHighlighter.class);

  @Nonnull
  private final Document myDocument;
  @Nonnull
  private final List<Element> myPieces;

  public UnifiedEditorHighlighter(@Nonnull Document document,
                                  @Nonnull EditorHighlighter highlighter1,
                                  @Nonnull EditorHighlighter highlighter2,
                                  @Nonnull List<HighlightRange> ranges,
                                  int textLength) {
    myDocument = document;
    myPieces = new ArrayList<Element>();
    init(highlighter1.createIterator(0), highlighter2.createIterator(0), ranges, textLength);
  }

  private void init(@Nonnull HighlighterIterator it1,
                    @Nonnull HighlighterIterator it2,
                    @Nonnull List<HighlightRange> ranges,
                    int textLength) {
    ApplicationManager.getApplication().assertReadAccessAllowed();

    int offset = 0;

    for (HighlightRange range : ranges) {
      TextRange base = range.getBase();
      TextRange changed = range.getChanged();

      if (base.isEmpty()) continue;

      if (base.getStartOffset() > offset) {
        addElement(new Element(offset, base.getStartOffset(), null, TextAttributes.ERASE_MARKER));
        offset = base.getStartOffset();
      }

      HighlighterIterator it = range.getSide().select(it1, it2);
      while (!it.atEnd() && changed.getStartOffset() >= it.getEnd()) {
        it.advance();
      }

      if (it.atEnd()) {
        LOG.error("Unexpected end of highlighter");
        break;
      }

      if (changed.getEndOffset() <= it.getStart()) {
        continue;
      }

      while (true) {
        int relativeStart = Math.max(it.getStart() - changed.getStartOffset(), 0);
        int relativeEnd = Math.min(it.getEnd() - changed.getStartOffset(), changed.getLength() + 1);

        addElement(new Element(offset + relativeStart, offset + relativeEnd, (IElementType)it.getTokenType(), it.getTextAttributes()));

        if (changed.getEndOffset() <= it.getEnd()) {
          offset += changed.getLength();
          break;
        }

        it.advance();
        if (it.atEnd()) {
          LOG.error("Unexpected end of highlighter");
          break;
        }
      }
    }

    if (offset < textLength) {
      addElement(new Element(offset, textLength, null, TextAttributes.ERASE_MARKER));
    }
  }

  private void addElement(@Nonnull Element element) {
    boolean merged = false;
    if (!myPieces.isEmpty()) {
      Element oldElement = myPieces.get(myPieces.size() - 1);
      if (oldElement.getEnd() >= element.getStart() && Comparing.equal(oldElement.getAttributes(),
                                                                       element.getAttributes()) && Comparing.equal(oldElement.getElementType(),
                                                                                                                   element.getElementType())) {
        merged = true;
        myPieces.remove(myPieces.size() - 1);
        myPieces.add(new Element(oldElement.getStart(), element.getEnd(), element.getElementType(), element.getAttributes()));
      }
    }
    if (!merged) {
      myPieces.add(element);
    }
  }

  @Nonnull
  @Override
  public HighlighterIterator createIterator(int startOffset) {
    int index = Collections.binarySearch(myPieces, new Element(startOffset, 0, null, null), new Comparator<Element>() {
      @Override
      public int compare(Element o1, Element o2) {
        return o1.getStart() - o2.getStart();
      }
    });
    // index: (-insertion point - 1), where insertionPoint is the index of the first element greater than the key
    // and we need index of the first element that is less or equal (floorElement)
    if (index < 0) index = Math.max(-index - 2, 0);
    return new ProxyIterator(myDocument, index, myPieces);
  }

  @Override
  public void setColorScheme(@Nonnull EditorColorsScheme scheme) {
  }

  @Override
  public void setEditor(@Nonnull HighlighterClient editor) {
  }

  @Override
  public void setText(@Nonnull CharSequence text) {
  }

  @Override
  public void beforeDocumentChange(DocumentEvent event) {
  }

  @Override
  public void documentChanged(DocumentEvent event) {
  }

  private static class ProxyIterator implements HighlighterIterator {
    private final Document myDocument;
    private int myIdx;
    private final List<Element> myPieces;

    private ProxyIterator(@Nonnull Document document, int idx, @Nonnull List<Element> pieces) {
      myDocument = document;
      myIdx = idx;
      myPieces = pieces;
    }

    @Override
    public TextAttributes getTextAttributes() {
      return myPieces.get(myIdx).getAttributes();
    }

    @Override
    public int getStart() {
      return myPieces.get(myIdx).getStart();
    }

    @Override
    public int getEnd() {
      return myPieces.get(myIdx).getEnd();
    }

    @Override
    public IElementType getTokenType() {
      return myPieces.get(myIdx).myElementType;
    }

    @Override
    public void advance() {
      if (myIdx < myPieces.size()) {
        myIdx++;
      }
    }

    @Override
    public void retreat() {
      if (myIdx > -1) {
        myIdx--;
      }
    }

    @Override
    public boolean atEnd() {
      return myIdx < 0 || myIdx >= myPieces.size();
    }

    @Override
    public Document getDocument() {
      return myDocument;
    }
  }

  private static class Element {
    private final int myStart;
    private final int myEnd;
    private final IElementType myElementType;
    private final TextAttributes myAttributes;

    private Element(int start, int end, IElementType elementType, TextAttributes attributes) {
      myStart = start;
      myEnd = end;
      myElementType = elementType;
      myAttributes = attributes;
    }

    public int getStart() {
      return myStart;
    }

    public int getEnd() {
      return myEnd;
    }

    public IElementType getElementType() {
      return myElementType;
    }

    public TextAttributes getAttributes() {
      return myAttributes;
    }
  }
}
