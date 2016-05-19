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

import com.github.gwtbootstrap.client.ui.Tooltip;
import com.github.gwtbootstrap.client.ui.constants.Placement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.util.GwtUtil;
import consulo.web.gwt.client.util.ReportableCallable;
import consulo.web.gwt.shared.transport.*;

import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 17-May-16
 */
public class Editor extends SimplePanel {
  public static final int ourLexerFlag = 1 << 1;
  public static final int ourEditorFlag = 1 << 2;

  public static final int ourSelectFlag = 1 << 24;

  private final EditorSegmentBuilder myBuilder;

  private final int myLineCount;

  private EditorCaretHandler myCaretHandler;

  private int myLastCaretOffset = -1;

  private final EditorTabPanel myEditorTabPanel;

  private final String myFileUrl;

  private GwtTextRange myLastCursorPsiElementTextRange;

  private GwtNavigateInfo myLastNavigationInfo;

  public Editor(EditorTabPanel editorTabPanel, String fileUrl, String text) {
    myEditorTabPanel = editorTabPanel;
    myFileUrl = fileUrl;
    myBuilder = new EditorSegmentBuilder(text);
    myLineCount = myBuilder.getLineCount();

    sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS);

    setWidth("100%");
    setHeight("100%");
  }

  @Override
  public void onBrowserEvent(final Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONMOUSEOVER:
        com.google.gwt.dom.client.Element element = DOM.eventGetToElement(event);

        Object range = element == null ? null : element.getPropertyObject("range");
        if (!(range instanceof GwtTextRange)) {
          return;
        }

        int startOffset = ((GwtTextRange)range).getStartOffset();
        if (startOffset == myLastCaretOffset) {
          return;
        }

        myLastCaretOffset = startOffset;

        if (event.getCtrlKey()) {
          getElement().getStyle().setCursor(Style.Cursor.POINTER);

          GwtUtil.rpc().getNavigationInfo(myFileUrl, myLastCaretOffset, new ReportableCallable<GwtNavigateInfo>() {
            @Override
            public void onSuccess(GwtNavigateInfo result) {
              if (result == null) {
                return;
              }

              event.getRelatedEventTarget();

              GwtTextRange resultElementRange = result.getRange();
              if (myLastCursorPsiElementTextRange != null && myLastCursorPsiElementTextRange.containsRange(resultElementRange)) {
                return;
              }
              myLastCursorPsiElementTextRange = resultElementRange;
              GwtHighlightInfo highlightInfo =
                      new GwtHighlightInfo(new GwtColor(0, 0, 255), null, GwtHighlightInfo.UNDERLINE, resultElementRange, Integer.MAX_VALUE);

              myLastNavigationInfo = result;

              addHighlightInfos(Arrays.asList(highlightInfo), ourSelectFlag);
            }
          });
        }
        break;
      case Event.ONMOUSEOUT:
        onMouseOut();
        break;
      case Event.ONCLICK:
        if (event.getCtrlKey()) {
          if (myLastNavigationInfo != null) {
            List<GwtNavigatable> navigates = myLastNavigationInfo.getNavigates();

            GwtNavigatable navigatable = navigates.get(0);

            onMouseOut();

            myEditorTabPanel.openFileInEditor(navigatable.getFile(), navigatable.getOffset());
          }
        }
        else {
          if(myLastCaretOffset == -1) {
            return;
          }
          if (myCaretHandler != null) {
            myCaretHandler.caretPlaced(new EditorCaretEvent(event.getClientX(), event.getClientY(), myLastCaretOffset));
          }
        }
        break;
      default:
        super.onBrowserEvent(event);
        break;
    }
  }

  private void onMouseOut() {
    if (myLastCursorPsiElementTextRange != null) {
      getElement().getStyle().clearCursor();

      myBuilder.removeHighlightByRange(myLastCursorPsiElementTextRange, ourSelectFlag);

      myLastCursorPsiElementTextRange = null;
      myLastNavigationInfo = null;
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
      if (fragment.range.containsRange(offset, offset)) {
        fragment.widget.getElement().focus();
        fragment.widget.getElement().scrollIntoView();

        break;
      }
    }
  }
}
