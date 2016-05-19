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
package consulo.web.gwt.client.ui;

import com.bfr.client.selection.Range;
import com.bfr.client.selection.RangeEndPoint;
import com.bfr.client.selection.Selection;
import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.transport.GwtHighlightInfo;
import consulo.web.gwt.client.transport.GwtTextRange;

import java.util.List;

/**
 * @author VISTALL
 * @since 17-May-16
 */
public class Editor extends SimplePanel {
  private EditorSegmentBuilder myBuilder;

  private int myLineCount;

  private EditorCaretHandler myCaretHandler;

  private int myLastCaretOffset = 0;

  public Editor(String text) {
    myBuilder = new EditorSegmentBuilder(text);
    myLineCount = myBuilder.getLineCount();

    sinkEvents(Event.ONCLICK);

    setWidth("100%");
    setHeight("100%");
  }

  @Override
  public void onBrowserEvent(Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONCLICK:
        fireBrowseEventImpl(event);
        break;
      default:
        super.onBrowserEvent(event);
        break;
    }
  }

  private void build() {
    Grid grid = new Grid(myLineCount + 1, 2);
    grid.setWidth("100%");

    grid.getColumnFormatter().addStyleName(0, "editorLineColumn");
    grid.getColumnFormatter().addStyleName(1, "editorCodeColumn");

    for (int i = 0; i < myLineCount; i++) {
      final HorizontalPanel panel = new HorizontalPanel();
      panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
      panel.setWidth("100%");
      panel.setHeight("100%");

      InlineHTML lineSpan = new InlineHTML(String.valueOf(i + 1));
      lineSpan.addStyleName("editorLine");
      lineSpan.addStyleName("editorGutterLine" + i);

      panel.add(lineSpan);

      HTMLTable.CellFormatter cellFormatter = grid.getCellFormatter();
      cellFormatter.addStyleName(i, 0, "noselectable");
      cellFormatter.addStyleName(i, 0, "editorLineRow");
      grid.setWidget(i, 0, panel);
    }

    int lineCount = 0;
    FlowPanel lineElement = null;

    for (EditorSegmentBuilder.Fragment fragment : myBuilder.getFragments()) {
      if (lineElement == null) {
        lineElement = new FlowPanel();
        lineElement.setWidth("100%");
        lineElement.addStyleName("editorLine");
        lineElement.addStyleName("gen_Line_" + lineCount);
      }

      lineElement.add(fragment.widget);

      if (fragment.lineWrap) {
        grid.setWidget(lineCount, 1, lineElement);
        grid.getCellFormatter().setWordWrap(lineCount, 1, false);
        lineElement = null;

        lineCount++;
      }
    }

    ScrollPanel scrollPanel = new ScrollPanel(grid);
    scrollPanel.setHeight("100%");
    scrollPanel.setWidth("100%");

    DockPanel panel = new DockPanel();
    panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    panel.setWidth("100%");
    panel.add(scrollPanel, DockPanel.CENTER);

    setWidget(panel);
  }

  private void fireBrowseEventImpl(Event event) {
    int caretOffset = findCaretOffset();

    if (caretOffset == myLastCaretOffset) {
      return;
    }
    myLastCaretOffset = caretOffset;

    if (myCaretHandler != null) {
      myCaretHandler.caretPlaced(new EditorCaretEvent(event.getClientX(), event.getClientY(), caretOffset));
    }
  }

  private int findCaretOffset() {
    Range browserRange = Selection.getBrowserRange();

    RangeEndPoint startPoint = browserRange.getStartPoint();
    Text text = startPoint.getTextNode();
    if (text == null) {
      return 0;
    }
    int offset = startPoint.getOffset();
    Node parentNode = text.getParentNode();
    if (parentNode == null) {
      return 0;
    }

    if (!parentNode.getNodeName().equalsIgnoreCase("span")) {
      return 0;
    }

    Object range = ((Element)parentNode).getPropertyObject("range");
    if(!(range instanceof GwtTextRange)) {
      return 0;
    }

    return ((GwtTextRange)range).getStartOffset();
  }

  private void setupTooltip(Widget w, String message) {
    Tooltip tooltip = new Tooltip();
    tooltip.setWidget(w);
    tooltip.setText(message);
    tooltip.setPlacement(Placement.BOTTOM);
    tooltip.reconfigure();
  }

  public void setCaretHandler(EditorCaretHandler caretHandler) {
    myCaretHandler = caretHandler;
  }

  public Widget getComponent() {
    build();

    return this;
  }

  public void addHighlightInfos(List<GwtHighlightInfo> result, int flag) {
    myBuilder.addHighlights(result, flag);
  }

  public void focusOffset(int offset) {
    for (EditorSegmentBuilder.Fragment fragment : myBuilder.getFragments()) {
      if(fragment.range.containsRange(offset, offset)) {
        fragment.widget.getElement().focus();
        fragment.widget.getElement().scrollIntoView();

        break;
      }
    }
  }
}
