// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.psi.template;

import consulo.application.ApplicationManager;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.impl.DebugUtil;
import consulo.language.impl.ast.*;
import consulo.language.impl.ast.SharedImplUtil;
import consulo.language.util.CharTable;
import consulo.logging.Logger;
import consulo.logging.attachment.AttachmentFactory;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;

public class RangeCollectorImpl extends TemplateDataElementType.RangeCollector {
  private final TemplateDataElementType myTemplateDataElementType;
  private final List<TextRange> myOuterAndRemoveRanges;

  static final Key<RangeCollectorImpl> OUTER_ELEMENT_RANGES = Key.create("template.parser.outer.element.handler");

  RangeCollectorImpl(@Nonnull TemplateDataElementType templateDataElementType) {
    this(templateDataElementType, new ArrayList<>());
  }

  private RangeCollectorImpl(@Nonnull TemplateDataElementType templateDataElementType, @Nonnull List<TextRange> outerAndRemoveRanges) {
    myTemplateDataElementType = templateDataElementType;
    myOuterAndRemoveRanges = outerAndRemoveRanges;
  }

  @Override
  public void addOuterRange(@Nonnull TextRange newRange) {
    addOuterRange(newRange, false);
  }

  @Override
  public void addOuterRange(@Nonnull TextRange newRange, boolean isInsertion) {
    if (newRange.isEmpty()) {
      return;
    }
    assertRangeOrder(newRange);

    if (!myOuterAndRemoveRanges.isEmpty()) {
      int lastItemIndex = myOuterAndRemoveRanges.size() - 1;
      TextRange lastRange = myOuterAndRemoveRanges.get(lastItemIndex);
      if (lastRange.getEndOffset() == newRange.getStartOffset() && !(lastRange instanceof RangeToRemove)) {
        TextRange joinedRange = lastRange instanceof InsertionRange || isInsertion
                                ? new InsertionRange(lastRange.getStartOffset(), newRange.getEndOffset())
                                : TextRange.create(lastRange.getStartOffset(), newRange.getEndOffset());
        myOuterAndRemoveRanges.set(lastItemIndex, joinedRange);
        return;
      }
    }
    myOuterAndRemoveRanges.add(isInsertion ? new InsertionRange(newRange.getStartOffset(), newRange.getEndOffset()) : newRange);
  }

  @Override
  public void addRangeToRemove(@Nonnull TextRange rangeToRemove) {
    if (rangeToRemove.isEmpty()) {
      return;
    }
    assertRangeOrder(rangeToRemove);

    myOuterAndRemoveRanges.add(new RangeToRemove(rangeToRemove.getStartOffset(), rangeToRemove.getEndOffset()));
  }

  private void assertRangeOrder(@Nonnull TextRange newRange) {
    TextRange range = ContainerUtil.getLastItem(myOuterAndRemoveRanges);
    assert range == null || newRange.getStartOffset() >= range.getStartOffset();
  }

  void prepareFileForParsing(@Nonnull Language templateLanguage, @Nonnull CharSequence originalSourceCode, @Nonnull CharSequence templateSourceCode) {
    addDummyStringsToRangesToRemove(templateSourceCode);
    OuterLanguageRangePatcher patcher = OuterLanguageRangePatcher.forLanguage(templateLanguage);
    if (patcher != null) {
      StringBuilder builder = templateSourceCode instanceof StringBuilder ? (StringBuilder)templateSourceCode : new StringBuilder(templateSourceCode);
      insertDummyStringIntoInsertionRanges(patcher, originalSourceCode, builder);
    }
  }

  /**
   * Sets {@link RangeToRemove#myTextToRemove} in {@link #myOuterAndRemoveRanges} from generated file,
   * so lazy parseables implementing {@link TemplateDataElementType.TemplateAwareElementType} will have dummy strings when they are parsed.
   */
  private void addDummyStringsToRangesToRemove(@Nonnull CharSequence generatedText) {
    int shift = 0;
    ListIterator<TextRange> iterator = myOuterAndRemoveRanges.listIterator();
    while (iterator.hasNext()) {
      TextRange range = iterator.next();
      if (range instanceof RangeToRemove) {
        CharSequence insertedString = generatedText.subSequence(range.getStartOffset() - shift, range.getEndOffset() - shift);
        iterator.set(new RangeToRemove(range.getStartOffset(), insertedString)); // only add insertedString
        shift -= range.getLength();
      }
      else {
        shift += range.getLength();
      }
    }
  }

  private void insertDummyStringIntoInsertionRanges(@Nonnull OuterLanguageRangePatcher patcher, @Nonnull CharSequence originalSourceCode, @Nonnull StringBuilder modifiedText) {
    if (myOuterAndRemoveRanges.isEmpty()) return;

    int shift = 0;
    ListIterator<TextRange> iterator = myOuterAndRemoveRanges.listIterator();
    while (iterator.hasNext()) {
      TextRange outerElementRange = iterator.next();
      if (outerElementRange instanceof RangeToRemove) {
        shift += outerElementRange.getLength();
      }
      else {
        if (outerElementRange instanceof InsertionRange) {
          String dummyString = patcher.getTextForOuterLanguageInsertionRange(myTemplateDataElementType, outerElementRange.subSequence(originalSourceCode));
          if (dummyString != null) {
            // Don't set RangeToRemove#myTextToRemove so it won't be applied to nested lazy parseables.
            // Nested lazy parseables may add dummy string themselves.
            iterator.add(new RangeToRemove(outerElementRange.getEndOffset(), outerElementRange.getEndOffset() + dummyString.length()));
            modifiedText.insert(outerElementRange.getStartOffset() + shift, dummyString);
            shift += dummyString.length();
          }
        }
        shift -= outerElementRange.getLength();
      }
    }
  }

  /**
   * Builds the merged tree with inserting outer language elements and removing additional elements according to the ranges from rangeCollector.
   * Chameleons which implement {@link TemplateDataElementType.TemplateAwareElementType} are treated as leaves, i.e. they are not expanded
   * but their contents are modified considering outer elements and RangeCollector is put to their user data to be applied when this
   * chameleon is expanded.
   *
   * @param language
   * @param templateFileElement parsed template data language file without outer elements and with possible custom additions
   * @param sourceCode          original source code (include template data language and template language)
   */
  void insertOuterElementsAndRemoveRanges(@Nonnull TreeElement templateFileElement, @Nonnull CharSequence sourceCode, @Nonnull CharTable charTable, @Nonnull Language language) {
    TreePatcher templateTreePatcher = TreePatcher.forLanguage(language);

    TreeElement currentLeafOrLazyParseable = findFirstSuitableElement(templateFileElement);

    // we use manual offset counter because node.getStartOffset() is expensive here
    // offset in original text
    int currentLeafOffset = 0;

    for (TextRange rangeToProcess : myOuterAndRemoveRanges) {
      int rangeStartOffset = rangeToProcess.getStartOffset();

      // search for leaf following or intersecting range
      while (currentLeafOrLazyParseable != null && currentLeafOffset + currentLeafOrLazyParseable.getTextLength() <= rangeStartOffset) {
        currentLeafOffset += currentLeafOrLazyParseable.getTextLength();
        currentLeafOrLazyParseable = findNextSuitableElement(currentLeafOrLazyParseable);
      }

      boolean addRangeToLazyParseableCollector = false;
      if (rangeToProcess instanceof RangeToRemove) {
        if (currentLeafOrLazyParseable == null) {
          Logger.getInstance(TemplateDataElementType.RangeCollector.class).error("RangeToRemove's range is out of original text bound", AttachmentFactory.get()
                                                                                         .create("myOuterAndRemoveRanges", StringUtil.join(myOuterAndRemoveRanges, TextRange::toString, ", ")), AttachmentFactory.get().create("rangeToProcess", rangeToProcess.toString()),
                                                                                 AttachmentFactory.get().create("sourceCode", sourceCode.toString()));
          continue;
        }
        currentLeafOrLazyParseable = removeElementsForRange(currentLeafOrLazyParseable, currentLeafOffset, rangeToProcess, templateTreePatcher, charTable);
        if (currentLeafOrLazyParseable != null && !(currentLeafOrLazyParseable instanceof LeafElement) && ((RangeToRemove)rangeToProcess).myTextToRemove != null) {
          addRangeToLazyParseableCollector = true;
        }
      }
      else {
        if (currentLeafOrLazyParseable instanceof LeafElement && currentLeafOffset < rangeStartOffset) {
          int splitOffset = rangeStartOffset - currentLeafOffset;
          currentLeafOrLazyParseable = templateTreePatcher.split((LeafElement)currentLeafOrLazyParseable, splitOffset, charTable);
          currentLeafOffset = rangeStartOffset;
        }
        if (currentLeafOrLazyParseable == null) {
          insertLastOuterElementForRange((CompositeElement)templateFileElement, rangeToProcess, sourceCode, charTable);
        }
        else {
          currentLeafOrLazyParseable = insertOuterElementFromRange(currentLeafOrLazyParseable, currentLeafOffset, rangeToProcess, sourceCode, templateTreePatcher, charTable);
          if (!(currentLeafOrLazyParseable instanceof LeafElement)) {
            addRangeToLazyParseableCollector = true;
          }
        }
      }
      if (addRangeToLazyParseableCollector) {
        RangeCollectorImpl lazyParseableCollector = currentLeafOrLazyParseable.getUserData(OUTER_ELEMENT_RANGES);
        assert lazyParseableCollector != null && lazyParseableCollector != this;
        lazyParseableCollector.myOuterAndRemoveRanges.add(rangeToProcess.shiftLeft(currentLeafOffset));
      }
    }

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      String after = templateFileElement.getText();
      assert after.contentEquals(sourceCode) : "Text presentation for the new tree must be the same: \nbefore: " + sourceCode + "\nafter: " + after;
    }
  }

  /**
   * Similar to {@link TreeUtil#findFirstLeaf(ASTNode)}, but also treats collapsed lazy parseable elements as leaves and returns them.
   */
  private static
  @Nullable
  TreeElement findFirstSuitableElement(@Nonnull ASTNode element) {
    if (isSuitableElement(element)) {
      return (TreeElement)element;
    }
    else {
      for (ASTNode child = element.getFirstChildNode(); child != null; child = child.getTreeNext()) {
        TreeElement leaf = findFirstSuitableElement(child);
        if (leaf != null) return leaf;
      }
      return null;
    }
  }

  /**
   * Similar to {@link TreeUtil#nextLeaf(ASTNode)}, but also treats collapsed lazy parseable elements as leaves and returns them.
   */
  private static
  @Nullable
  TreeElement findNextSuitableElement(@Nonnull TreeElement start) {
    TreeElement element = start;
    while (element != null) {
      TreeElement nextTree = element;
      TreeElement next = null;
      while (next == null && (nextTree = nextTree.getTreeNext()) != null) {
        next = findFirstSuitableElement(nextTree);
      }
      if (next != null) {
        return next;
      }
      element = element.getTreeParent();
    }
    return null;
  }

  private static boolean isSuitableElement(@Nonnull ASTNode element) {
    return element instanceof LeafElement || TreeUtil.isCollapsedChameleon(element) && element.getElementType() instanceof TemplateDataElementType.TemplateAwareElementType;
  }

  private void insertLastOuterElementForRange(@Nonnull CompositeElement templateFileElement, @Nonnull TextRange outerElementRange, @Nonnull CharSequence sourceCode, @Nonnull CharTable charTable) {
    assert isLastRange(myOuterAndRemoveRanges, outerElementRange) : "This should only happen for the last inserted range. Got " +
                                                                    myOuterAndRemoveRanges.lastIndexOf(outerElementRange) +
                                                                    " of " +
                                                                    (myOuterAndRemoveRanges.size() - 1);
    OuterLanguageElementImpl outerLanguageElement =
            myTemplateDataElementType.createOuterLanguageElement(charTable.intern(outerElementRange.subSequence(sourceCode)), myTemplateDataElementType.myOuterElementType);
    templateFileElement.rawAddChildren(outerLanguageElement);
  }

  private static boolean isLastRange(@Nonnull List<TextRange> outerElementsRanges, @Nonnull TextRange outerElementRange) {
    return outerElementsRanges.get(outerElementsRanges.size() - 1) == outerElementRange;
  }

  private
  @Nonnull
  TreeElement insertOuterElementFromRange(@Nonnull TreeElement currentLeaf,
                                          int currentLeafOffset,
                                          @Nonnull TextRange outerElementRange,
                                          @Nonnull CharSequence sourceCode,
                                          @Nonnull TreePatcher templateTreePatcher,
                                          @Nonnull CharTable charTable) {
    CharSequence outerElementText = outerElementRange.subSequence(sourceCode);
    if (currentLeaf instanceof LazyParseableElement) {
      StringBuilder builder = new StringBuilder(currentLeaf.getText());
      builder.insert(outerElementRange.getStartOffset() - currentLeafOffset, outerElementText);
      TreeElement newElement = newLazyParseable(currentLeaf, builder.toString());
      currentLeaf.rawInsertAfterMe(newElement);
      currentLeaf.rawRemove();
      return newElement;
    }

    OuterLanguageElementImpl newLeaf = myTemplateDataElementType.createOuterLanguageElement(charTable.intern(outerElementText), myTemplateDataElementType.myOuterElementType);
    templateTreePatcher.insert(currentLeaf.getTreeParent(), currentLeaf, newLeaf);
    return newLeaf;
  }

  @Nonnull
  private TreeElement newLazyParseable(@Nonnull TreeElement currentLeaf, @Nonnull CharSequence text) {
    TemplateDataElementType.TemplateAwareElementType elementType = (TemplateDataElementType.TemplateAwareElementType)currentLeaf.getElementType();
    TreeElement newElement = elementType.createTreeElement(text);
    RangeCollectorImpl collector = currentLeaf.getUserData(OUTER_ELEMENT_RANGES);
    if (collector == null) collector = new RangeCollectorImpl(myTemplateDataElementType);
    newElement.putUserData(OUTER_ELEMENT_RANGES, collector);
    return newElement;
  }

  @Nullable
  private TreeElement removeElementsForRange(@Nonnull TreeElement startLeaf,
                                             int startLeafOffset,
                                             @Nonnull TextRange rangeToRemove,
                                             @Nonnull TreePatcher templateTreePatcher,
                                             @Nonnull CharTable charTable) {
    @Nullable TreeElement nextLeaf = startLeaf;
    int nextLeafStartOffset = startLeafOffset;
    Collection<TreeElement> leavesToRemove = new ArrayList<>();
    while (nextLeaf != null && rangeToRemove.containsRange(nextLeafStartOffset, nextLeafStartOffset + nextLeaf.getTextLength())) {
      leavesToRemove.add(nextLeaf);
      nextLeafStartOffset += nextLeaf.getTextLength();
      nextLeaf = findNextSuitableElement(nextLeaf);
    }

    nextLeaf = splitOrRemoveRangeInsideLeafIfOverlap(nextLeaf, nextLeafStartOffset, rangeToRemove, templateTreePatcher, charTable);

    for (TreeElement element : leavesToRemove) {
      element.rawRemove();
    }
    return nextLeaf;
  }


  /**
   * Removes part the nextLeaf that intersects rangeToRemove.
   * If nextLeaf doesn't intersect rangeToRemove the method returns the nextLeaf without changes
   *
   * @return new leaf after removing the range or original nextLeaf if nothing changed
   */
  @Nullable
  private TreeElement splitOrRemoveRangeInsideLeafIfOverlap(@Nullable TreeElement nextLeaf,
                                                            int nextLeafStartOffset,
                                                            @Nonnull TextRange rangeToRemove,
                                                            @Nonnull TreePatcher templateTreePatcher,
                                                            @Nonnull CharTable charTable) {
    if (nextLeaf == null) return null;
    if (nextLeafStartOffset >= rangeToRemove.getEndOffset()) return nextLeaf;

    if (rangeToRemove.getStartOffset() > nextLeafStartOffset) {
      return removeRange(nextLeaf, rangeToRemove.shiftLeft(nextLeafStartOffset), charTable);
    }

    int offsetToSplit = rangeToRemove.getEndOffset() - nextLeafStartOffset;
    return removeLeftPartOfLeaf(nextLeaf, offsetToSplit, templateTreePatcher, charTable);
  }

  /**
   * Splits the node according to the offsetToSplit and remove left leaf
   *
   * @return right part of the split node
   */
  @Nonnull
  private TreeElement removeLeftPartOfLeaf(@Nonnull TreeElement nextLeaf, int offsetToSplit, @Nonnull TreePatcher templateTreePatcher, @Nonnull CharTable charTable) {
    if (offsetToSplit == 0) return nextLeaf;
    if (!(nextLeaf instanceof LeafElement)) {
      return removeRange(nextLeaf, TextRange.from(0, offsetToSplit), charTable);
    }
    LeafElement rLeaf = templateTreePatcher.split((LeafElement)nextLeaf, offsetToSplit, charTable);
    LeafElement lLeaf = (LeafElement)TreeUtil.prevLeaf(rLeaf);
    assert lLeaf != null;
    lLeaf.rawRemove();
    return rLeaf;
  }

  /**
   * Like {@link TemplateDataElementType#parseContents} builds the tree considering outer language elements, but for inner lazy parseables.
   */
  @Nonnull
  ASTNode applyRangeCollectorAndExpandChameleon(@Nonnull ASTNode chameleon, @Nonnull Language language, @Nonnull Function<CharSequence, ASTNode> parser) {
    CharSequence chars = chameleon.getChars();
    if (myOuterAndRemoveRanges.isEmpty()) return parser.apply(chars);

    StringBuilder stringBuilder = applyOuterAndRemoveRanges(chars);

    OuterLanguageRangePatcher outerLanguageRangePatcher = OuterLanguageRangePatcher.forLanguage(language);
    if (outerLanguageRangePatcher != null) {
      insertDummyStringIntoInsertionRanges(outerLanguageRangePatcher, chars, stringBuilder);
    }

    ASTNode root = parser.apply(stringBuilder.toString());
    DebugUtil.performPsiModification("lazy parseable outer elements insertion", () -> {
      insertOuterElementsAndRemoveRanges((TreeElement)root, chars, SharedImplUtil.findCharTableByTree(chameleon), language);
    });

    return root;
  }

  @Nonnull
  private StringBuilder applyOuterAndRemoveRanges(CharSequence chars) {
    StringBuilder stringBuilder = new StringBuilder(chars);
    int shift = 0;
    for (TextRange outerElementRange : myOuterAndRemoveRanges) {
      if (outerElementRange instanceof RangeToRemove) {
        CharSequence textToRemove = ((RangeToRemove)outerElementRange).myTextToRemove;
        if (textToRemove != null) {
          stringBuilder.insert(outerElementRange.getStartOffset() + shift, textToRemove);
          shift += textToRemove.length();
        }
      }
      else {
        stringBuilder.delete(outerElementRange.getStartOffset() + shift, outerElementRange.getEndOffset() + shift);
        shift -= outerElementRange.getLength();
      }
    }
    return stringBuilder;
  }

  /**
   * Removes "middle" part of the leaf and returns the new leaf with content of the right and left parts
   * e.g. if we process whitespace leaf " \n " and range "1, 2" the result will be new leaf with content "  "
   */
  @Nonnull
  private TreeElement removeRange(@Nonnull TreeElement leaf, @Nonnull TextRange rangeToRemove, @Nonnull CharTable table) {
    CharSequence chars = leaf.getChars();
    String res = rangeToRemove.replace(chars.toString(), "");
    TreeElement newLeaf = leaf instanceof LeafElement ? ASTFactory.leaf(leaf.getElementType(), table.intern(res)) : newLazyParseable(leaf, res);
    leaf.rawInsertBeforeMe(newLeaf);
    leaf.rawRemove();
    return newLeaf;
  }

  @Nonnull
  CharSequence applyTemplateDataModifications(@Nonnull CharSequence sourceCode, @Nonnull TemplateDataModifications modifications) {
    assert myOuterAndRemoveRanges.isEmpty();
    List<TextRange> ranges = modifications.myOuterAndRemoveRanges;
    if (ranges.isEmpty()) return sourceCode;
    for (TextRange range : ranges) {
      if (range instanceof RangeToRemove) {
        if (range.isEmpty()) continue;
        assertRangeOrder(range);
        CharSequence textToRemove = ((RangeToRemove)range).myTextToRemove;
        assert textToRemove != null;
        myOuterAndRemoveRanges.add(new RangeToRemove(range.getStartOffset(), textToRemove));
      }
      else {
        addOuterRange(range, range instanceof InsertionRange);
      }
    }

    return applyOuterAndRemoveRanges(sourceCode);
  }


  final static class RangeToRemove extends TextRange {
    /**
     * We need this text to propagate dummy strings through lazy parseables. If this text is null, dummy identifier won't be propagated.
     */
    public final
    @Nullable
    CharSequence myTextToRemove;

    RangeToRemove(int startOffset, @Nonnull CharSequence text) {
      super(startOffset, startOffset + text.length());
      myTextToRemove = text;
    }

    RangeToRemove(int startOffset, int endOffset) {
      super(startOffset, endOffset);
      myTextToRemove = null;
    }

    @Override
    @Nonnull
    public TextRange shiftLeft(int delta) {
      if (delta == 0) return this;
      return myTextToRemove != null ? new RangeToRemove(getStartOffset() - delta, myTextToRemove) : new RangeToRemove(getStartOffset() - delta, getEndOffset() - delta);
    }

    @Override
    public String toString() {
      return "RangeToRemove" + super.toString();
    }
  }

  static final class InsertionRange extends TextRange {

    InsertionRange(int startOffset, int endOffset) {
      super(startOffset, endOffset);
    }

    @Override
    @Nonnull
    public TextRange shiftLeft(int delta) {
      if (delta == 0) return this;
      return new InsertionRange(getStartOffset() - delta, getEndOffset() - delta);
    }

    @Override
    public String toString() {
      return "InsertionRange" + super.toString();
    }
  }
}