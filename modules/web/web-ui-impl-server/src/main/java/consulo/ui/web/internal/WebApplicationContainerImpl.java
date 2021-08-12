/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ui.web.internal;

import com.intellij.openapi.editor.colors.EditorColorKey;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.style.ComponentColors;
import consulo.ui.style.StandardColors;
import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;
import consulo.ui.web.internal.base.VaadinComponentDelegate;
import consulo.ui.web.internal.base.VaadinSingleComponentContainer;
import consulo.ui.web.internal.util.Mappers;
import consulo.web.gwt.shared.ui.state.ApplicationContainerState;
import consulo.web.gwt.shared.ui.state.ApplicationState;

import javax.annotation.Nonnull;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2020-11-21
 */
public class WebApplicationContainerImpl extends VaadinComponentDelegate<WebApplicationContainerImpl.Vaadin> {
  public static class Vaadin extends VaadinSingleComponentContainer {
    public void set(@Nonnull Component component) {
      setContent(TargetVaddin.to(component));

      markAsDirtyRecursive();
    }

    @Override
    public void beforeClientResponse(boolean initial) {
      super.beforeClientResponse(initial);

      WebApplicationContainerImpl container = (WebApplicationContainerImpl)toUIComponent();

      container.fillAppState(getState().myApplicationState);
    }

    @Override
    public ApplicationContainerState getState() {
      return (ApplicationContainerState)super.getState();
    }
  }

  public WebApplicationContainerImpl() {
  }

  private void fillAppState(ApplicationState applicationState) {
    Style currentStyle = StyleManager.get().getCurrentStyle();

    for (StandardColors color : StandardColors.values()) {
      applicationState.myStandardColors.put(color.name(), Mappers.map(currentStyle.getColorValue(color).toRGB()));
    }

    for (ComponentColors color : ComponentColors.values()) {
      applicationState.myComponentColors.put(color.name(), Mappers.map(currentStyle.getColorValue(color).toRGB()));
    }

    EditorColorsScheme globalScheme = EditorColorsManager.getInstance().getGlobalScheme();

    Map<EditorColorKey, ColorValue> colors = new LinkedHashMap<>();
    globalScheme.fillColors(colors);

    for (Map.Entry<EditorColorKey, ColorValue> entry : colors.entrySet()) {
      ColorValue value = entry.getValue();
      if (value == null) {
        continue;
      }

      applicationState.mySchemeColors.put(entry.getKey().getExternalName(), Mappers.map(value.toRGB()));
    }
  }

  @Override
  @Nonnull
  public Vaadin createVaadinComponent() {
    return new Vaadin();
  }

  @RequiredUIAccess
  @Nonnull
  public WebApplicationContainerImpl set(@Nonnull Component component) {
    getVaadinComponent().set(component);
    return this;
  }
}