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
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.transport.GwtHighlightInfo;
import consulo.web.gwt.client.transport.GwtNavigatable;
import consulo.web.gwt.client.transport.GwtProjectInfo;
import consulo.web.gwt.client.transport.GwtVirtualFile;
import consulo.web.gwt.client.ui.*;
import consulo.web.gwt.shared.GwtTransportService;
import consulo.web.gwt.shared.GwtTransportServiceAsync;
import org.cafesip.gwtcomp.client.ui.SuperTreeItem;
import org.jetbrains.annotations.NotNull;

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

  private static final GwtTransportServiceAsync ourAsyncService = GWT.create(GwtTransportService.class);

  private Map<String, Integer> myOpenedFiles = new HashMap<String, Integer>();
  private final com.github.gwtbootstrap.client.ui.TabPanel myTabPanel = new com.github.gwtbootstrap.client.ui.TabPanel();

  @Override
  public void onModuleLoad() {
    //final SimplePanel globalPanel = new SimplePanel();
    /*globalPanel.setWidth("100%");
    globalPanel.setHeight(Window.getClientHeight() + "px");
    Window.addResizeHandler(new ResizeHandler() {
      @Override
      public void onResize(ResizeEvent event) {
        int height = event.getHeight();
        globalPanel.setHeight(height + "px");
      }
    });*/

    FlowPanel flowPanel = new FlowPanel();
    Command cmd = new Command() {
      @Override
      public void execute() {
      }
    };

    MenuBar fileMenu = new MenuBar(true);
    fileMenu.addItem("Settings", cmd);
    fileMenu.addItem("Project Structure", cmd);


    MenuBar menu = new MenuBar();
    menu.addItem("File", fileMenu);

    flowPanel.add(menu);

    consulo.web.gwt.client.ui.HorizontalSplitPanel splitPanel = new consulo.web.gwt.client.ui.HorizontalSplitPanel();
    splitPanel.setSplitPosition("20%");

    final DoubleClickTree tree = new DoubleClickTree();
    ourAsyncService.getProjectInfo("ignored", new ReportableCallable<GwtProjectInfo>() {
      @Override
      public void onSuccess(GwtProjectInfo result) {
        addNodes(tree, null, result);
      }
    });

    splitPanel.setLeftWidget(tree);
    splitPanel.setRightWidget(myTabPanel);

    tree.addDoubleClickHandler(new DoubleClickTreeHandler() {
      @Override
      public void onDoubleClick(DoubleClickTreeEvent event) {
        TreeItem selectedItem = event.getItem();
        GwtVirtualFile virtualFile = (GwtVirtualFile)selectedItem.getUserObject();
        if (virtualFile.isDirectory()) {
          boolean state = selectedItem.getState();
          selectedItem.setState(!state);
          return;
        }

        openFileInEditor(virtualFile);
      }
    });

    flowPanel.add(splitPanel);

    RootPanel.get().add(flowPanel);
  }

  private void openFileInEditor(final GwtVirtualFile virtualFile) {
    Integer indexOfOpened = myOpenedFiles.get(virtualFile.getUrl());
    if (indexOfOpened != null) {
      myTabPanel.selectTab(indexOfOpened);
      return;
    }

    ourAsyncService.getContent(virtualFile.getUrl(), new AsyncCallback<String>() {
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

        myTabPanel.add(tabLink);

        TabPane tabPane = tabLink.getTabPane();
        tabPane.setHeight("100%");
        tabPane.setWidth("100%");
        tabPane.addStyleName("disableOverflow");

        tabPane.add(editor.getComponent());

        // TabPanel can't return tab size???
        int index = myOpenedFiles.size();
        myTabPanel.selectTab(index);
        myOpenedFiles.put(virtualFile.getUrl(), index);

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

        ourAsyncService.getLexerHighlight(virtualFile.getUrl(), new ReportableCallable<List<GwtHighlightInfo>>() {
          @Override
          public void onSuccess(List<GwtHighlightInfo> result) {
            editor.addHighlightInfos(result, ourLexerFlag);

            runHighlightPasses(virtualFile, editor, 0);

            editor.setCaretHandler(new EditorCaretHandler() {
              @Override
              public void caretPlaced(@NotNull final EditorCaretEvent event) {
                runHighlightPasses(virtualFile, editor, event.getOffset());

                ourAsyncService.getNavigationInfo(virtualFile.getUrl(), event.getOffset(), new ReportableCallable<List<GwtNavigatable>>() {
                  @Override
                  public void onSuccess(List<GwtNavigatable> result) {
                    if (!result.isEmpty()) {

                      final PopupPanel popupPanel = new PopupPanel(true);
                      for (final GwtNavigatable navigatable : result) {
                        Anchor anchor = new Anchor("Navigate to declaration");
                        anchor.addClickHandler(new ClickHandler() {
                          @Override
                          public void onClick(ClickEvent event) {
                            ourAsyncService.findFileByUrl(navigatable.getFileUrl(), new ReportableCallable<GwtVirtualFile>() {
                              @Override
                              public void onSuccess(GwtVirtualFile result) {
                                popupPanel.hide();
                                openFileInEditor(result);
                              }
                            });
                          }
                        });
                        popupPanel.add(anchor);
                      }

                      popupPanel.setPopupPosition(event.getClientX(), event.getClientY());
                      popupPanel.show();
                    }
                  }
                });
              }
            });
          }
        });
      }
    });
  }

  private static void runHighlightPasses(GwtVirtualFile virtualFile, final Editor editor, int offset) {
    ourAsyncService.runHighlightPasses(virtualFile.getUrl(), offset, new ReportableCallable<List<GwtHighlightInfo>>() {
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
      Image image = new Image("/icon?path=\"" + icon + "\"");
      image.setStyleName("overlayImage");

      panel.add(image);
    }
    return panel;
  }
}
