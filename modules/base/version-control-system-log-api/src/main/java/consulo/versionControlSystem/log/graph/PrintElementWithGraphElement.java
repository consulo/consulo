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

package consulo.versionControlSystem.log.graph;

import jakarta.annotation.Nonnull;

public abstract class PrintElementWithGraphElement implements PrintElement {

  protected final int myRowIndex;
  protected final int myPositionInCurrentRow;

  @Nonnull
  protected final GraphElement myGraphElement;
  @Nonnull
  protected final PrintElementManager myPrintElementManager;

  protected PrintElementWithGraphElement(int rowIndex,
                                         int positionInCurrentRow,
                                         @Nonnull GraphElement graphElement,
                                         @Nonnull PrintElementManager printElementManager) {
    myRowIndex = rowIndex;
    myPositionInCurrentRow = positionInCurrentRow;
    myGraphElement = graphElement;
    myPrintElementManager = printElementManager;
  }

  @Nonnull
  public GraphElement getGraphElement() {
    return myGraphElement;
  }

  @Override
  public int getRowIndex() {
    return myRowIndex;
  }

  @Override
  public int getPositionInCurrentRow() {
    return myPositionInCurrentRow;
  }

  @Override
  public int getColorId() {
    return myPrintElementManager.getColorId(myGraphElement);
  }

  @Override
  public boolean isSelected() {
    return myPrintElementManager.isSelected(this);
  }

  @Nonnull
  public static PrintElementWithGraphElement converted(@Nonnull PrintElementWithGraphElement element,
                                                       @Nonnull GraphElement convertedGraphElement) {
    return new PrintElementWithGraphElement(element.getRowIndex(), element.getPositionInCurrentRow(), convertedGraphElement,
                                            element.myPrintElementManager) {
    };
  }
}
