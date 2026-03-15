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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractPrintElementGenerator implements PrintElementGenerator {

  
  protected final LinearGraph myLinearGraph;
  
  protected final PrintElementManager myPrintElementManager;

  protected AbstractPrintElementGenerator(LinearGraph linearGraph, PrintElementManager printElementManager) {
    myLinearGraph = linearGraph;
    myPrintElementManager = printElementManager;
  }

  
  @Override
  public Collection<PrintElementWithGraphElement> getPrintElements(int rowIndex) {
    Collection<PrintElementWithGraphElement> result = new ArrayList<>();

    Map<GraphEdge, SimpleRowElement> arrows = new HashMap<>();

    for (SimpleRowElement rowElement : getSimpleRowElements(rowIndex)) {
      if (!rowElement.myType.equals(RowElementType.NODE)) {
        arrows.put((GraphEdge)rowElement.myElement, rowElement);
      }
    }

    if (rowIndex < myLinearGraph.nodesCount() - 1) {
      for (ShortEdge shortEdge : getDownShortEdges(rowIndex)) {
        RowElementType rowElementType = RowElementType.NODE;
        if ((arrows.get(shortEdge.myEdge) != null) && RowElementType.DOWN_ARROW.equals(arrows.get(shortEdge.myEdge).myType)) {
          rowElementType = RowElementType.DOWN_ARROW;
          arrows.remove(shortEdge.myEdge);
        }
        result.add(createEdgePrintElement(rowIndex, shortEdge, EdgePrintElement.Type.DOWN, !rowElementType.equals(RowElementType.NODE)));
      }
    }

    if (rowIndex > 0) {
      for (ShortEdge shortEdge : getDownShortEdges(rowIndex - 1)) {
        RowElementType rowElementType = RowElementType.NODE;
        if ((arrows.get(shortEdge.myEdge) != null) && RowElementType.UP_ARROW.equals(arrows.get(shortEdge.myEdge).myType)) {
          rowElementType = RowElementType.UP_ARROW;
          arrows.remove(shortEdge.myEdge);
        }
        result.add(createEdgePrintElement(rowIndex, shortEdge, EdgePrintElement.Type.UP, !rowElementType.equals(RowElementType.NODE)));
      }
    }

    for (SimpleRowElement arrow : arrows.values()) {
      result.add(new TerminalEdgePrintElement(rowIndex, arrow.myPosition, arrow.myType == RowElementType.UP_ARROW
                                                                          ? EdgePrintElement.Type.UP
                                                                          : EdgePrintElement.Type.DOWN, (GraphEdge)arrow.myElement,
                                              myPrintElementManager));
    }

    for (SimpleRowElement rowElement : getSimpleRowElements(rowIndex)) {
      if (rowElement.myType.equals(RowElementType.NODE)) {
        result.add(createSimplePrintElement(rowIndex, rowElement));
      }
    }

    return result;
  }

  
  private SimplePrintElementImpl createSimplePrintElement(int rowIndex, SimpleRowElement rowElement) {
    return new SimplePrintElementImpl(rowIndex, rowElement.myPosition, rowElement.myElement, myPrintElementManager);
  }

  
  private EdgePrintElementImpl createEdgePrintElement(int rowIndex,
                                                      ShortEdge shortEdge,
                                                      EdgePrintElement.Type type,
                                                      boolean hasArrow) {
    int positionInCurrentRow, positionInOtherRow;
    if (type == EdgePrintElement.Type.DOWN) {
      positionInCurrentRow = shortEdge.myUpPosition;
      positionInOtherRow = shortEdge.myDownPosition;
    }
    else {
      positionInCurrentRow = shortEdge.myDownPosition;
      positionInOtherRow = shortEdge.myUpPosition;
    }
    return new EdgePrintElementImpl(rowIndex, positionInCurrentRow, positionInOtherRow, type, shortEdge.myEdge, hasArrow,
                                    myPrintElementManager);
  }

  
  @Override
  public PrintElementWithGraphElement withGraphElement(PrintElement printElement) {
    if (printElement instanceof PrintElementWithGraphElement graphElement) {
      return graphElement;
    }

    int rowIndex = printElement.getRowIndex();
    for (PrintElementWithGraphElement printElementWithGE : getPrintElements(rowIndex)) {
      if (printElementWithGE.equals(printElement)) return printElementWithGE;
    }
    throw new IllegalStateException("Not found graphElement for this printElement: " + printElement);
  }

  // rowIndex in [0, getCountVisibleRow() - 2]
  
  protected abstract Collection<ShortEdge> getDownShortEdges(int rowIndex);

  
  protected abstract Collection<SimpleRowElement> getSimpleRowElements(int rowIndex);

  protected static class ShortEdge {
    
    public final GraphEdge myEdge;
    public final int myUpPosition;
    public final int myDownPosition;

    public ShortEdge(GraphEdge edge, int upPosition, int downPosition) {
      myEdge = edge;
      myUpPosition = upPosition;
      myDownPosition = downPosition;
    }
  }

  protected static class SimpleRowElement {
    
    public final GraphElement myElement;
    
    public final RowElementType myType;
    public final int myPosition;

    public SimpleRowElement(GraphElement element, RowElementType type, int position) {
      myElement = element;
      myPosition = position;
      myType = type;
    }
  }

  enum RowElementType {
    NODE,
    UP_ARROW,
    DOWN_ARROW
  }
}
