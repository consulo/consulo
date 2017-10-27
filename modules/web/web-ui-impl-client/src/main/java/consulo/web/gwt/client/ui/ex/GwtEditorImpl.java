/*
 * Copyright 2013-2016 consulo.io
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
package consulo.web.gwt.client.ui.ex;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.ui.EditorSegmentBuilder;
import consulo.web.gwt.client.ui.WidgetWithUpdateUI;
import consulo.web.gwt.client.util.GwtStyleUtil;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.client.util.GwtUtil;
import consulo.web.gwt.client.util.ReportableCallable;
import consulo.web.gwt.shared.transport.*;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 17-May-16
 */
public class GwtEditorImpl extends SimplePanel {
  private static class CodeLinePanel extends FlowPanel implements WidgetWithUpdateUI {
    private GwtEditorImpl myEditor;
    private int myLine;

    public CodeLinePanel(GwtEditorImpl editor, int line) {
      myEditor = editor;
      myLine = line;

      sinkEvents(Event.ONCLICK);
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
      if (Boolean.TRUE) {
        return;
      }
      Element parentElement = getElement().getParentElement();

      GwtEditorColorScheme scheme = myEditor.getScheme();
      if (myEditor.myCurrentLinePanel == this) {

        // we need change color td element, due we have padding
        parentElement.getStyle().setBackgroundColor(GwtStyleUtil.toString(scheme.getColor(GwtEditorColorScheme.CARET_ROW_COLOR)));
      }
      else {
        myEditor.setDefaultTextColors(parentElement);
      }
    }

    public int getLine() {
      return myLine;
    }
  }

  public static class LineNumberSpan extends InlineHTML implements WidgetWithUpdateUI {
    private GwtEditorImpl myEditor;

    public LineNumberSpan(String html, GwtEditorImpl editor) {
      super(html);
      myEditor = editor;

      updateUI();
    }

    @Override
    public void updateUI() {
      if (Boolean.TRUE) {
        return;
      }
      GwtEditorColorScheme scheme = myEditor.getScheme();

      getElement().getStyle().setColor(GwtStyleUtil.toString(scheme.getColor(GwtEditorColorScheme.LINE_NUMBERS_COLOR)));
    }
  }

  public static class MainGrid extends Grid implements WidgetWithUpdateUI {
    private GwtEditorImpl myEditor;

    public MainGrid(GwtEditorImpl editor, int rows, int columns) {
      super(rows, columns);
      myEditor = editor;

      updateUI();
    }

    @Override
    public void updateUI() {
      myEditor.setDefaultTextColors(this);
    }
  }

  public static class GutterPanel extends Grid implements WidgetWithUpdateUI {
    private GwtEditorImpl myEditor;

    public GutterPanel(int lineCount, GwtEditorImpl editor) {
      super(lineCount + 1, 1);
      myEditor = editor;

      // dummy element - fill free space with same background and resize it
      set(lineCount, new InlineHTML("&#8205;"));

      getCellFormatter().getElement(lineCount, 0).getStyle().setHeight(100, Style.Unit.PCT);

      updateUI();
    }

    public void set(int row, Widget widget) {
      setWidget(row, 0, widget);
    }

    @Override
    public void updateUI() {
      if (Boolean.TRUE) {
        return;
      }
      GwtEditorColorScheme scheme = myEditor.getScheme();

      getElement().getStyle().setProperty("borderRightColor", GwtStyleUtil.toString(scheme.getColor(GwtEditorColorScheme.TEARLINE_COLOR)));
      getElement().getStyle().setProperty("borderRightStyle", "solid");
      getElement().getStyle().setProperty("borderRightWidth", "1px");
      getElement().getStyle().setWhiteSpace(Style.WhiteSpace.NOWRAP);
      getElement().getStyle().setBackgroundColor(GwtStyleUtil.toString(scheme.getColor(GwtEditorColorScheme.GUTTER_BACKGROUND)));
    }
  }

  public static class GutterLineGrid extends Grid implements WidgetWithUpdateUI {
    private GwtEditorImpl myEditor;
    private int myLine;

    public GutterLineGrid(int rows, int columns, GwtEditorImpl editor, int line) {
      super(rows, columns);
      myEditor = editor;
      myLine = line;
    }

    @Override
    public void updateUI() {
      if (Boolean.TRUE) {
        return;
      }
      final Style style = getElement().getStyle();
      GwtEditorColorScheme scheme = myEditor.getScheme();
      if (myEditor.myCurrentLinePanel != null && myEditor.myCurrentLinePanel.myLine == myLine) {
        style.setBackgroundColor(GwtStyleUtil.toString(scheme.getColor(GwtEditorColorScheme.CARET_ROW_COLOR)));
      }
      else {
        style.setBackgroundColor(GwtStyleUtil.toString(scheme.getColor(GwtEditorColorScheme.GUTTER_BACKGROUND)));
      }
    }
  }

  public static final int ourLexerFlag = 1 << 1;
  public static final int ourEditorFlag = 1 << 2;

  public static final int ourSelectFlag = 1 << 24;

  @Nullable
  private EditorSegmentBuilder myBuilder;

  private int myLineCount;

  private int myDelayedCaredOffset = -1;

  private int myLastCaretOffset = -1;

  private String myFileUrl;

  private GwtTextRange myLastCursorPsiElementTextRange;

  private GwtNavigateInfo myLastNavigationInfo;

  private CodeLinePanel myCurrentLinePanel;

  private GwtEditorColorScheme myScheme;

  private GutterPanel myGutterPanel;

  private boolean myInsideGutter;

  private DecoratedPopupPanel myLastTooltip;

  private GwtTextRange myLastTooltipRange;

  enum HighlightState {
    UNKNOWN,
    LEXER,
    PASS
  }

  private HighlightState myHighlightState = HighlightState.UNKNOWN;

  /*private EditorColorSchemeService.Listener myListener = new EditorColorSchemeService.Listener() {
    @Override
    public void schemeChanged(GwtEditorColorScheme scheme) {
      myScheme = scheme;

      Scheduler.get().scheduleDeferred(new Command() {
        @Override
        public void execute() {
          GwtUIUtil.updateUI(GwtEditorImpl.this);

          doHighlightImpl();
        }
      });
    }
  };*/

  public GwtEditorImpl() {
    sinkEvents(Event.ONCLICK | Event.MOUSEEVENTS | Event.ONKEYUP);

    String text = "some text\nvfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv\n" +
                  "vfdsavfdasvfsdavafv";

    myScheme = new GwtEditorColorScheme();
    setDefaultTextColors(this);

    addStyleName("scroll");

    GwtUIUtil.fill(this);

    setWidget(GwtUIUtil.loadingPanelDeprecated());

    Scheduler.get().scheduleDeferred(new Command() {
      @Override
      public void execute() {
        myBuilder = new EditorSegmentBuilder(text);
        myLineCount = myBuilder.getLineCount();

        setWidget(build());

        if (myDelayedCaredOffset != -1) {
          focusOffset(myDelayedCaredOffset);
          myDelayedCaredOffset = -1;
        }

        //doHighlightImpl();
      }
    });
  }

  private void doHighlightImpl() {
    GwtUtil.rpc().getLexerHighlight(myFileUrl, new ReportableCallable<List<GwtHighlightInfo>>() {
      @Override
      public void onSuccess(List<GwtHighlightInfo> result) {
        addHighlightInfos(result, GwtEditorImpl.ourLexerFlag);

        myHighlightState = HighlightState.LEXER;

        runHighlightPasses(myLastCaretOffset, null);

        myHighlightState = HighlightState.PASS;
      }
    });
  }

  private void runHighlightPasses(int offset, final Runnable callback) {
    GwtUtil.rpc().runHighlightPasses(myFileUrl, offset, new ReportableCallable<List<GwtHighlightInfo>>() {
      @Override
      public void onSuccess(List<GwtHighlightInfo> result) {
        addHighlightInfos(result, GwtEditorImpl.ourEditorFlag);

        if (callback != null) {
          callback.run();
        }
      }
    });
  }

  public void updateUI() {
    // update main panel
    setDefaultTextColors(this);

    // update current line
    if (myCurrentLinePanel != null) {
      myCurrentLinePanel.updateUI();
    }
  }

  private void setDefaultTextColors(Widget widget) {
    setDefaultTextColors(widget.getElement());
  }

  private void setDefaultTextColors(Element element) {
    if (Boolean.TRUE) {
      return;
    }
    GwtTextAttributes textAttr = myScheme.getAttributes(GwtEditorColorScheme.TEXT);
    if (textAttr != null) {
      GwtColor background = textAttr.getBackground();
      if (background != null) {
        element.getStyle().setBackgroundColor(GwtStyleUtil.toString(background));
      }
      else {
        element.getStyle().clearBackgroundColor();
      }

      GwtColor foreground = textAttr.getForeground();
      if (foreground != null) {
        element.getStyle().setColor(GwtStyleUtil.toString(foreground));
      }
      else {
        element.getStyle().clearColor();
      }
    }
  }

  public void dispose() {

  }

  private boolean insideGutter(Element element) {
    Element temp = element;
    while (temp != null) {
      // if we entered editor element
      if (temp == getElement()) {
        break;
      }

      if (myGutterPanel.getElement() == temp) {
        getElement().getStyle().setCursor(Style.Cursor.DEFAULT);
        return true;
      }

      temp = temp.getParentElement();
    }

    return false;
  }

  @Override
  public void onBrowserEvent(final Event event) {
    if (myBuilder == null) {
      return;
    }

    switch (DOM.eventGetType(event)) {
      case Event.ONMOUSEOVER: {
        com.google.gwt.dom.client.Element element = DOM.eventGetToElement(event);

        myInsideGutter = insideGutter(element);
        if (myInsideGutter) {
          return;
        }

        Object range = element == null ? null : element.getPropertyObject("range");
        if (!(range instanceof GwtTextRange)) {
          return;
        }

        final int startOffset = ((GwtTextRange)range).getStartOffset();

        final Widget widget = (Widget)element.getPropertyObject("widget");

        if (event.getCtrlKey()) {
          if (myHighlightState == HighlightState.UNKNOWN) {
            return;
          }
          GwtUtil.rpc().getNavigationInfo(myFileUrl, startOffset, new ReportableCallable<GwtNavigateInfo>() {
            @Override
            public void onSuccess(GwtNavigateInfo result) {
              if (result == null) {
                return;
              }

              GwtTextRange resultElementRange = result.getRange();
              if (myLastCursorPsiElementTextRange != null && myLastCursorPsiElementTextRange.containsRange(resultElementRange)) {
                return;
              }

              getElement().getStyle().setCursor(Style.Cursor.POINTER);

              if (result.getDocText() != null) {
                myLastTooltipRange = myLastCursorPsiElementTextRange;
                showTooltip(widget, result.getDocText());
              }
              else {
                removeTooltip();
              }

              myLastCursorPsiElementTextRange = resultElementRange;
              GwtHighlightInfo highlightInfo = new GwtHighlightInfo(myScheme.getAttributes(GwtEditorColorScheme.CTRL_CLICKABLE), resultElementRange, Integer.MAX_VALUE);

              myLastNavigationInfo = result;

              addHighlightInfos(Arrays.asList(highlightInfo), ourSelectFlag);
            }
          });
        }
        else {
          if (widget instanceof EditorSegmentBuilder.CharSpan) {
            myLastTooltipRange = ((EditorSegmentBuilder.CharSpan)widget).range;
            String toolTip = ((EditorSegmentBuilder.CharSpan)widget).getToolTip();
            if (toolTip != null) {
              showTooltip(widget, toolTip);
            }
          }
          else {
            myLastTooltipRange = null;

            removeTooltip();
          }
        }
        event.preventDefault();
        break;
      }
      case Event.ONKEYUP:
        if (event.getKeyCode() == KeyCodes.KEY_CTRL) {
          removeTooltip();
        }
        break;
      case Event.ONMOUSEOUT: {
        boolean old = myInsideGutter;
        myInsideGutter = insideGutter(DOM.eventGetToElement(event));
        if (old != myInsideGutter) {
          getElement().getStyle().clearCursor();
        }
        if (myInsideGutter) {
          event.preventDefault();
          return;
        }

        com.google.gwt.dom.client.Element element = DOM.eventGetToElement(event);
        GwtTextRange range = element == null ? null : (GwtTextRange)element.getPropertyObject("range");

        if (range == null || myLastTooltipRange != null && !myLastTooltipRange.containsRange(range)) {
          removeTooltip();
        }

        onMouseOut();
        event.preventDefault();
        break;
      }
      case Event.ONCLICK: {
        if (myInsideGutter) {
          event.preventDefault();
          return;
        }

        com.google.gwt.dom.client.Element element = DOM.eventGetTarget(event);

        int offset = 0;
        Object spanRange = element.getPropertyObject("range");
        if (spanRange != null) {
          offset = ((GwtTextRange)spanRange).getStartOffset();
        }
        else {
          Object lineRange = element.getPropertyObject("lineRange");
          if (lineRange != null) {
            offset = ((GwtTextRange)lineRange).getStartOffset();
          }
        }
        if (offset == myLastCaretOffset) {
          return;
        }

        myLastCaretOffset = offset;

        if (event.getCtrlKey()) {
          if (myLastNavigationInfo != null) {
            List<GwtNavigatable> navigates = myLastNavigationInfo.getNavigates();

            GwtNavigatable navigatable = navigates.get(0);

            onMouseOut();

            //myEditorTabPanel.openFileInEditor(navigatable.getFile(), navigatable.getOffset());
          }
        }
        else {
          onOffsetChangeImpl();
        }
        event.preventDefault();
        break;
      }
      default:
        super.onBrowserEvent(event);
        break;
    }
  }

  private void onOffsetChangeImpl() {
    if (myLastCaretOffset == -1 || myHighlightState != HighlightState.PASS) {
      return;
    }

    runHighlightPasses(myLastCaretOffset, null);
  }

  private void onMouseOut() {
    if (myBuilder == null) {
      return;
    }

    if (myLastCursorPsiElementTextRange != null) {
      getElement().getStyle().clearCursor();

      myBuilder.removeHighlightByRange(myLastCursorPsiElementTextRange, ourSelectFlag);

      myLastCursorPsiElementTextRange = null;
      myLastNavigationInfo = null;
    }
  }

  private Widget build() {
    Grid gridPanel = GwtUIUtil.fillAndReturn(new MainGrid(this, 1, 2));

    // try to fill area by code
    gridPanel.getColumnFormatter().getElement(1).getStyle().setWidth(100, Style.Unit.PCT);

    gridPanel.getRowFormatter().setVerticalAlign(0, HasVerticalAlignment.ALIGN_TOP);

    myGutterPanel = GwtUIUtil.fillAndReturn(new GutterPanel(myLineCount, this));
    gridPanel.setWidget(0, 0, myGutterPanel);

    myGutterPanel.addStyleName("noselectable");

    for (int i = 0; i < myLineCount; i++) {
      final GutterLineGrid panel = GwtUIUtil.fillAndReturn(new GutterLineGrid(1, 2, this, i));
      panel.updateUI();

      // place lines to right
      panel.getCellFormatter().setHorizontalAlignment(0, 0, HasHorizontalAlignment.ALIGN_RIGHT);

      panel.getCellFormatter().getElement(0, 0).getStyle().setPaddingLeft(5, Style.Unit.PX);
      panel.getCellFormatter().getElement(0, 1).getStyle().setPaddingRight(5, Style.Unit.PX);
      panel.getCellFormatter().getElement(0, 1).addClassName("editorLine");

      // try fill line number as primary panel
      panel.getColumnFormatter().getElement(0).getStyle().setWidth(100, Style.Unit.PCT);

      LineNumberSpan lineSpan = new LineNumberSpan(String.valueOf(i + 1), this);
      lineSpan.addStyleName("editorLine");

      panel.setWidget(0, 0, lineSpan);

      myGutterPanel.set(i, panel);
    }

    Grid editorCodePanel = new Grid(myLineCount + 1, 1) {
      {
        sinkEvents(Event.ONCHANGE | Event.ONPASTE | Event.KEYEVENTS);
      }

      @Override
      public void onBrowserEvent(Event event) {
        int type = DOM.eventGetType(event);
        switch (type) {
          case Event.ONKEYDOWN:
            switch (event.getKeyCode()) {
              case KeyCodes.KEY_B:
                if (event.getCtrlKey()) {
                  GwtUtil.rpc().getNavigationInfo(myFileUrl, myLastCaretOffset, new ReportableCallable<GwtNavigateInfo>() {
                    @Override
                    public void onSuccess(GwtNavigateInfo result) {
                      if (result == null) {
                        return;
                      }

                      GwtNavigatable navigatable = result.getNavigates().get(0);

                      //myEditorTabPanel.openFileInEditor(navigatable.getFile(), navigatable.getOffset());
                    }
                  });
                }
                break;
            }
            break;
        }
        event.preventDefault();
      }
    };

    gridPanel.setWidget(0, 1, editorCodePanel);

    GwtUIUtil.fill(editorCodePanel);
    // dont provide red code
    editorCodePanel.getElement().setAttribute("spellcheck", "false");
    // editable
    editorCodePanel.getElement().setAttribute("contenteditable", "true");
    // disable border
    editorCodePanel.addStyleName("noFocusBorder");

    // inside one row - with fully fill
    editorCodePanel.setWidget(myLineCount, 0, new InlineHTML("&#8205;"));
    editorCodePanel.getCellFormatter().getElement(myLineCount, 0).getStyle().setHeight(100, Style.Unit.PCT);

    int lineCount = 0;
    CodeLinePanel lineElement = null;
    int startOffset = 0;

    for (EditorSegmentBuilder.CharSpan fragment : myBuilder.getFragments()) {
      if (lineElement == null) {
        lineElement = new CodeLinePanel(this, lineCount);
        setDefaultTextColors(lineElement);

        lineElement.setWidth("100%");
        lineElement.addStyleName("editorLine");

        startOffset = fragment.range.getStartOffset();
      }

      lineElement.add(fragment);

      if (fragment.lineWrap) {
        editorCodePanel.getCellFormatter().getElement(lineCount, 0).getStyle().setPaddingLeft(5, Style.Unit.PX);

        editorCodePanel.setWidget(lineCount, 0, lineElement);

        lineElement.getElement().setPropertyObject("lineRange", new GwtTextRange(startOffset, fragment.range.getEndOffset()));
        lineElement.updateUI(); // update after adding

        lineElement = null;

        lineCount++;
      }
    }

    return gridPanel;
  }

  public GwtEditorColorScheme getScheme() {
    return myScheme;
  }

  private void removeTooltip() {
    myLastTooltipRange = null;
    if (myLastTooltip != null) {
      myLastTooltip.hide();
      myLastTooltip = null;
    }
  }

  private void showTooltip(Widget widget, String html) {
    removeTooltip();

    myLastTooltip = new DecoratedPopupPanel(true);
    myLastTooltip.setWidget(new HTML(html));

    int left = widget.getAbsoluteLeft();
    int top = widget.getAbsoluteTop() + 16;
    myLastTooltip.setPopupPosition(left, top);
    myLastTooltip.show();
  }


  public void addHighlightInfos(List<GwtHighlightInfo> result, int flag) {
    if (myBuilder == null) {
      return;
    }
    myBuilder.addHighlights(result, flag);
  }

  public void setCaretOffset(int offset) {
    myLastCaretOffset = offset;

    focusOffset(offset);

    onOffsetChangeImpl();
  }

  public void focusOffset(int offset) {
    if (myBuilder == null) {
      myDelayedCaredOffset = offset;
      return;
    }

    myLastCaretOffset = offset;
    for (EditorSegmentBuilder.CharSpan fragment : myBuilder.getFragments()) {
      if (fragment.range.containsRange(offset, offset)) {
        fragment.getElement().focus();
        fragment.getElement().scrollIntoView();

        set(fragment.getElement());
        Widget parent = fragment.getParent();
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

    CodeLinePanel currentLinePanel = myCurrentLinePanel;
    if (currentLinePanel != null) {
      myCurrentLinePanel = null; // drop current line

      currentLinePanel.updateUI();

      GwtUIUtil.updateUI(myGutterPanel);
    }

    myCurrentLinePanel = widget;

    myCurrentLinePanel.updateUI();
    GwtUIUtil.updateUI(myGutterPanel);
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
