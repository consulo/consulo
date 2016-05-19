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

import com.github.gwtbootstrap.client.ui.TabLink;
import com.github.gwtbootstrap.client.ui.TabPane;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineHTML;
import com.google.gwt.user.client.ui.SimplePanel;
import consulo.web.gwt.client.util.GwtUtil;
import consulo.web.gwt.client.util.ReportableCallable;
import consulo.web.gwt.shared.transport.GwtHighlightInfo;
import consulo.web.gwt.shared.transport.GwtVirtualFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 19-May-16
 */
public class EditorTabPanel extends SimplePanel {
  public class EditorTabInfo {
    private Editor myEditor;
    private int myIndex;

    public EditorTabInfo(Editor editor, int index) {

      myEditor = editor;
      myIndex = index;
    }
  }

  private Map<String, EditorTabInfo> myOpenedFiles = new HashMap<String, EditorTabInfo>();
  private final com.github.gwtbootstrap.client.ui.TabPanel myTabPanel = new com.github.gwtbootstrap.client.ui.TabPanel();

  public EditorTabPanel() {
    setWidget(myTabPanel);
  }

  public void openFileInEditor(final GwtVirtualFile virtualFile, final int offset) {
    EditorTabInfo editorTab = myOpenedFiles.get(virtualFile.getUrl());
    if (editorTab != null) {
      myTabPanel.selectTab(editorTab.myIndex);

      editorTab.myEditor.setCaretOffset(offset);
      return;
    }

    GwtUtil.rpc().getContent(virtualFile.getUrl(), new ReportableCallable<String>() {
      @Override
      public void onSuccess(String result) {
        if (result == null) {
          return;
        }

        final Editor editor = new Editor(EditorTabPanel.this, virtualFile.getUrl(), result);

        final TabLink tabLink = new TabLink();
        final HorizontalPanel tab = new HorizontalPanel();
        tab.add(GwtUtil.icon(virtualFile.getIconLayers()));
        InlineHTML span = new InlineHTML(virtualFile.getName());
        span.setStyleName("textAfterIcon18");
        tab.add(span);
        Image closeImage = new Image("/icons/actions/closeNew.png");

        tab.add(closeImage);

        tabLink.add(tab);

        myTabPanel.add(tabLink);

        TabPane tabPane = tabLink.getTabPane();
        tabPane.setHeight("100%");
        tabPane.setWidth("100%");
        tabPane.addStyleName("disableOverflow");

        tabPane.add(editor.getComponent());

        // TabPanel can't return tab size???
        int index = myOpenedFiles.size();
        myTabPanel.selectTab(index);
        editor.focusOffset(offset);

        myOpenedFiles.put(virtualFile.getUrl(), new EditorTabInfo(editor, index));

        closeImage.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            myOpenedFiles.remove(virtualFile.getUrl());

            myTabPanel.remove(tabLink);

            int size = myOpenedFiles.size();
            if (size > 0) {
              myTabPanel.selectTab(size - 1);
            }
          }
        });

        GwtUtil.rpc().getLexerHighlight(virtualFile.getUrl(), new ReportableCallable<List<GwtHighlightInfo>>() {
          @Override
          public void onSuccess(List<GwtHighlightInfo> result) {
            editor.addHighlightInfos(result, Editor.ourLexerFlag);

            runHighlightPasses(virtualFile, editor, offset, null);

            editor.setCaretHandler(new EditorCaretHandler() {
              @Override
              public void caretPlaced(int offset) {
                runHighlightPasses(virtualFile, editor, offset, null);
              }
            });
          }
        });
      }
    });
  }

  public static void runHighlightPasses(GwtVirtualFile virtualFile, final Editor editor, int offset, final Runnable callback) {
    GwtUtil.rpc().runHighlightPasses(virtualFile.getUrl(), offset, new ReportableCallable<List<GwtHighlightInfo>>() {
      @Override
      public void onSuccess(List<GwtHighlightInfo> result) {
        editor.addHighlightInfos(result, Editor.ourEditorFlag);

        if (callback != null) {
          callback.run();
        }
      }
    });
  }
}
