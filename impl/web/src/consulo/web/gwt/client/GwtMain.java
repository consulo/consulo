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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTree;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.TreeViewModel;
import consulo.web.gwt.client.service.EditorColorSchemeListService;
import consulo.web.gwt.client.service.EditorColorSchemeService;
import consulo.web.gwt.client.service.FetchService;
import consulo.web.gwt.client.ui.DefaultCellTreeResources;
import consulo.web.gwt.client.ui.EditorTabPanel;
import consulo.web.gwt.client.util.GwtUIUtil;
import consulo.web.gwt.client.util.GwtUtil;
import consulo.web.gwt.client.util.Log;
import consulo.web.gwt.client.util.ReportableCallable;
import consulo.web.gwt.shared.transport.GwtProjectInfo;
import consulo.web.gwt.shared.transport.GwtVirtualFile;

import java.util.Arrays;
import java.util.List;

/**
 * @author VISTALL
 * @since 15-May-16
 */
public class GwtMain implements EntryPoint {
  @Override
  public void onModuleLoad() {
    final SimplePanel panel = GwtUIUtil.fillAndReturn(new SimplePanel());
    panel.add(GwtUIUtil.loadingPanel());
    RootPanel.get().add(panel);

    fetch(new Runnable() {
      @Override
      public void run() {
        initContentPanel(panel);
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

  private static class ProjectTreeViewModel implements TreeViewModel {
    private final AbstractCell<GwtVirtualFile> myCell = new AbstractCell<GwtVirtualFile>(BrowserEvents.DBLCLICK) {
      @Override
      public void render(Context context, GwtVirtualFile value, SafeHtmlBuilder sb) {
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(GwtUIUtil.icon(value.getIconLayers()));
        String rightIcon = value.getRightIcon();
        if (rightIcon != null) {
          Widget rightIconWidget = GwtUIUtil.icon(Arrays.asList(rightIcon));
          panel.add(rightIconWidget);
        }

        Label label = new Label(value.getName());
        label.addStyleName("textAfterIcon18");
        if (myResult.getModuleDirectoryUrls().contains(value.getUrl())) {
          label.addStyleName("bolded");
        }
        label.getElement().getStyle().setCursor(Style.Cursor.DEFAULT);
        panel.add(label);

        SafeHtml safeValue = SafeHtmlUtils.fromSafeConstant(panel.toString());

        sb.append(safeValue);
      }

      @Override
      public void onBrowserEvent(Context context, Element parent, GwtVirtualFile value, NativeEvent event, ValueUpdater<GwtVirtualFile> valueUpdater) {
        String type = event.getType();

        if (type.equals(BrowserEvents.DBLCLICK)) {
          if (value.isDirectory()) {
            Log.log("double click on directory");
          }
          else {
            myEditorTabPanel.openFileInEditor(value, -1);
          }
        }
        else {
          super.onBrowserEvent(context, parent, value, event, valueUpdater);
        }
      }
    };

    private GwtProjectInfo myResult;
    private EditorTabPanel myEditorTabPanel;

    public ProjectTreeViewModel(GwtProjectInfo result, EditorTabPanel editorTabPanel) {
      myResult = result;
      myEditorTabPanel = editorTabPanel;
    }

    @Override
    public <T> NodeInfo<?> getNodeInfo(final T topValue) {
      if (topValue instanceof GwtProjectInfo) {
        final ListDataProvider<GwtVirtualFile> provider = new ListDataProvider<GwtVirtualFile>();
        provider.getList().add(((GwtProjectInfo)topValue).getBaseDirectory());

        return new DefaultNodeInfo<GwtVirtualFile>(provider, myCell);
      }

      if (topValue instanceof GwtVirtualFile) {
        final ListDataProvider<GwtVirtualFile> provider = new ListDataProvider<GwtVirtualFile>();

        GwtUtil.rpc().listChildren(((GwtVirtualFile)topValue).getUrl(), new ReportableCallable<List<GwtVirtualFile>>() {
          @Override
          public void onSuccess(List<GwtVirtualFile> result) {
            provider.setList(result);
          }
        });

        return new DefaultNodeInfo<GwtVirtualFile>(provider, myCell);
      }

      return null;
    }

    @Override
    public boolean isLeaf(Object value) {
      return value instanceof GwtVirtualFile && !((GwtVirtualFile)value).isDirectory();
    }
  }

  private static void initContentPanel(final SimplePanel rootPanel) {
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

    final Grid splitPanel = GwtUIUtil.fillAndReturn(new Grid(1, 2));
    splitPanel.getCellFormatter().setWidth(0, 0, "20%");
    splitPanel.getCellFormatter().addStyleName(0, 0, "projectTreeBorder");
    splitPanel.getCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_TOP);
    splitPanel.getCellFormatter().setVerticalAlignment(0, 1, HasVerticalAlignment.ALIGN_TOP);

    final EditorTabPanel editorTabPanel = GwtUIUtil.fillAndReturn(new EditorTabPanel());
    splitPanel.setWidget(0, 1, editorTabPanel);

    splitPanel.setWidget(0, 0, GwtUIUtil.loadingPanel());

    GwtUtil.rpc().getProjectInfo("ignored", new ReportableCallable<GwtProjectInfo>() {
      @Override
      public void onSuccess(GwtProjectInfo result) {
        CellTree projectTree =
                new CellTree(new ProjectTreeViewModel(result, editorTabPanel), result, GWT.<CellTree.Resources>create(DefaultCellTreeResources.class));
        projectTree.getRootTreeNode().setChildOpen(0, true);

        splitPanel.setWidget(0, 0, projectTree);
      }
    });

    flowPanel.add(splitPanel);

    rootPanel.setWidget(flowPanel);
  }
}
