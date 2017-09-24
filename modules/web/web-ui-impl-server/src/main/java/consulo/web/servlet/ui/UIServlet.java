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
package consulo.web.servlet.ui;

import com.intellij.openapi.util.Disposer;
import com.intellij.util.ReflectionUtil;
import com.vaadin.server.*;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.UI;
import consulo.ui.RequiredUIAccess;

import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author VISTALL
 * @since 11-Sep-17
 */
public class UIServlet extends VaadinServlet {
  public static class UIImpl extends UI {
    private String myURLPrefix;
    private final UIBuilder myBuilder;

    private VaadinUIWindowImpl myUIWindow = new VaadinUIWindowImpl(this);

    public UIImpl(String urlPrefix, UIBuilder builder) {
      myURLPrefix = urlPrefix;
      myBuilder = builder;
    }

    public String getURLPrefix() {
      return myURLPrefix;
    }

    @Override
    @RequiredUIAccess
    protected void init(VaadinRequest vaadinRequest) {
      myBuilder.build(myUIWindow);
    }

    public void onDetach() {
      Disposer.dispose(myUIWindow);
    }
  }

  private final Class<? extends UIBuilder> myClass;
  private final String myURLPrefix;

  public UIServlet(Class<? extends UIBuilder> aClass, String urlPrefix) {
    myClass = aClass;
    myURLPrefix = urlPrefix;
  }

  @Override
  protected DeploymentConfiguration createDeploymentConfiguration(Properties initParameters) {
    return new DefaultDeploymentConfiguration(getClass(), initParameters) {
      @Override
      public String getResourcesPath() {
        return myURLPrefix;
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
    VaadinServletService service = new VaadinServletService(this, deploymentConfiguration) {
      @Override
      public void closeSession(VaadinSession session) {
        super.closeSession(session);

        UIImpl next = (UIImpl)session.getUIs().iterator().next();

        next.onDetach();
      }
    };
    service.init();
    service.setClassLoader(UIImpl.class.getClassLoader());
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
          return UIImpl.class;
        }

        @Override
        public boolean isPreservedOnRefresh(UICreateEvent event) {
          return true;
        }

        @Override
        public String getTheme(UICreateEvent event) {
          return "consulo";
        }

        @Override
        public UI createInstance(UICreateEvent event) {
          return new UIImpl(myURLPrefix, ReflectionUtil.newInstance(myClass));
        }
      });
    });
  }
}