/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.ui.impl.layout;

import com.intellij.util.containers.MultiMap;
import consulo.desktop.swt.ui.impl.DesktopSwtComponent;
import consulo.ui.Component;
import consulo.ui.layout.Layout;
import consulo.util.lang.Pair;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
public abstract class DesktopSwtLayoutComponent extends DesktopSwtComponent<Composite> implements Layout {
  private static final String ourNullMapper = "____null____";

  private List<Pair<DesktopSwtComponent<?>, Object>> myComponents = new ArrayList<>();

  private MultiMap<String, Pair<DesktopSwtComponent<?>, Object>> myMappedComponents = new MultiMap<>();

  @Override
  protected Composite createSWT(Composite parent) {
    return new Composite(parent, SWT.NONE);
  }

  @Override
  protected void initialize(Composite component) {
    org.eclipse.swt.widgets.Layout layout = createLayout();
    if (layout != null) {
      component.setLayout(layout);
    }

    for (Pair<DesktopSwtComponent<?>, Object> pair : myComponents) {
      DesktopSwtComponent<?> comp = pair.getFirst();
      Object value = pair.getSecond();

      ((DesktopSwtComponent)comp).bind(getComposite(), convertLayoutData(value));

      String k = value == null ? ourNullMapper : value.toString();

      myMappedComponents.putValue(k, pair);
    }

    myComponents.clear();
  }

  @Override
  public void setParent(@Nullable Component component) {
    super.setParent(component);

    if (component != null) {
      initialize(((DesktopSwtComponent)component).getComposite());
    }
  }

  @Override
  public void disposeSWT() {
    super.disposeSWT();

    myComponents.addAll(myMappedComponents.values());

    myMappedComponents.clear();

    for (Pair<DesktopSwtComponent<?>, Object> pair : myMappedComponents.values()) {
      pair.getFirst().disposeSWT();
    }

    for (Pair<DesktopSwtComponent<?>, Object> pair : myComponents) {
      pair.getFirst().disposeSWT();
    }
  }

  protected Object convertLayoutData(Object layoutData) {
    return layoutData;
  }

  @Nullable
  protected abstract org.eclipse.swt.widgets.Layout createLayout();

  protected void add(Component component, Object layoutData) {
    add((DesktopSwtComponent<?>)component, layoutData);
  }

  protected void add(DesktopSwtComponent<?> component, Object layoutData) {
    if (myComponent != null) {
      if (layoutData != null) {
        Collection<Pair<DesktopSwtComponent<?>, Object>> components = myMappedComponents.remove(layoutData.toString());
        if (components != null) {
          for (Pair<DesktopSwtComponent<?>, Object> oldPair : components) {
            DesktopSwtComponent<?> first = oldPair.getFirst();

            ((DesktopSwtComponent)first).setParent(null);
          }
        }
      }

      myMappedComponents.putValue(layoutData == null ? ourNullMapper : layoutData.toString(), Pair.create(component, layoutData));

      ((DesktopSwtComponent)component).bind(getComposite(), convertLayoutData(layoutData));

      myComponent.layout(true, true);
    }
    else {
      myComponents.add(Pair.create(component, layoutData));
    }
  }
}
