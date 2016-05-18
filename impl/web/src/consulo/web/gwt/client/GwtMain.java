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

import com.github.gwtbootstrap.client.ui.TabLink;
import com.github.gwtbootstrap.client.ui.TabPane;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.transport.GwtHighlightInfo;
import consulo.web.gwt.client.transport.GwtProjectInfo;
import consulo.web.gwt.client.transport.GwtVirtualFile;
import consulo.web.gwt.client.ui.*;
import consulo.web.gwt.shared.GwtTransportService;
import consulo.web.gwt.shared.GwtTransportServiceAsync;
import org.cafesip.gwtcomp.client.ui.SuperTreeItem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 15-May-16
 */
public class GwtMain implements EntryPoint {
  private static final int ourLexerFlag = 1;
  private static final int ourEditorFlag = 2;

  private Map<String, Integer> opened = new HashMap<String, Integer>();

  @Override
  public void onModuleLoad() {
    final SimplePanel globalPanel = new SimplePanel();
    /*globalPanel.setWidth("100%");
    globalPanel.setHeight(Window.getClientHeight() + "px");
    Window.addResizeHandler(new ResizeHandler() {
      @Override
      public void onResize(ResizeEvent event) {
        int height = event.getHeight();
        globalPanel.setHeight(height + "px");
      }
    });*/

    final GwtTransportServiceAsync serviceAsync = GWT.create(GwtTransportService.class);

    final com.github.gwtbootstrap.client.ui.TabPanel tabPanel = new com.github.gwtbootstrap.client.ui.TabPanel();

    consulo.web.gwt.client.ui.HorizontalSplitPanel splitPanel = new consulo.web.gwt.client.ui.HorizontalSplitPanel();
    splitPanel.setSplitPosition("20%");

    final DoubleClickTree tree = new DoubleClickTree();
    serviceAsync.getProjectInfo("ignored", new AsyncCallback<GwtProjectInfo>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(GwtProjectInfo result) {
        addNodes(tree, null, result);
      }
    });

    splitPanel.setLeftWidget(tree);

    splitPanel.setRightWidget(tabPanel);

    tree.addDoubleClickHandler(new DoubleClickTreeHandler() {
      @Override
      public void onDoubleClick(DoubleClickTreeEvent event) {
        TreeItem selectedItem = event.getItem();

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
          }

          @Override
          public void onSuccess(String result) {
            if (result == null) {
              return;
            }

            final Editor editor = new Editor(result);
            editor.repaint();

            final TabLink tabLink = new TabLink();
            final HorizontalPanel tab = new HorizontalPanel();
            tab.add(icon(virtualFile.getIconLayers()));
            InlineHTML span = new InlineHTML(virtualFile.getName());
            span.setStyleName("textAfterIcon18");
            tab.add(span);
            Image closeImage = new Image("/icons/actions/closeNew.png");

            tab.add(closeImage);

            tabLink.add(tab);

            tabPanel.add(tabLink);

            TabPane tabPane = tabLink.getTabPane();
            tabPane.setHeight("100%");
            tabPane.setWidth("100%");
            tabPane.addStyleName("disableOverflow");

            tabPane.add(editor.getComponent());

            // TabPanel can't return tab size???
            int index = opened.size();
            tabPanel.selectTab(index);
            opened.put(virtualFile.getUrl(), index);

            closeImage.addClickHandler(new ClickHandler() {
              @Override
              public void onClick(ClickEvent event) {
                opened.remove(virtualFile.getUrl());

                tabPanel.remove(tabLink);

                int size = opened.size();
                if (size > 0) {
                  tabPanel.selectTab(size - 1);
                }
              }
            });

            serviceAsync.getLexerHighlight(virtualFile.getUrl(), new AsyncCallback<List<GwtHighlightInfo>>() {
              @Override
              public void onFailure(Throwable caught) {

              }

              @Override
              public void onSuccess(List<GwtHighlightInfo> result) {
                editor.addHighlightInfos(result, ourLexerFlag);

                runHighlightPasses(serviceAsync, virtualFile, editor, 0);

                editor.setCaretHandler(new EditorCaretHandler() {
                  @Override
                  public void caretPlaced(int offset) {
                    runHighlightPasses(serviceAsync, virtualFile, editor, offset);
                  }
                });
              }
            });
          }
        });
      }
    });

    globalPanel.add(splitPanel);

    RootPanel.get().add(globalPanel);
  }

  private void runHighlightPasses(GwtTransportServiceAsync serviceAsync, GwtVirtualFile virtualFile, final Editor editor, int offset) {
    serviceAsync.runHighlightPasses(virtualFile.getUrl(), offset, new AsyncCallback<List<GwtHighlightInfo>>() {
      @Override
      public void onFailure(Throwable caught) {
      }

      @Override
      public void onSuccess(List<GwtHighlightInfo> result) {
        editor.addHighlightInfos(result, ourEditorFlag);
      }
    });
  }

  private static void addNodes(HasTreeItems parent, GwtVirtualFile virtualFile, GwtProjectInfo projectInfo) {
    GwtVirtualFile targetFile = virtualFile != null ? virtualFile : projectInfo.getBaseDirectory();

    HorizontalPanel panel = new HorizontalPanel();
    panel.add(icon(targetFile.getIconLayers()));
    String rightIcon = targetFile.getRightIcon();
    if (rightIcon != null) {
      Widget rightIconWidget = icon(Arrays.asList(rightIcon));
      panel.add(rightIconWidget);
    }

    Label label = new Label(targetFile.getName());
    label.addStyleName("textAfterIcon18");
    if (projectInfo.getModuleDirectoryUrls().contains(targetFile.getUrl())) {
      label.addStyleName("bolded");
    }
    panel.add(label);

    SuperTreeItem item = new SuperTreeItem(panel, 1);
    item.addStyleName("noselectable");
    item.setUserObject(targetFile);

    for (GwtVirtualFile child : targetFile.getChildren()) {
      addNodes(item, child, projectInfo);
    }

    if (parent instanceof Tree) {
      item.setState(true);
    }

    parent.addItem(item);
  }

  private static Widget icon(List<String> icons) {
    FlowPanel panel = new FlowPanel();
    panel.setStyleName("imageWrapper");

    for (String icon : icons) {
      Image image = new Image("/icons/" + icon);
      image.setStyleName("overlayImage");

      panel.add(image);
    }
    return panel;
  }
}
