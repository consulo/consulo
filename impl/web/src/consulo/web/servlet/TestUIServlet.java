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
package consulo.web.servlet;

import com.intellij.openapi.util.IconLoader;
import com.intellij.util.Function;
import consulo.SomeTestUIBuilder;
import consulo.ui.Component;
import consulo.ui.RequiredUIThread;
import consulo.ui.UIAccess;
import consulo.web.servlet.ui.UIServlet;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 11-Jun-16
 */
public class TestUIServlet extends UIServlet {
  static {
    IconLoader.activate(); // TODO [VISTALL] hack until we not start Consulo app
  }

  public TestUIServlet() {
    super("ui");
  }

  @NotNull
  @Override
  public Function<UIAccess, Component> uiFactory() {
    return new Function<UIAccess, Component>() {
      @Override
      @RequiredUIThread
      public Component fun(UIAccess uiAccess) {
        return SomeTestUIBuilder.build();
      }
    };
  }
}
