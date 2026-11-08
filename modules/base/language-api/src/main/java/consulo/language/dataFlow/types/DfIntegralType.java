// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.dataFlow.types;

import consulo.language.dataFlow.rangeSet.LongRangeBinOp;
import consulo.language.dataFlow.rangeSet.LongRangeSet;
import consulo.language.dataFlow.rangeSet.LongRangeType;

/**
 * Represents an integral primitive whose value can be represented as {@link LongRangeSet}.
 */
public interface DfIntegralType extends DfType {
  /**
   * @return a range represented by this type
   */
  LongRangeSet getRange();

  /**
   * @return a LongRangeType that describes this value the best
   */
  LongRangeType getLongRangeType();

  /**
   * @return a wide range represented by this type (will be used in case if backbranch is visited)
   */
  default LongRangeSet getWideRange() {
    return getRange();
  }

  /**
   * Perform binary operation between this type and other type
   * @param other other operand
   * @param op operation to perform
   * @return result of the operation
   */
  DfType eval(DfType other, LongRangeBinOp op);

  DfType meetRange(LongRangeSet range);
}
