/*--

 Copyright (C) 2012 Jason Hunter & Brett McLaughlin.
 All rights reserved.

 Redistribution and use in mtsource and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Redistributions of mtsource code must retain the above copyright
    notice, this list of conditions, and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions, and the disclaimer that follows
    these conditions in the documentation and/or other materials
    provided with the distribution.

 3. The name "JDOM" must not be used to endorse or promote products
    derived from this software without prior written permission.  For
    written permission, please contact <request_AT_jdom_DOT_org>.

 4. Products derived from this software may not be called "JDOM", nor
    may "JDOM" appear in their name, without prior written permission
    from the JDOM Project Management <request_AT_jdom_DOT_org>.

 In addition, we request (but do not require) that you include in the
 end-user documentation provided with the redistribution and/or in the
 software itself an acknowledgement equivalent to the following:
     "This product includes software developed by the
      JDOM Project (http://www.jdom.org/)."
 Alternatively, the acknowledgment may be graphical using the logos
 available at http://www.jdom.org/images/logos.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED.  IN NO EVENT SHALL THE JDOM AUTHORS OR THE PROJECT
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 This software consists of voluntary contributions made by many
 individuals on behalf of the JDOM Project and was originally
 created by Jason Hunter <jhunter_AT_jdom_DOT_org> and
 Brett McLaughlin <brett_AT_jdom_DOT_org>.  For more information
 on the JDOM Project, please see <http://www.jdom.org/>.

 */

package consulo.util.jdom;

import org.jdom.CDATA;
import org.jdom.Content;
import org.jdom.Text;
import org.jdom.Verifier;
import org.jdom.internal.ArrayCopy;
import org.jdom.output.EscapeStrategy;
import org.jdom.output.Format;
import org.jdom.output.support.FormatStack;
import org.jdom.output.support.Walker;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * This Walker implementation walks a list of Content in a Formatted form of
 * some sort.
 * <p>
 * The JDOM content can be loosely categorised in to 'Text-like' content
 * (consisting of Text, CDATA, and EntityRef), and everything else. This
 * distinction is significant for for this class and its sub-classes.
 * <p>
 * There will be text manipulation, and some (but not necessarily
 * all) Text-like content will be returned as text() instead of next().
 * <p>
 * The trick in this class is that it deals with the regular content, and
 * delegates the Text-like content to the sub-classes.
 * <p>
 * Subclasses are tasked with analysing chunks of Text-like content in the
 * {@link #analyzeMultiText(MultiText, int, int)}  method. The subclasses are
 * responsible for adding the relevant text content to the suppliedMultiText
 * instance in such a way as to result in the correct format.
 * <p>
 * The Subclass needs to concern itself with only the text portion because this
 * abstract class will ensure the Text-like content is appropriately indented.
 *
 * @author Rolf Lear
 */
class CustomWalker implements Walker {

  /*
   * We use Text instances to return formatted text to the caller.
   * We do not need to validate the Text content... it is 'safe' to
   * not use the default Text class.
   */
  private static final CDATA CDATATOKEN = new CDATA("");

  /**
   * Indicate how text content should be added
   *
   * @author Rolf Lear
   */
  protected enum Trim {
    /**
     * Left Trim
     */
    LEFT,
    /**
     * Right Trim
     */
    RIGHT,
    /**
     * Both Trim
     */
    BOTH,
    /**
     * Trim Both and replace all internal whitespace with a single space
     */
    COMPACT,
    /**
     * No Trimming at all
     */
    NONE}

  private static final Iterator<Content> EMPTYIT = new Iterator<Content>() {
    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public Content next() {
      throw new NoSuchElementException("Cannot call next() on an empty iterator.");
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("Cannot remove from an empty iterator.");
    }
  };

  /**
   * Collect together the items that constitute formatted Text-like content.
   *
   * @author Rolf Lear
   */
  protected final class MultiText {

    /**
     * This is private so only this abstract class can create instances.
     */
    private MultiText() {
    }

    /**
     * Ensure we have space for at least one more text-like item.
     */
    private void ensureSpace() {
      if (mtSize >= mtData.length) {
        mtData = ArrayCopy.copyOf(mtData, mtSize + 1 + (mtSize / 2));
        mtText = ArrayCopy.copyOf(mtText, mtData.length);
      }
    }

    /**
     * Handle the case where we have been accumulating true text content,
     * and the next item is not more text.
     */
    private void closeText() {
      if (mtBuffer.length() == 0) {
        // empty text does not need adding at all.
        return;
      }
      ensureSpace();
      mtData[mtSize] = null;
      mtText[mtSize++] = mtBuffer.toString();
      mtBuffer.setLength(0);
    }

    /**
     * Append some text to the text-like sequence that will be treated as
     * plain XML text (PCDATA). If the last content added to this text-like
     * sequence then this new text will be appended directly to the previous
     * text.
     *
     * @param trim How to prepare the Text content
     * @param text The actual Text content.
     */
    public void appendText(Trim trim, String text) {
      int tLen = text.length();
      if (tLen == 0) {
        return;
      }
      String toAdd = null;
      switch (trim) {
        case NONE:
          toAdd = text;
          break;
        case BOTH:
          toAdd = Format.trimBoth(text);
          break;
        case LEFT:
          toAdd = Format.trimLeft(text);
          break;
        case RIGHT:
          toAdd = Format.trimRight(text);
          break;
        case COMPACT:
          toAdd = Format.compact(text);
          break;
      }
      if (toAdd != null) {
        toAdd = escapeText(toAdd);
        mtBuffer.append(toAdd);
        mtGotText = true;
      }
    }

    private String escapeText(String text) {
      if (escape == null || !fStack.getEscapeOutput()) {
        return text;
      }
      return JDOMUtil.escapeText(text, false, false);
    }

    private String escapeCDATA(String text) {
      if (escape == null) {
        return text;
      }
      return text;
    }

    /**
     * Append some text to the text-like sequence that will be treated as
     * CDATA.
     *
     * @param trim How to prepare the CDATA content
     * @param text The actual CDATA content.
     */
    public void appendCDATA(Trim trim, String text) {
      // this resets the mtBuffer too.
      closeText();
      String toAdd = switch (trim) {
        case NONE -> text;
        case BOTH -> Format.trimBoth(text);
        case LEFT -> Format.trimLeft(text);
        case RIGHT -> Format.trimRight(text);
        case COMPACT -> Format.compact(text);
      };

      toAdd = escapeCDATA(toAdd);
      ensureSpace();
      // mark this as being CDATA text
      mtData[mtSize] = CDATATOKEN;
      mtText[mtSize++] = toAdd;

      mtGotText = true;
    }

    /**
     * Simple method that ensures the text is processed, regardless of
     * content, and is never escaped.
     *
     * @param text
     */
    private void forceAppend(String text) {
      mtGotText = true;
      mtBuffer.append(text);
    }

    /**
     * Add some JDOM Content (typically an EntityRef) that will be treated
     * as part of the Text-like sequence.
     *
     * @param c the content to add.
     */
    public void appendRaw(Content c) {
      closeText();
      ensureSpace();
      mtText[mtSize] = null;
      mtData[mtSize++] = c;
      mtBuffer.setLength(0);
    }

    /**
     * Indicate that there is no further content to be added to the
     * text-like sequence.
     */
    public void done() {
      if (mtPostPad && newLineIndent != null) {
        // this will be ignored if there was not some content.
        mtBuffer.append(newLineIndent);
      }
      if (mtGotText) {
        closeText();
      }
      mtBuffer.setLength(0);
    }
  }

  private @Nullable Content pending = null;
  private final Iterator<? extends Content> content;
  private final boolean allText;
  private final boolean allWhite;
  private final String newLineIndent;
  private final String endOfLine;
  private final @Nullable EscapeStrategy escape;
  private final FormatStack fStack;
  private @Nullable boolean hasNext = true;

  // MultiText handling changed in 2.0.5
  // MultiText is something quite complicated, but it goes something like this:
  // XML Content is either text-like, or its not. If we encounter text-like content
  // then we find out how many text-like contents are in a row, and we add them to a
  // multi-text. We then either get to the end of the content, or a non-text content.
  // If we complete the multi-text, we then move on to the non-text item, and we set multi-text
  // to null. Both multi-text and pendingMT are thus null.
  // If the content following the non-text is then text-like, we populate pendingMT.
  // bottom line is that multi-text and pendingMT can never both be set.
  // we use one set of variables to back up both of them. This is fast, and safe in a single
  // threaded environment (which the Walkers are guaranteed to be in).
  // all MultiText-specific variables have the names mt*
  private @Nullable MultiText multiText = null;
  private @Nullable MultiText pendingMT = null;
  private final MultiText holdingMT = new MultiText();

  private final StringBuilder mtBuffer = new StringBuilder();
  // if there should be indenting after this text.
  private boolean mtPostPad = false;
  // indicate whether there is something actually added.
  private boolean mtGotText = false;
  // the number of mixed content values.
  private int mtSize = 0;
  private int mtSourceSize = 0;
  private @Nullable Content[] mtSource = new Content[8];
  // the location of the processed content.
  private @Nullable Content[] mtData = new Content[8];
  // whether the mixed content should be returned as raw JDOM objects
  private @Nullable String[] mtText = new String[8];

  // the current cursor in the mixed content.
  private int mtPos = -1;
  // we cheat here by using Boolean as a three-state option...
  // we expect it to be null often.
  private @Nullable Boolean mtWasEscape;

  /**
   * Create a Walker that preserves all content in its raw state.
   *
   * @param xx       the content to walk.
   * @param fStack   the current FormatStack
   * @param doEscape Whether Text values should be escaped.
   */
  public CustomWalker(List<? extends Content> xx, FormatStack fStack, boolean doEscape) {
    super();
    this.fStack = fStack;
    this.content = xx.isEmpty() ? EMPTYIT : xx.iterator();
    this.escape = doEscape ? fStack.getEscapeStrategy() : null;
    newLineIndent = fStack.getPadBetween();
    endOfLine = fStack.getLevelEOL();
    if (!content.hasNext()) {
      allText = true;
      allWhite = true;
    }
    else {
      boolean aText = false;
      boolean aWhite = false;
      pending = content.next();
      if (isTextLike(pending)) {
        // the first item in the list is Text-like, and we pre-check
        // to see whether all content is text.... and whether it amounts
        // to something.
        pendingMT = buildMultiText(true);
        analyzeMultiText(pendingMT, 0, mtSourceSize);
        pendingMT.done();

        if (pending == null) {
          aText = true;
          aWhite = mtSize == 0;
        }
        if (mtSize == 0) {
          // first content in list is ignorable.
          pendingMT = null;
        }
      }
      allText = aText;
      allWhite = aWhite;
    }
    hasNext = pendingMT != null || pending != null;
  }

  @Override
  public final @Nullable Content next() {

    if (!hasNext) {
      throw new NoSuchElementException("Cannot walk off end of Content");
    }

    if (multiText != null && mtPos + 1 >= mtSize) {
      // finished this multi-text. need to move on.
      multiText = null;
      resetMultiText();
    }
    if (pendingMT != null) {
      // we have a multi-text pending from the last block
      // this will only be the case when the previous value was non-text.
      if (mtWasEscape != null && fStack.getEscapeOutput() != mtWasEscape.booleanValue()) {
        // we calculated pending with one escape strategy, but it changed...
        // we need to recalculate it....

        mtSize = 0;
        mtWasEscape = fStack.getEscapeOutput();
        analyzeMultiText(pendingMT, 0, mtSourceSize);
        pendingMT.done();
      }
      multiText = pendingMT;
      pendingMT = null;
    }

    if (multiText != null) {

      // OK, we have text-like content to push back.
      // and it still has values in it.
      // advance the cursor
      mtPos++;

      Content ret = mtText[mtPos] == null ? mtData[mtPos] : null;

      // we can calculate the hasNext
      hasNext = mtPos + 1 < mtSize || pending != null;

      // return null to indicate text content.
      return ret;
    }

    // non-text, increment and return content.
    Content ret = pending;
    pending = content.hasNext() ? content.next() : null;

    // OK, we are returning some content.
    // we need to determine the state of the next loop.
    // cursor at this point has been advanced!
    if (pending == null) {
      hasNext = false;
    }
    else {
      // there is some more content.
      // we need to inspect it to determine whether it is good
      if (isTextLike(pending)) {
        // calculate what this next text-like content looks like.
        pendingMT = buildMultiText(false);
        analyzeMultiText(pendingMT, 0, mtSourceSize);
        pendingMT.done();

        if (mtSize > 0) {
          hasNext = true;
        }
        else {
          // all white text... perhaps we need indenting anyway.
          // buildMultiText has moved on the pending value....
          if (pending != null && newLineIndent != null) {
            // yes, we need indenting.
            // redefine the pending.
            resetMultiText();
            pendingMT = holdingMT;
            pendingMT.forceAppend(newLineIndent);
            pendingMT.done();
            hasNext = true;
          }
          else {
            pendingMT = null;
            hasNext = pending != null;
          }
        }
      }
      else {
        // it is non-text content... we have more content.
        // but, we just returned non-text content. We may need to indent
        if (newLineIndent != null) {
          resetMultiText();
          pendingMT = holdingMT;
          pendingMT.forceAppend(newLineIndent);
          pendingMT.done();
        }
        hasNext = true;
      }
    }
    return ret;
  }

  private void resetMultiText() {
    mtSourceSize = 0;
    mtPos = -1;
    mtSize = 0;
    mtGotText = false;
    mtPostPad = false;
    mtWasEscape = null;
    mtBuffer.setLength(0);
  }

  /**
   * Add the content at the specified indices to the provided MultiText.
   *
   * @param mText  the MultiText to append to.
   * @param offset The first Text-like content to add to the MultiText
   * @param len    The number of Text-like content items to add.
   */
  protected void analyzeMultiText(MultiText mText, int offset, int len) {
    while (len > 0) {
      Content c = get(offset);
      if (c instanceof Text) {
        // either Text or CDATA
        if (!Verifier.isAllXMLWhitespace(c.getValue())) {
          break;
        }
      }
      else {
        break;
      }
      offset++;
      len--;
    }

    while (len > 0) {
      Content c = get(offset + len - 1);
      if (c instanceof Text) {
        // either Text or CDATA
        if (!Verifier.isAllXMLWhitespace(c.getValue())) {
          break;
        }
      }
      else {
        break;
      }
      len--;
    }

    for (int i = 0; i < len; i++) {
      Trim trim = Trim.NONE;
      if (i + 1 == len) {
        trim = Trim.RIGHT;
      }
      if (i == 0) {
        trim = Trim.LEFT;
      }
      if (len == 1) {
        trim = Trim.BOTH;
      }
      Content c = get(offset + i);
      switch (c.getCType()) {
        case Text:
          mText.appendText(trim, c.getValue());
          break;
        case CDATA:
          mText.appendCDATA(trim, c.getValue());
          break;
        case EntityRef:
          // treat like any other content.
          // raw.
        default:
          mText.appendRaw(c);
          break;
      }
    }
  }

  /**
   * Get the content at a position in the input content. Useful for subclasses
   * in their {@link #analyzeMultiText(MultiText, int, int)} calls.
   *
   * @param index the index to get the content at.
   * @return the content at the index.
   */
  protected final Content get(int index) {
    return Objects.requireNonNull(mtSource[index]);
  }

  @Override
  public final boolean isAllText() {
    return allText;
  }

  @Override
  public final boolean hasNext() {
    return hasNext;
  }

  /**
   * This method was changed in 2.0.5
   * It now is only called when building the content of the variable pendingMT
   * This is important, because only pendingMT can be referenced when analyzing
   * the MultiText content.
   *
   * @param first
   * @return The updated MultiText containing the correct sequence of Text-like content
   */
  private MultiText buildMultiText(boolean first) {
    // set up a sequence where the next bunch of stuff is text.
    if (!first && newLineIndent != null) {
      mtBuffer.append(newLineIndent);
    }
    mtSourceSize = 0;
    do {
      if (mtSourceSize >= mtSource.length) {
        mtSource = ArrayCopy.copyOf(mtSource, mtSource.length * 2);
      }
      mtSource[mtSourceSize++] = pending;
      pending = content.hasNext() ? content.next() : null;
    }
    while (pending != null && isTextLike(pending));

    mtPostPad = pending != null;
    mtWasEscape = fStack.getEscapeOutput();
    return holdingMT;
  }

  @Override
  public final @Nullable String text() {
    if (multiText == null || mtPos >= mtSize) {
      return null;
    }
    return mtText[mtPos];
  }

  @Override
  public final boolean isCDATA() {
    if (multiText == null || mtPos >= mtSize) {
      return false;
    }
    if (mtText[mtPos] == null) {
      return false;
    }

    return mtData[mtPos] == CDATATOKEN;
  }

  @Override
  public final boolean isAllWhitespace() {
    return allWhite;
  }

  private final boolean isTextLike(Content c) {
    switch (c.getCType()) {
      case Text:
      case CDATA:
      case EntityRef:
        return true;
      default:
        // nothing.
    }
    return false;
  }

}
