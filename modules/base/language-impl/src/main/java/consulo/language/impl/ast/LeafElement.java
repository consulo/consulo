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

package consulo.language.impl.ast;

import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import consulo.util.lang.CharArrayUtil;
import consulo.util.lang.ref.SoftReference;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class LeafElement extends TreeElement {
  private static final Logger LOG = Logger.getInstance(LeafElement.class);
  private static final Key<SoftReference<String>> CACHED_TEXT = Key.create("CACHED_TEXT");

  private static final int TEXT_MATCHES_THRESHOLD = 5;

  private final CharSequence myText;

  protected LeafElement(@Nonnull IElementType type, CharSequence text) {
    super(type);
    myText = text;
  }

  @Nonnull
  @Override
  public LeafElement clone() {
    LeafElement clone = (LeafElement)super.clone();
    clone.clearCaches();
    return clone;
  }

  @Override
  public int getTextLength() {
    return myText.length();
  }

  @Nonnull
  @Override
  public CharSequence getChars() {
    return myText;
  }

  @Nonnull
  @Override
  public String getText() {
    CharSequence text = myText;
    if (text.length() > 1000 && !(text instanceof String)) { // e.g. a large text file
      String cachedText = SoftReference.dereference(getUserData(CACHED_TEXT));
      if (cachedText == null) {
        cachedText = text.toString();
        putUserData(CACHED_TEXT, new SoftReference<>(cachedText));
      }
      return cachedText;
    }

    return text.toString();
  }

  public char charAt(int position) {
    return myText.charAt(position);
  }

  public int copyTo(@Nullable char[] buffer, int start) {
    final int length = myText.length();
    if (buffer != null) {
      CharArrayUtil.getChars(myText, buffer, start, length);
    }
    return start + length;
  }

  @Override
  @Nonnull
  public char[] textToCharArray() {
    final char[] buffer = new char[myText.length()];
    CharArrayUtil.getChars(myText, buffer, 0);
    return buffer;
  }

  @Override
  public boolean textContains(char c) {
    final CharSequence text = myText;
    final int len = text.length();

    if (len > TEXT_MATCHES_THRESHOLD) {
      char[] chars = CharArrayUtil.fromSequenceWithoutCopying(text);
      if (chars != null) {
        for (char aChar : chars) {
          if (aChar == c) return true;
        }
        return false;
      }
    }

    for (int i = 0; i < len; ++i) {
      if (c == text.charAt(i)) return true;
    }

    return false;
  }

  @Override
  public int textMatches(@Nonnull CharSequence buffer, int start) {
    assert start >= 0 : start;
    return leafTextMatches(myText, buffer, start);
  }

  static int leafTextMatches(@Nonnull CharSequence text, @Nonnull CharSequence buffer, int start) {
    assert start >= 0 : start;
    final int length = text.length();
    if (buffer.length() - start < length) {
      return start == 0 ? Integer.MIN_VALUE : -start;
    }
    for (int i = 0; i < length; i++) {
      int k = i + start;
      if (text.charAt(i) != buffer.charAt(k)) {
        return k == 0 ? Integer.MIN_VALUE : -k;
      }
    }
    return start + length;
  }

  @Nonnull
  public LeafElement rawReplaceWithText(@Nonnull String newText) {
    LeafElement newLeaf = ASTFactory.leaf(getElementType(), newText);
    copyUserDataTo(newLeaf);
    rawReplaceWithList(newLeaf);
    newLeaf.clearCaches();
    return newLeaf;
  }

  @Nonnull
  public LeafElement replaceWithText(@Nonnull String newText) {
    LeafElement newLeaf = ChangeUtil.copyLeafWithText(this, newText);
    getTreeParent().replaceChild(this, newLeaf);
    return newLeaf;
  }

  @Override
  public LeafElement findLeafElementAt(int offset) {
    return this;
  }

  @Override
  public boolean textMatches(@Nonnull final CharSequence buf, int start, int end) {
    final CharSequence text = getChars();
    final int len = text.length();

    if (end - start != len) return false;
    if (buf == text) return true;

    if (len > TEXT_MATCHES_THRESHOLD && text instanceof String && buf instanceof String) {
      return ((String)text).regionMatches(0, (String)buf, start, len);
    }

    for (int i = 0; i < len; i++) {
      if (text.charAt(i) != buf.charAt(start + i)) return false;
    }

    return true;
  }

  @Override
  public void acceptTree(@Nonnull TreeElementVisitor visitor) {
    visitor.visitLeaf(this);
  }

  @Override
  public ASTNode findChildByType(@Nonnull IElementType type) {
    return null;
  }

  @Override
  public ASTNode findChildByType(@Nonnull IElementType type, @Nullable ASTNode anchor) {
    return null;
  }

  @Override
  @Nullable
  public ASTNode findChildByType(@Nonnull TokenSet typesSet) {
    return null;
  }

  @Override
  @Nullable
  public ASTNode findChildByType(@Nonnull TokenSet typesSet, @Nullable ASTNode anchor) {
    return null;
  }

  @Override
  public int hc() {
    return leafHC(getChars());
  }

  static int leafHC(CharSequence text) {
    int hc = 0;
    int length = text.length();
    for (int i = 0; i < length; i++) {
      hc += text.charAt(i);
    }
    return hc;
  }

  @Override
  public TreeElement getFirstChildNode() {
    return null;
  }

  @Override
  public TreeElement getLastChildNode() {
    return null;
  }

  @Override
  public int getCachedLength() {
    return myText.length();
  }

  @Nonnull
  @Override
  public ASTNode[] getChildren(TokenSet filter) {
    return EMPTY_ARRAY;
  }

  @Override
  public void addChild(@Nonnull ASTNode child, ASTNode anchorBefore) {
    throw new IncorrectOperationException("Leaf elements cannot have children.");
  }

  @Override
  public void addLeaf(@Nonnull final IElementType leafType, @Nonnull final CharSequence leafText, final ASTNode anchorBefore) {
    throw new IncorrectOperationException("Leaf elements cannot have children.");
  }

  @Override
  public void addChild(@Nonnull ASTNode child) {
    throw new IncorrectOperationException("Leaf elements cannot have children.");
  }

  @Override
  public void removeChild(@Nonnull ASTNode child) {
    throw new IncorrectOperationException("Leaf elements cannot have children.");
  }

  @Override
  public void replaceChild(@Nonnull ASTNode oldChild, @Nonnull ASTNode newChild) {
    throw new IncorrectOperationException("Leaf elements cannot have children.");
  }

  @Override
  public void replaceAllChildrenToChildrenOf(@Nonnull ASTNode anotherParent) {
    throw new IncorrectOperationException("Leaf elements cannot have children.");
  }

  @Override
  public void removeRange(@Nonnull ASTNode first, ASTNode firstWhichStayInTree) {
    throw new IncorrectOperationException("Leaf elements cannot have children.");
  }

  @Override
  public void addChildren(@Nonnull ASTNode firstChild, ASTNode lastChild, ASTNode anchorBefore) {
    throw new IncorrectOperationException("Leaf elements cannot have children.");
  }

  @Override
  public PsiElement getPsi() {
    return null;
  }

  @Override
  public <T extends PsiElement> T getPsi(@Nonnull Class<T> clazz) {
    return getPsi(clazz, getPsi(), LOG);
  }

  static <T extends PsiElement> T getPsi(@Nonnull Class<T> clazz, PsiElement element, @Nonnull Logger log) {
    log.assertTrue(clazz.isInstance(element), "unexpected psi class. expected: " + clazz + " got: " + (element == null ? null : element.getClass()));
    //noinspection unchecked
    return (T)element;
  }
}
