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
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import consulo.web.gwt.client.service.EditorColorSchemeListService;
import consulo.web.gwt.client.service.EditorColorSchemeService;
import consulo.web.gwt.client.service.FetchService;
import consulo.web.gwt.client.ui.DoubleClickTree;
import consulo.web.gwt.client.ui.DoubleClickTreeEvent;
import consulo.web.gwt.client.ui.DoubleClickTreeHandler;
import consulo.web.gwt.client.ui.EditorTabPanel;
import consulo.web.gwt.client.util.GwtUtil;
import consulo.web.gwt.client.util.ReportableCallable;
import consulo.web.gwt.shared.transport.GwtProjectInfo;
import consulo.web.gwt.shared.transport.GwtVirtualFile;
import org.cafesip.gwtcomp.client.ui.SuperTreeItem;

import java.util.Arrays;

/**
 * @author VISTALL
 * @since 15-May-16
 */
public class GwtMain implements EntryPoint {

  @Override
  public void onModuleLoad() {
    fetch(new Runnable() {
      @Override
      public void run() {
        initContentPanel();
      }
    }, 0, new EditorColorSchemeListService(), new EditorColorSchemeService());
  }

  private static void fetch(final Runnable after, final int index, final FetchService... fetchServices) {
    if (index == fetchServices.length) {
      after.run();
      return;
    }

    final FetchService fetchService = fetchServices[index];
    fetchService.fetch(new Runnable() {
                         @Override
                         public void run() {
                           Window.alert("Error initialization");
                         }
                       }, new Runnable() {
                         @Override
                         public void run() {
                           GwtUtil.put(fetchService.getKey(), fetchService);

                           fetch(after, index + 1, fetchServices);
                         }
                       }
    );
  }

  private static void initContentPanel() {
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

    final EditorColorSchemeService schemeService = GwtUtil.get(EditorColorSchemeService.KEY);
    EditorColorSchemeListService listService = GwtUtil.get(EditorColorSchemeListService.KEY);

    MenuBar schemeMenu = new MenuBar(true);

    for (final String schemeName : listService.getSchemes()) {
      schemeMenu.addItem(schemeName, new Command() {
        @Override
        public void execute() {
          schemeService.setScheme(schemeName);
        }
      });
    }

    menu.addItem("Scheme", schemeMenu);

    flowPanel.add(menu);

    Grid splitPanel = GwtUtil.fillAndReturn(new Grid(1, 2));
    splitPanel.getCellFormatter().setWidth(0, 0, "20%");
    splitPanel.getCellFormatter().addStyleName(0, 0, "projectTreeBorder");
    splitPanel.getCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_TOP);
    splitPanel.getCellFormatter().setVerticalAlignment(0, 1, HasVerticalAlignment.ALIGN_TOP);

    //splitPanel.setSplitPosition("20%");

    final DoubleClickTree tree = new DoubleClickTree();
    GwtUtil.rpc().getProjectInfo("ignored", new ReportableCallable<GwtProjectInfo>() {
      @Override
      public void onSuccess(GwtProjectInfo result) {
        addNodes(tree, null, result);
      }
    });

    splitPanel.setWidget(0, 0, tree);

    final EditorTabPanel tabPanel = new EditorTabPanel();
    splitPanel.setWidget(0, 1, tabPanel);

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

        tabPanel.openFileInEditor(virtualFile, -1);
      }
    });

    flowPanel.add(splitPanel);

    RootPanel.get().add(flowPanel);
  }

  private static void addNodes(HasTreeItems parent, GwtVirtualFile virtualFile, GwtProjectInfo projectInfo) {
    GwtVirtualFile targetFile = virtualFile != null ? virtualFile : projectInfo.getBaseDirectory();

    HorizontalPanel panel = new HorizontalPanel();
    panel.add(GwtUtil.icon(targetFile.getIconLayers()));
    String rightIcon = targetFile.getRightIcon();
    if (rightIcon != null) {
      Widget rightIconWidget = GwtUtil.icon(Arrays.asList(rightIcon));
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
}
