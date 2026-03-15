// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.impl.psi.template;

import consulo.document.util.TextRange;
import consulo.util.lang.Pair;
import org.jetbrains.annotations.TestOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects modifications to apply to template text for later parsing by template data language parser.
 *
 * @see TemplateDataElementType.RangeCollector
 */
public class TemplateDataModifications {

  public static final TemplateDataModifications EMPTY = new TemplateDataModifications(Collections.emptyList());

  
  final List<TextRange> myOuterAndRemoveRanges;

  public TemplateDataModifications() {
    this(new ArrayList<>());
  }

  private TemplateDataModifications(List<TextRange> ranges) {
    myOuterAndRemoveRanges = ranges;
  }

  /**
   * @see TemplateDataElementType.RangeCollector#addOuterRange(TextRange)
   */
  public void addOuterRange(TextRange newRange) {
    addOuterRange(newRange, false);
  }

  /**
   * @see TemplateDataElementType.RangeCollector#addOuterRange(TextRange, boolean)
   */
  public void addOuterRange(TextRange range, boolean isInsertion) {
    myOuterAndRemoveRanges.add(isInsertion ? new RangeCollectorImpl.InsertionRange(range.getStartOffset(), range.getEndOffset()) : range);
  }

  /**
   * @see TemplateDataElementType.RangeCollector#addRangeToRemove(TextRange)
   */
  public void addRangeToRemove(int startOffset, CharSequence textToInsert) {
    myOuterAndRemoveRanges.add(new RangeCollectorImpl.RangeToRemove(startOffset, textToInsert));
  }

  
  public static TemplateDataModifications fromRangeToRemove(int startOffset, CharSequence textToInsert) {
    TemplateDataModifications modifications = new TemplateDataModifications();
    modifications.addRangeToRemove(startOffset, textToInsert);
    return modifications;
  }

  public boolean addAll(TemplateDataModifications other) {
    return myOuterAndRemoveRanges.addAll(other.myOuterAndRemoveRanges);
  }

  @TestOnly
  
  public Pair<CharSequence, TemplateDataElementType.RangeCollector> applyToText(CharSequence text, TemplateDataElementType anyType) {
    RangeCollectorImpl collector = new RangeCollectorImpl(anyType);
    return Pair.create(collector.applyTemplateDataModifications(text, this), collector);
  }
}
