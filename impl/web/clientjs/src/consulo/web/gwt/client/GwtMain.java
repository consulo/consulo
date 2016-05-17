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
package consulo.web.gwt.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.transport.GwtHighlightInfo;
import consulo.web.gwt.client.transport.GwtVirtualFile;
import consulo.web.gwt.client.ui.Editor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 15-May-16
 */
public class GwtMain implements EntryPoint {
  private Map<String, Integer> opened = new HashMap<String, Integer>();

  @Override
  public void onModuleLoad() {
    final VerticalPanel globalPanel = new VerticalPanel();
    globalPanel.setWidth("100%");
    globalPanel.setHeight(Window.getClientHeight() + "px");
    Window.addResizeHandler(new ResizeHandler() {
      @Override
      public void onResize(ResizeEvent event) {
        int height = event.getHeight();
        globalPanel.setHeight(height + "px");
      }
    });

    final GwtTransportServiceAsync serviceAsync = GWT.create(GwtTransportService.class);

    final TabPanel tabPanel = new TabPanel();

    HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
    splitPanel.setSplitPosition("20%");

    final Tree tree = new Tree();
    serviceAsync.getProjectDirectory(new AsyncCallback<GwtVirtualFile>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert(caught.getMessage());
      }

      @Override
      public void onSuccess(GwtVirtualFile result) {
        addNodes(tree, result);
      }
    });

    splitPanel.add(tree);

    splitPanel.add(tabPanel);

    tree.addSelectionHandler(new SelectionHandler<TreeItem>() {
      @Override
      public void onSelection(SelectionEvent<TreeItem> event) {
        TreeItem selectedItem = event.getSelectedItem();

        final GwtVirtualFile virtualFile = (GwtVirtualFile)selectedItem.getUserObject();
        if (virtualFile.isDirectory()) {
          boolean state = selectedItem.getState();
          selectedItem.setState(!state);
          return;
        }

        Integer indexOfOpened = opened.get(virtualFile.getUrl());
        if (indexOfOpened != null) {
          tabPanel.selectTab(indexOfOpened);
          return;
        }

        serviceAsync.getContent(virtualFile.getUrl(), new AsyncCallback<String>() {
          @Override
          public void onFailure(Throwable caught) {
            Window.alert(caught.getMessage());
          }

          @Override
          public void onSuccess(String result) {
            if (result == null) {
              return;
            }

            String tabText = "<img src=\"/icons/" + virtualFile.getIcon() + "\">" + virtualFile.getName();

            final Editor editor = new Editor(result);
            editor.update();

            tabPanel.add(editor.getComponent(), tabText, true);
            int index = tabPanel.getWidgetCount() - 1;
            tabPanel.selectTab(index);
            opened.put(virtualFile.getUrl(), index);

            serviceAsync.getLexerHighlight(virtualFile.getUrl(), new AsyncCallback<List<GwtHighlightInfo>>() {
              @Override
              public void onFailure(Throwable caught) {

              }

              @Override
              public void onSuccess(List<GwtHighlightInfo> result) {
                editor.addHighlightInfos(result);

                runHighlightPasses(serviceAsync, virtualFile, editor);
              }
            });
          }
        });
      }
    });

    globalPanel.add(splitPanel);

    RootPanel.get().add(globalPanel);
  }

  private void runHighlightPasses(GwtTransportServiceAsync serviceAsync, GwtVirtualFile virtualFile, final Editor editor) {
    serviceAsync.runHighlightPasses(virtualFile.getUrl(), new AsyncCallback<List<GwtHighlightInfo>>() {
      @Override
      public void onFailure(Throwable caught) {
        Window.alert(caught.getMessage());
      }

      @Override
      public void onSuccess(List<GwtHighlightInfo> result) {
        editor.addHighlightInfos(result);
      }
    });
  }

  private static void addNodes(HasTreeItems parent, GwtVirtualFile virtualFile) {
    HorizontalPanel panel = new HorizontalPanel();
    panel.add(new Image("/icons/" + virtualFile.getIcon()));
    panel.add(new HTML(virtualFile.getName()));

    TreeItem item = new TreeItem(panel);
    item.setUserObject(virtualFile);

    for (GwtVirtualFile child : virtualFile.getChildren()) {
      addNodes(item, child);
    }

    if (parent instanceof Tree) {
      item.setState(true);
    }
    parent.addItem(item);
  }
}
