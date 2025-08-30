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

import consulo.versionControlSystem.log.graph.GraphElement;
import consulo.versionControlSystem.log.graph.NodePrintElement;
import consulo.versionControlSystem.log.graph.PrintElementManager;
import consulo.versionControlSystem.log.graph.PrintElementWithGraphElement;
import jakarta.annotation.Nonnull;

public class SimplePrintElementImpl extends PrintElementWithGraphElement implements NodePrintElement {

  public SimplePrintElementImpl(int rowIndex,
                                int positionInCurrentRow,
                                @Nonnull GraphElement graphElement,
                                @Nonnull PrintElementManager printElementManager) {
    super(rowIndex, positionInCurrentRow, graphElement, printElementManager);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof NodePrintElement)) return false;

    NodePrintElement that = (NodePrintElement)o;

    if (myPositionInCurrentRow != that.getPositionInCurrentRow()) return false;
    if (myRowIndex != that.getRowIndex()) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myRowIndex;
    result = 31 * result + myPositionInCurrentRow;
    return result;
  }
}
