/*
 * Copyright 2013-2016 must-be.org
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
package consulo.web.gwt.client.text;

import consulo.web.gwt.client.transport.GwtColor;
import consulo.web.gwt.client.transport.GwtHighlightInfo;
import consulo.web.gwt.client.transport.GwtTextRange;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 17-May-16
 */
public class SegmentBuilder {
  private List<Segment> mySegments = new ArrayList<Segment>();
  private String myText;

  public SegmentBuilder(String text) {
    myText = text;

    int otherSimpleOffset = -1;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);

      switch (c) {
        case '&':
        case '<':
        case '>':
        case '\"':
        case ' ':
        case '\t':
          if (otherSimpleOffset != -1) {
            mySegments.add(new SimpleSegment(new GwtTextRange(otherSimpleOffset, i), text.substring(otherSimpleOffset, i)));
            otherSimpleOffset = -1;
          }

          mySegments.add(new ReplaceSegment(new GwtTextRange(i, i + 1), mapChar(c)));
          break;
        default:
          if (otherSimpleOffset == -1) {
            otherSimpleOffset = i;
          }
          break;
      }
    }

    if (otherSimpleOffset != -1) {
      mySegments.add(new SimpleSegment(new GwtTextRange(otherSimpleOffset, text.length()), text.substring(otherSimpleOffset, text.length())));
    }
  }

  public void addHighlight(GwtHighlightInfo highlightInfo, int flag) {
    boolean foundInsideSegment = false;
    GwtTextRange highlightTextRange = highlightInfo.getTextRange();
    for (int i = 0; i < mySegments.size(); i++) {
      Segment segment = mySegments.get(i);

      if (segment.getTextRange().containsRange(highlightTextRange)) {
        insertHighlightInsideSegment(highlightInfo, i, segment, highlightTextRange, flag);
        foundInsideSegment = true;
        break;
      }
    }

    if (!foundInsideSegment) {
      for (int i = 0; i < mySegments.size(); i++) {
        Segment segment = mySegments.get(i);

        GwtTextRange textRange = highlightInfo.getTextRange();
        if(textRange.containsRange(segment.getTextRange())) {
          WrappedStyledSegment wrappedStyledSegment = new WrappedStyledSegment(segment);
          add(wrappedStyledSegment, highlightInfo, flag);
          copy(segment, wrappedStyledSegment);
          mySegments.set(i, wrappedStyledSegment);
        }
      }
    }
  }

  private void insertHighlightInsideSegment(GwtHighlightInfo highlightInfo, int index, Segment segment, GwtTextRange highlightTextRange, int flag) {
    // if segment equal highlight info
    GwtTextRange segmentTextRange = segment.getTextRange();
    if (segmentTextRange.equals(highlightTextRange)) {
      // replace base with styled segment
      if (!(segment instanceof OriginalStyledSegment)) {
        segment = new OriginalStyledSegment(myText, segment.getTextRange());
        mySegments.set(index, segment);
      }

      add((OriginalStyledSegment)segment, highlightInfo, flag);
    }
    else {
      int start = highlightTextRange.getStartOffset() - segmentTextRange.getStartOffset();
      if (start > 0) {
        final int startOffset = segmentTextRange.getStartOffset();
        final int endOffset = highlightTextRange.getStartOffset();
        mySegments.add(index++, new SimpleSegment(new GwtTextRange(startOffset, endOffset), myText.substring(startOffset, endOffset)));
      }

      OriginalStyledSegment element = new OriginalStyledSegment(myText, highlightTextRange);
      copy(segment, element);

      add(element, highlightInfo, flag);

      mySegments.set(index++, element);

      int end = segmentTextRange.getEndOffset() - highlightTextRange.getEndOffset();
      if (end > 0) {
        final int startOffset = highlightTextRange.getEndOffset();
        final int endOffset = segmentTextRange.getEndOffset();
        mySegments.add(index, new SimpleSegment(new GwtTextRange(startOffset, endOffset), myText.substring(startOffset, endOffset)));
      }
    }
  }

  private static void copy(Segment from, Segment to) {
    if(from instanceof StyledSegment && to instanceof StyledSegment) {
      ((StyledSegment)from).copy((StyledSegment)to);
    }
  }

  private void add(StyledSegment styledSegment, GwtHighlightInfo highlightInfo, int flag) {
    GwtColor foreground = highlightInfo.getForeground();
    if (foreground != null) {
      styledSegment.add("color", "rgb(" + foreground.getRed() + ", " + foreground.getGreen() + ", " + foreground.getBlue() + ");", flag);
    }

    GwtColor background = highlightInfo.getBackground();
    if (background != null) {
      styledSegment.add("background-color", "rgb(" + background.getRed() + ", " + background.getGreen() + ", " + background.getBlue() + ");", flag);
    }

    if (highlightInfo.isBold()) {
      styledSegment.add("font-weight", "bold;", flag);
    }

    if (highlightInfo.isItalic()) {
      styledSegment.add("font-style", "italic;", flag);
    }
  }

  private String mapChar(char c) {
    switch (c) {
      case '&':
        return "&amp;";
      case '<':
        return "&lt;";
      case '>':
        return "&gt;";
      case '\"':
        return "&quot;";
      case '\t':
        return "&emsp;";
      case ' ':
        return "&nbsp;";
    }
    return String.valueOf(c);
  }
  public String toHtmlAsText() {
    StringBuilder builder = new StringBuilder();
    for (Segment segment : mySegments) {
      builder.append(segment.getText());
    }
    return builder.toString();
  }

  public void removeHighlightByFlag(int flag) {
    for (Segment segment : mySegments) {
      if(segment instanceof StyledSegment) {
        ((StyledSegment)segment).removeByFlag(flag);
      }
    }
  }
}
