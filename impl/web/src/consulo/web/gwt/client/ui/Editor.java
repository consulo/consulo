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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.text.SegmentBuilder;
import consulo.web.gwt.client.transport.GwtHighlightInfo;

import java.util.List;

/**
 * @author VISTALL
 * @since 17-May-16
 */
public class Editor {
  private SimplePanel myPanel = new SimplePanel();
  private SegmentBuilder myBuilder;

  private int myLineCount;

  private HTML myLineNumberPanel;

  public Editor(String text) {
    myBuilder = new SegmentBuilder(text);

    int lineCount = 0;
    for (int i = 0; i < text.length(); i++) {
      if (text.charAt(i) == '\n') {
        lineCount++;
      }
    }
    myLineCount = lineCount;
    myPanel.setWidth("100%");
    myPanel.setHeight("100%");
  }

  public void update() {

   /* StringBuilder builder = new StringBuilder();
    builder.append("<div id=\"leftGutter\"><pre id=\"linePanel\">");
    for (int i = 0; i < myLineCount; i++) {
      builder.append("<span id=\"line_").append(i).append("\">").append(String.valueOf(i + 1)).append("</span>").append("\n");
    }
    builder.append("</pre></div>");

    myPanel.add(myLineNumberPanel = new HTML(builder.toString()));
    final HTML codePanel = myBuilder.toHtml();
    myPanel.add(codePanel);        */

    String s = myBuilder.toHtmlAsText();

    Grid grid = new Grid(myLineCount + 1, 2);
    grid.setWidth("100%");

    grid.getColumnFormatter().addStyleName(0, "editorLineColumn");
    grid.getColumnFormatter().addStyleName(1, "editorCodeColumn");

    for (int i = 0; i < myLineCount; i++) {
      final HorizontalPanel panel = new HorizontalPanel();

      InlineHTML lineSpan = new InlineHTML(String.valueOf(i + 1));
      lineSpan.addStyleName("editorLine");
      lineSpan.addStyleName("editorGutterLine" + i);

      lineSpan.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          panel.add(new Image("/icons/debugger/db_set_breakpoint.png"));
        }
      });

      panel.add(lineSpan);

      grid.setWidget(i, 0, panel);
    }

    String[] split = s.split("\n");
    for (int i = 0; i < split.length; i++) {
      final String line = split[i];


      final InlineHTML lineSpan = new InlineHTML(line);
      lineSpan.setWidth("100%");
      lineSpan.addStyleName("editorLine");
      lineSpan.addStyleName("editorCodeLine" + i);

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
      }) ;   */

      grid.setWidget(i, 1, lineSpan);
    }

    CustomScrollPanel scrollPanel = new CustomScrollPanel(grid);
    scrollPanel.setAlwaysShowScrollBars(true);
    scrollPanel.setHeight("100%");
    scrollPanel.setWidth("100%");

    DockPanel panel = new DockPanel();
    panel.add(grid, DockPanel.CENTER);

    VerticalPanel markerPanel = new VerticalPanel();
    markerPanel.add(new HTML("1"));
    markerPanel.add(new HTML("2"));
    markerPanel.add(new HTML("3"));

    markerPanel.setWidth("16px");
    markerPanel.setHeight("100%");

    panel.add(markerPanel, DockPanel.EAST);

    myPanel.setWidget(panel);
  }

  public Widget getComponent() {
    return myPanel;
  }

  public void addHighlightInfos(List<GwtHighlightInfo> result) {
    for (GwtHighlightInfo highlightInfo : result) {
      myBuilder.addHighlight(highlightInfo);
    }
    update();
  }
}
