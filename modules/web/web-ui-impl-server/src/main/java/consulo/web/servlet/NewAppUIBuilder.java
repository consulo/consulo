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

import com.vaadin.server.Sizeable;
import com.vaadin.ui.UI;
import consulo.SomeTestUIBuilder;
import consulo.ui.RequiredUIAccess;
import consulo.ui.SplitLayout;
import consulo.ui.Window;
import consulo.ui.ex.internal.WGwtEditorImpl;
import consulo.ui.internal.WGwtLabelImpl;
import consulo.web.servlet.ui.UIBuilder;
import consulo.web.servlet.ui.UIServlet;
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

    com.vaadin.ui.Window components = new com.vaadin.ui.Window("Test");
    components.setModal(true);
    components.setWidth(100, Sizeable.Unit.PIXELS);
    components.setHeight(100, Sizeable.Unit.PIXELS);
    components.setContent(new WGwtLabelImpl("TEst"));
    UI.getCurrent().addWindow(components);

   // AppUIBuilder.build(window);
    SplitLayout splitLayout = SomeTestUIBuilder.buildTabbed(window);

    splitLayout.setSecondComponent(new WGwtEditorImpl());
  }
}
