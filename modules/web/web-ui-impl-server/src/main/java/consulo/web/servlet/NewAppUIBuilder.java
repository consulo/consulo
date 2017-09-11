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

import com.vaadin.server.*;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.UI;
import consulo.SomeTestUIBuilder;
import consulo.ui.Component;
import consulo.ui.MenuBar;
import consulo.ui.RequiredUIAccess;
import consulo.ui.Size;
import consulo.ui.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author VISTALL
 * @since 10-Sep-17
 */
public class NewAppUIBuilder extends UI {
  @WebServlet(urlPatterns = "/app/*")
  public static class Servlet extends VaadinServlet {
    @Override
    protected DeploymentConfiguration createDeploymentConfiguration(Properties initParameters) {
      return new DefaultDeploymentConfiguration(getClass(), initParameters) {
        @Override
        public String getResourcesPath() {
          return "/app";
        }

        @Override
        public String getWidgetset(String defaultValue) {
          return "consulo.web.gwt.UI";
        }

        @Override
        public PushMode getPushMode() {
          return PushMode.AUTOMATIC;
        }
      };
    }

    @Override
    protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) throws ServiceException {
      VaadinServletService service = new VaadinServletService(this, deploymentConfiguration);
      service.init();
      service.setClassLoader(NewAppUIBuilder.class.getClassLoader());
      return service;
    }

    @Override
    protected void servletInitialized() throws ServletException {
      getService().addSessionInitListener((SessionInitListener)sessionInitEvent -> {

        VaadinSession session = sessionInitEvent.getSession();
        List<UIProvider> uiProviders = new ArrayList<>(session.getUIProviders());
        for (UIProvider provider : uiProviders) {
          if (DefaultUIProvider.class.getCanonicalName().equals(provider.getClass().getCanonicalName())) {
            session.removeUIProvider(provider);
          }
        }

        session.addUIProvider(new UIProvider() {
          @Override
          public Class<? extends UI> getUIClass(UIClassSelectionEvent event) {
            return NewAppUIBuilder.class;
          }

          @Override
          public String getTheme(UICreateEvent event) {
            return "consulo";
          }

          @Override
          public UI createInstance(UICreateEvent event) {
            return new NewAppUIBuilder();
          }
        });
      });
    }
  }

  @Override
  @RequiredUIAccess
  protected void init(VaadinRequest request) {

    SomeTestUIBuilder.build(new Window() {
      @RequiredUIAccess
      @Override
      public void setContent(@NotNull Component content) {
        NewAppUIBuilder.this.setContent((com.vaadin.ui.Component)content);
      }

      @RequiredUIAccess
      @Override
      public void setMenuBar(@Nullable MenuBar menuBar) {

      }

      @Override
      public boolean isVisible() {
        return NewAppUIBuilder.this.isVisible();
      }

      @RequiredUIAccess
      @Override
      public void setVisible(boolean value) {

      }

      @Override
      public boolean isEnabled() {
        return false;
      }

      @RequiredUIAccess
      @Override
      public void setEnabled(boolean value) {

      }

      @Nullable
      @Override
      public Component getParentComponent() {
        return null;
      }

      @RequiredUIAccess
      @Override
      public void setSize(@NotNull Size size) {

      }
    });
  }
}
