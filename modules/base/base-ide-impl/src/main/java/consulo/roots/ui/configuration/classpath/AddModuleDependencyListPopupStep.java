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
package consulo.roots.ui.configuration.classpath;

import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-09-05
 */
public class AddModuleDependencyListPopupStep extends BaseListPopupStep<Map.Entry<AddModuleDependencyActionProvider, AddModuleDependencyContext>> {
  private final ModifiableRootModel myRootModel;

  public AddModuleDependencyListPopupStep(@Nonnull ModifiableRootModel rootModel, @Nonnull Set<Map.Entry<AddModuleDependencyActionProvider, AddModuleDependencyContext>> values) {
    super("Add Dependency", new ArrayList<>(values));
    myRootModel = rootModel;
  }

  @Nonnull
  @Override
  public String getTextFor(Map.Entry<AddModuleDependencyActionProvider, AddModuleDependencyContext> value) {
    return value.getKey().getActionName(myRootModel).get();
  }

  @Override
  public Image getIconFor(Map.Entry<AddModuleDependencyActionProvider, AddModuleDependencyContext> value) {
    return value.getKey().getIcon(myRootModel);
  }

  @Override
  public PopupStep onChosen(Map.Entry<AddModuleDependencyActionProvider, AddModuleDependencyContext> selectedValue, boolean finalChoice) {
    return doFinalStep(() -> providerSelected(selectedValue));
  }

  @RequiredUIAccess
  @SuppressWarnings("unchecked")
  public static void providerSelected(Map.Entry<AddModuleDependencyActionProvider, AddModuleDependencyContext> entry) {
    AddModuleDependencyActionProvider provider = entry.getKey();
    AddModuleDependencyContext value = entry.getValue();

    AsyncResult result = provider.invoke(value);

    result.doWhenDone(o -> value.processAddOrderEntries(o));
  }
}
