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
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.service.EditorColorSchemeService;
import consulo.web.gwt.client.util.GwtStyleUtil;
import consulo.web.gwt.client.util.GwtUtil;
import consulo.web.gwt.client.util.Log;
import consulo.web.gwt.client.util.ReportableCallable;
import consulo.web.gwt.shared.transport.*;

import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 17-May-16
 */
public class Editor extends SimplePanel implements WidgetWithUpdateUI {
  private static class CodeLinePanel extends FlowPanel implements WidgetWithUpdateUI {
    private Editor myEditor;

    public CodeLinePanel(Editor editor) {
      myEditor = editor;
      sinkEvents(Event.ONCLICK);

      updateUI();
    }

    @Override
    public void onBrowserEvent(Event event) {
      switch (DOM.eventGetType(event)) {
        case Event.ONCLICK:
          myEditor.changeLine(this);
          break;
        default:
          event.preventDefault();
          break;
      }
    }

    @Override
    public void updateUI() {
      GwtEditorColorScheme scheme = myEditor.getScheme();
      if (myEditor.myCurrentLinePanel == this) {
        getElement().getStyle().setBackgroundColor(GwtStyleUtil.toString(scheme.getColor(GwtEditorColorScheme.CARET_ROW_COLOR)));
      }
      else {
        myEditor.setDefaultTextColors(this);
      }
    }
  }

  public static class LineNumberSpan extends InlineHTML implements WidgetWithUpdateUI {
    private Editor myEditor;

    public LineNumberSpan(String html, Editor editor) {
      super(html);
      myEditor = editor;

      updateUI();
    }

    @Override
    public void updateUI() {
      Log.log("MainGrid.LineNumberSpan");

      GwtEditorColorScheme scheme = myEditor.getScheme();

      getElement().getStyle().setColor(GwtStyleUtil.toString(scheme.getColor(GwtEditorColorScheme.LINE_NUMBERS_COLOR)));
    }
  }

  public static class MainGrid extends Grid implements WidgetWithUpdateUI {
    private Editor myEditor;

    public MainGrid(Editor editor, int rows, int columns) {
      super(rows, columns);
      myEditor = editor;

      updateUI();
    }

    @Override
    public void updateUI() {
      Log.log("MainGrid.updateUI");
      myEditor.setDefaultTextColors(this);
    }
  }

  public static class GutterPanel extends VerticalPanel implements WidgetWithUpdateUI {
    private Editor myEditor;

    public GutterPanel(Editor editor) {
      myEditor = editor;

      updateUI();
    }

    @Override
    public void updateUI() {
      GwtEditorColorScheme scheme = myEditor.getScheme();

      getElement().getStyle().setProperty("borderRightColor", GwtStyleUtil.toString(scheme.getColor(GwtEditorColorScheme.TEARLINE_COLOR)));
      getElement().getStyle().setProperty("borderRightStyle", "solid");
      getElement().getStyle().setProperty("borderRightWidth", "1px");
      getElement().getStyle().setWhiteSpace(Style.WhiteSpace.NOWRAP);
      getElement().getStyle().setBackgroundColor(GwtStyleUtil.toString(scheme.getColor(GwtEditorColorScheme.GUTTER_BACKGROUND)));
    }
  }

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

  private CodeLinePanel myCurrentLinePanel;

  private GwtEditorColorScheme myScheme;

  private EditorColorSchemeService.Listener myListener = new EditorColorSchemeService.Listener() {
    @Override
    public void schemeChanged(GwtEditorColorScheme scheme) {
      myScheme = scheme;

      Log.log("scheme changed to " + scheme.getName());

      Scheduler.get().scheduleDeferred(new Command() {
        @Override
        public void execute() {
          GwtUtil.updateUI(Editor.this);

          doHighlightImpl();
        }
      });
    }
  };

  public Editor(EditorTabPanel editorTabPanel, String fileUrl, String text) {
    myEditorTabPanel = editorTabPanel;
    myFileUrl = fileUrl;
    myBuilder = new EditorSegmentBuilder(text);
    myLineCount = myBuilder.getLineCount();

    sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS);

    final EditorColorSchemeService schemeService = GwtUtil.get(EditorColorSchemeService.KEY);
    schemeService.addListener(myListener);

    myScheme = schemeService.getScheme();

    setDefaultTextColors(this);
  }

  public void doHighlight() {
    Scheduler.get().scheduleDeferred(new Command() {
      @Override
      public void execute() {
        doHighlightImpl();
      }
    });
  }

  private void doHighlightImpl() {
    GwtUtil.rpc().getLexerHighlight(myFileUrl, new ReportableCallable<List<GwtHighlightInfo>>() {
      @Override
      public void onSuccess(List<GwtHighlightInfo> result) {
        addHighlightInfos(result, Editor.ourLexerFlag);

        runHighlightPasses(myLastCaretOffset, null);

        setCaretHandler(new EditorCaretHandler() {
          @Override
          public void caretPlaced(int offset) {
            runHighlightPasses(offset, null);
          }
        });
      }
    });
  }

  private void runHighlightPasses(int offset, final Runnable callback) {
    GwtUtil.rpc().runHighlightPasses(myFileUrl, offset, new ReportableCallable<List<GwtHighlightInfo>>() {
      @Override
      public void onSuccess(List<GwtHighlightInfo> result) {
        addHighlightInfos(result, Editor.ourEditorFlag);

        if (callback != null) {
          callback.run();
        }
      }
    });
  }

  @Override
  public void updateUI() {
    // update main panel
    setDefaultTextColors(this);

    // update current line
    if (myCurrentLinePanel != null) {
      myCurrentLinePanel.updateUI();
    }
  }

  private void setDefaultTextColors(Widget widget) {
    GwtTextAttributes textAttr = myScheme.getAttributes(GwtEditorColorScheme.TEXT);
    if (textAttr != null) {
      GwtColor background = textAttr.getBackground();
      if (background != null) {
        widget.getElement().getStyle().setBackgroundColor(GwtStyleUtil.toString(background));
      }
      else {
        widget.getElement().getStyle().clearBackgroundColor();
      }

      GwtColor foreground = textAttr.getForeground();
      if (foreground != null) {
        widget.getElement().getStyle().setColor(GwtStyleUtil.toString(foreground));
      }
      else {
        widget.getElement().getStyle().clearColor();
      }
    }
  }

  public void dispose() {
    final EditorColorSchemeService schemeService = GwtUtil.get(EditorColorSchemeService.KEY);
    schemeService.removeListener(myListener);
  }

  @Override
  public void onBrowserEvent(final Event event) {
    switch (DOM.eventGetType(event)) {
      case Event.ONMOUSEOVER:
        com.google.gwt.dom.client.Element element = DOM.eventGetToElement(event);

        Object range = element == null ? null : element.getPropertyObject("range");
        if (!(range instanceof GwtTextRange)) {
          myLastCaretOffset = -1;
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

              GwtTextRange resultElementRange = result.getRange();
              if (myLastCursorPsiElementTextRange != null && myLastCursorPsiElementTextRange.containsRange(resultElementRange)) {
                return;
              }
              myLastCursorPsiElementTextRange = resultElementRange;
              GwtHighlightInfo highlightInfo =
                      new GwtHighlightInfo(myScheme.getAttributes(GwtEditorColorScheme.HYPERLINK_ATTRIBUTES), resultElementRange, Integer.MAX_VALUE);

              myLastNavigationInfo = result;

              addHighlightInfos(Arrays.asList(highlightInfo), ourSelectFlag);
            }
          });
        }
        event.preventDefault();
        break;
      case Event.ONMOUSEOUT:
        onMouseOut();
        event.preventDefault();
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
          if (myCaretHandler != null) {
            myCaretHandler.caretPlaced(myLastCaretOffset);
          }
        }
        event.preventDefault();
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
    Grid gridPanel = GwtUtil.fillAndReturn(new MainGrid(this, 1, 2));

    // try to fill area by code
    gridPanel.getColumnFormatter().getElement(1).getStyle().setWidth(100, Style.Unit.PCT);

    GutterPanel editorLinePanel = new GutterPanel(this);
    gridPanel.setWidget(0, 0, editorLinePanel);

    editorLinePanel.addStyleName("noselectable");

    for (int i = 0; i < myLineCount; i++) {
      final Grid panel = GwtUtil.fillAndReturn(new Grid(1, 5)); // 5 fake size
      // place lines to right
      panel.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_RIGHT);

      panel.getCellFormatter().getElement(0, 0).getStyle().setPaddingLeft(5, Style.Unit.PX);
      panel.getCellFormatter().getElement(0, 4).getStyle().setPaddingRight(5, Style.Unit.PX);

      // try fill line number as primary panel
      panel.getColumnFormatter().getElement(0).getStyle().setWidth(100, Style.Unit.PCT);

      LineNumberSpan lineSpan = new LineNumberSpan(String.valueOf(i + 1), this);
      lineSpan.addStyleName("editorLine");
      lineSpan.addStyleName("editorGutterLine" + i);

      panel.setWidget(0, 0, lineSpan);

      editorLinePanel.add(panel);
    }

    VerticalPanel editorCodePanel = new VerticalPanel() {
      {
        sinkEvents(Event.ONCHANGE | Event.ONPASTE | Event.KEYEVENTS);
      }

      @Override
      public void onBrowserEvent(Event event) {
        event.preventDefault();
      }
    };
    gridPanel.setWidget(0, 1, editorCodePanel);

    GwtUtil.fill(editorCodePanel);

    // dont provide red code
    editorCodePanel.getElement().setAttribute("spellcheck", "false");
    // editable
    editorCodePanel.getElement().setAttribute("contenteditable", "true");
    // disable border
    editorCodePanel.addStyleName("noFocusBorder");

    int lineCount = 0;
    FlowPanel lineElement = null;

    for (EditorSegmentBuilder.Fragment fragment : myBuilder.getFragments()) {
      if (lineElement == null) {
        lineElement = new CodeLinePanel(this);
        setDefaultTextColors(lineElement);

        lineElement.setWidth("100%");
        lineElement.addStyleName("editorLine");
        lineElement.addStyleName("gen_Line_" + lineCount);
      }

      lineElement.add(fragment.widget);

      if (fragment.lineWrap) {
        editorCodePanel.add(lineElement);
        lineElement = null;

        lineCount++;
      }
    }

    ScrollPanel scrollPanel = new ScrollPanel(gridPanel);
    GwtUtil.fill(scrollPanel);

    DockPanel panel = new DockPanel();
    panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
    panel.setWidth("100%");
    panel.add(scrollPanel, DockPanel.CENTER);

    setWidget(panel);
  }

  public GwtEditorColorScheme getScheme() {
    return myScheme;
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

  public void setCaretOffset(int offset) {
    myLastCaretOffset = offset;

    focusOffset(offset);

    if (myCaretHandler != null) {
      myCaretHandler.caretPlaced(myLastCaretOffset);
    }
  }

  public void focusOffset(int offset) {
    myLastCaretOffset = offset;
    for (EditorSegmentBuilder.Fragment fragment : myBuilder.getFragments()) {
      if (fragment.range.containsRange(offset, offset)) {
        fragment.widget.getElement().focus();
        fragment.widget.getElement().scrollIntoView();

        set(fragment.widget.getElement());
        Widget parent = fragment.widget.getParent();
        if (parent instanceof CodeLinePanel) {
          changeLine((CodeLinePanel)parent);
        }

        break;
      }
    }
  }

  public void changeLine(CodeLinePanel widget) {
    if (myCurrentLinePanel == widget) {
      return;
    }

    if (myCurrentLinePanel != null) {
      setDefaultTextColors(myCurrentLinePanel);
    }

    widget.updateUI();
    myCurrentLinePanel = widget;
  }

  public native void set(Element element) /*-{
    var range = $doc.createRange();
    var sel = $wnd.getSelection();

    range.setStart(element, 1);
    range.setEnd(element, 1);
    range.collapse(true);
    sel.removeAllRanges();
    sel.addRange(range);
    element.focus();
  }-*/;
}
