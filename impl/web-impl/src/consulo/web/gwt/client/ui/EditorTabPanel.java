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
package consulo.web.gwt.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.client.util.GwtUtil;
import consulo.web.gwt.client.util.ReportableCallable;
import consulo.web.gwt.shared.transport.GwtVirtualFile;

import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 19-May-16
 */
public class EditorTabPanel extends TabPanel {
  private Map<String, Editor> myOpenedFiles = new HashMap<String, Editor>();

  public void openFileInEditor(final GwtVirtualFile virtualFile, final int offset) {
    Editor editorTab = myOpenedFiles.get(virtualFile.getUrl());
    if (editorTab != null) {
      final int index = getWidgetIndex(editorTab);

      selectTab(index);

      if(offset != -1) {
        editorTab.setCaretOffset(offset);
      }
      return;
    }

    GwtUtil.rpc().getContent(virtualFile.getUrl(), new ReportableCallable<String>() {
      @Override
      public void onSuccess(String result) {
        if (result == null) {
          return;
        }

        final Editor editor = new Editor(EditorTabPanel.this, virtualFile.getUrl(), result);

        final HorizontalPanel tabHeader = new HorizontalPanel();
        tabHeader.add(GwtUIUtil.icon(virtualFile.getIconLayers()));
        InlineHTML span = new InlineHTML(virtualFile.getName());
        span.setStyleName("textAfterIcon18");
        tabHeader.add(span);
        Image closeImage = new Image("/icons/actions/closeNew.png");

        tabHeader.add(closeImage);

        add(editor, tabHeader);

        int index = myOpenedFiles.size();
        selectTab(index);
        if(offset != -1) {
          editor.focusOffset(offset);
        }

        myOpenedFiles.put(virtualFile.getUrl(), editor);

        closeImage.addClickHandler(new ClickHandler() {
          @Override
          public void onClick(ClickEvent event) {
            Editor tabInfo = myOpenedFiles.remove(virtualFile.getUrl());
            if (tabInfo != null) {
              tabInfo.dispose();
            }

            remove(editor);

            int size = myOpenedFiles.size();
            if (size > 0) {
              selectTab(size - 1);
            }
          }
        });
      }
    });
  }
}
