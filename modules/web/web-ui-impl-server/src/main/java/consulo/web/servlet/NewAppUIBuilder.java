/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.servlet;

import com.intellij.openapi.application.ex.ApplicationEx;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.vaadin.server.Sizeable;
import com.vaadin.ui.UI;
import consulo.application.impl.FrameTitleUtil;
import consulo.ui.Components;
import consulo.ui.DockLayout;
import consulo.ui.HorizontalLayout;
import consulo.ui.Layouts;
import consulo.ui.ListBox;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.VerticalLayout;
import consulo.ui.Window;
import consulo.ui.border.BorderPosition;
import consulo.ui.internal.WGwtLabelImpl;
import consulo.ui.internal.WGwtVerticalLayoutImpl;
import consulo.web.servlet.ui.UIBuilder;
import consulo.web.servlet.ui.UIServlet;
import consulo.web.ui.FileTreeComponent;
import org.jetbrains.annotations.NotNull;

import javax.servlet.annotation.WebServlet;

/**
 * @author VISTALL
 * @since 10-Sep-17
 */
public class NewAppUIBuilder extends UIBuilder {
  @WebServlet(urlPatterns = "/app/*")
  public static class Servlet extends UIServlet {
    public Servlet() {
      super(NewAppUIBuilder.class, "/app");
    }
  }

  @RequiredUIAccess
  @Override
  protected void build(@NotNull Window window) {
    ApplicationEx application = ApplicationManagerEx.getApplicationEx();
    if (application == null || !application.isLoaded()) {
      window.setContent(Components.label("Loading"));
      return;
    }

    com.vaadin.ui.Window welcomeFrame = new com.vaadin.ui.Window(FrameTitleUtil.buildTitle());
    welcomeFrame.setResizable(false);
    welcomeFrame.setClosable(false);
    welcomeFrame.setModal(true);
    welcomeFrame.setWidth(777, Sizeable.Unit.PIXELS);
    welcomeFrame.setHeight(460, Sizeable.Unit.PIXELS);
    welcomeFrame.setContent(new WGwtLabelImpl("TEst"));

    //AnAction[] recentProjectsActions = RecentProjectsManager.getInstance().getRecentProjectsActions(false);

    ListBox<String> listSelect = Components.listBox("Test");
    listSelect.addBorder(BorderPosition.RIGHT);
    listSelect.setSize(new Size(200, -1));

    DockLayout layout = Layouts.dock();
    layout.left(listSelect);

    VerticalLayout projectActionLayout = new WGwtVerticalLayoutImpl();

    projectActionLayout.add(Components.button("Open Project", () -> {
      com.vaadin.ui.Window fileTree = new com.vaadin.ui.Window("Select file");
      fileTree.setModal(true);
      fileTree.setWidth(400, Sizeable.Unit.PIXELS);
      fileTree.setHeight(400, Sizeable.Unit.PIXELS);
      fileTree.setContent(new WGwtLabelImpl("TEst"));

      DockLayout dockLayout = Layouts.dock();
      dockLayout.center(FileTreeComponent.create());
      HorizontalLayout botton = Layouts.horizontal();
      botton.addBorder(BorderPosition.TOP);

      consulo.ui.Button ok = Components.button("OK");
      ok.setEnabled(false);
      botton.add(ok);
      consulo.ui.Button cancel = Components.button("Cancel");
      cancel.addListener(consulo.ui.Button.ClickHandler.class, () -> {
        fileTree.close();
      });

      botton.add(cancel);
      dockLayout.bottom(botton);

      fileTree.setContent((com.vaadin.ui.Component)dockLayout);


      UI.getCurrent().addWindow(fileTree);
    }));
    layout.center(projectActionLayout);

    welcomeFrame.setContent((com.vaadin.ui.Component)layout);

    UI.getCurrent().addWindow(welcomeFrame);
  }
}
