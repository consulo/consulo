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
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.text.SegmentBuilder;
import consulo.web.gwt.client.transport.GwtHighlightInfo;
import consulo.web.gwt.client.transport.GwtTextRange;

import java.util.List;

/**
 * @author VISTALL
 * @since 17-May-16
 */
public class Editor extends SimplePanel {
  private SegmentBuilder myBuilder;

  private int myLineCount;

  private EditorCaretHandler myCaretHandler;

  private int myLastCaretOffset = 0;

  public Editor(String text) {
    myBuilder = new SegmentBuilder(text);

    int lineCount = 0;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '\n') {
        lineCount++;
      }
    }
    myLineCount = lineCount;
    setWidth("100%");
    setHeight("100%");
  }

  public void repaint() {

   /* StringBuilder builder = new StringBuilder();
    builder.append("<div id=\"leftGutter\"><pre id=\"linePanel\">");
    for (int i = 0; i < myLineCount; i++) {
      builder.append("<span id=\"line_").append(i).append("\">").append(String.valueOf(i + 1)).append("</span>").append("\n");
    }
    builder.append("</pre></div>");

    myPanel.add(myLineNumberPanel = new HTML(builder.toString()));
    final HTML codePanel = myBuilder.toHtml();
    myPanel.add(codePanel);        */

    String htmlAsText = myBuilder.toHtmlAsText();

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

      /*lineSpan.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          panel.add(new Image("/icons/debugger/db_set_breakpoint.png"));
        }
      });*/

      panel.add(lineSpan);

      HTMLTable.CellFormatter cellFormatter = grid.getCellFormatter();
      cellFormatter.addStyleName(i, 0, "noselectable");
      cellFormatter.addStyleName(i, 0, "editorLineRow");
      grid.setWidget(i, 0, panel);
    }

    String[] split = htmlAsText.split("\n");
    for (int i = 0; i < split.length; i++) {
      final String line = split[i];

      final InlineHTML lineSpan = new InlineHTML(line);
      lineSpan.setWidth("100%");
      lineSpan.addStyleName("editorLine");
      lineSpan.addStyleName("gen_Line_" + i);

      /*lineSpan.addMouseOverHandler(new MouseOverHandler() {
        @Override
        public void onMouseOver(MouseOverEvent event) {
          Element parentElement = lineSpan.getElement().getParentElement();
          parentElement.getStyle().setBackgroundColor("yellow");
        }
      }) ;
      lineSpan.addMouseOutHandler(new MouseOutHandler() {
        @Override
        public void onMouseOut(MouseOutEvent event) {
          Element parentElement = lineSpan.getElement().getParentElement();
          parentElement.getStyle().clearBackgroundColor();
        }
      }) ;*/

      grid.setWidget(i, 1, lineSpan);
      grid.getCellFormatter().setWordWrap(i, 1, false);

      lineSpan.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
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

          String styleName = ((Element)parentNode).getClassName();

          // TODO [VISTALL]  ge_Line_%% is not handled
          String[] iter = styleName.split("\\s+");
          for (String s1 : iter) {
            String prefix = "gen_TextRage_";
            if (s1.startsWith(prefix)) {
              String startEnd = s1.substring(prefix.length(), s1.length());
              String[] startAndEnd = startEnd.split("_");
              GwtTextRange textRange = new GwtTextRange(Integer.parseInt(startAndEnd[0]), Integer.parseInt(startAndEnd[1]));

              return textRange.getStartOffset() + offset;
            }
          }

          return 0;
        }
      });
    }

    ScrollPanel scrollPanel = new ScrollPanel(grid);
    scrollPanel.setHeight("100%");
    scrollPanel.setWidth("100%");

    DockPanel panel = new DockPanel();
    panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    panel.setWidth("100%");
    panel.add(scrollPanel, DockPanel.CENTER);

  /*  VerticalPanel markerPanel = new VerticalPanel();
    markerPanel.add(new HTML("1"));
    markerPanel.add(new HTML("2"));
    markerPanel.add(new HTML("3"));

    markerPanel.setWidth("16px");
    markerPanel.setHeight("100%");
    markerPanel.getElement().getStyle().setBackgroundColor("silver");        */

    // panel.add(markerPanel, DockPanel.EAST);

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
    return this;
  }

  public void addHighlightInfos(List<GwtHighlightInfo> result, int flag) {
    myBuilder.addHighlights(result, flag);

    repaint();
  }
}
