/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.versionControlSystem.log.impl.internal.graph;

import consulo.versionControlSystem.log.graph.*;
import jakarta.annotation.Nonnull;

public class EdgePrintElementImpl extends PrintElementWithGraphElement implements EdgePrintElement {

  @Nonnull
  public static EdgePrintElement.LineStyle convertToLineStyle(@Nonnull GraphEdgeType edgeType) {
    switch (edgeType) {
      case USUAL:
      case NOT_LOAD_COMMIT:
        return EdgePrintElement.LineStyle.SOLID;
      case DOTTED:
      case DOTTED_ARROW_UP:
      case DOTTED_ARROW_DOWN:
        return EdgePrintElement.LineStyle.DASHED;
      default:
        throw new IllegalStateException("Edge type not supported: " + edgeType);
    }
  }

  @Nonnull
  private final Type myType;
  @Nonnull
  private final LineStyle myLineStyle;
  private final int myPositionInOtherRow;
  private final boolean myHasArrow;

  public EdgePrintElementImpl(int rowIndex,
                              int positionInCurrentRow,
                              int positionInOtherRow,
                              @Nonnull Type type,
                              @Nonnull GraphEdge graphEdge,
                              boolean hasArrow,
                              @Nonnull PrintElementManager printElementManager) {
    super(rowIndex, positionInCurrentRow, graphEdge, printElementManager);
    myType = type;
    myLineStyle = convertToLineStyle(graphEdge.getType());
    myPositionInOtherRow = positionInOtherRow;
    myHasArrow = hasArrow;
  }

  @Override
  public int getPositionInOtherRow() {
    return myPositionInOtherRow;
  }

  @Nonnull
  @Override
  public Type getType() {
    return myType;
  }

  @Nonnull
  @Override
  public LineStyle getLineStyle() {
    return myLineStyle;
  }

  @Override
  public boolean hasArrow() {
    return myHasArrow;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof EdgePrintElement)) return false;

    EdgePrintElement that = (EdgePrintElement)o;

    if (myPositionInCurrentRow != that.getPositionInCurrentRow()) return false;
    if (myPositionInOtherRow != that.getPositionInOtherRow()) return false;
    if (myRowIndex != that.getRowIndex()) return false;
    if (myType != that.getType()) return false;
    if (myHasArrow != that.hasArrow()) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myRowIndex;
    result = 31 * result + myPositionInCurrentRow;
    result = 31 * result + myPositionInOtherRow;
    result = 37 * result + myType.hashCode();
    result = 31 * result + (myHasArrow ? 1 : 0);
    return result;
  }
}
