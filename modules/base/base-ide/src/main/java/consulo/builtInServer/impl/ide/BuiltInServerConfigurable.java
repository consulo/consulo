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
package consulo.builtInServer.impl.ide;

import com.intellij.openapi.options.Configurable;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.options.SimpleConfigurableByProperties;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.IntBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.util.LabeledBuilder;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-11-27
 */
public class BuiltInServerConfigurable extends SimpleConfigurableByProperties implements Configurable {
  @Override
  public String getDisplayName() {
    return "Built-in server";
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
    BuiltInServerOptions options = BuiltInServerOptions.getInstance();

    VerticalLayout root = VerticalLayout.create();
    IntBox portBox = IntBox.create().withRange(1024, Short.MAX_VALUE & 0xFFFF);
    propertyBuilder.add(portBox, () -> options.builtInServerPort, it -> options.builtInServerPort = it);
    root.add(LabeledBuilder.simple(LocalizeValue.localizeTODO("Port"), portBox));

    CheckBox canAcceptExternalConnectionsBox = CheckBox.create(LocalizeValue.localizeTODO("Can accept external connections"));
    propertyBuilder.add(canAcceptExternalConnectionsBox, () -> options.builtInServerAvailableExternally, it -> options.builtInServerAvailableExternally = it);
    root.add(canAcceptExternalConnectionsBox);

    CheckBox allowUnsignedRequestsBox = CheckBox.create(LocalizeValue.localizeTODO("Allow unsigned requests"));
    propertyBuilder.add(allowUnsignedRequestsBox, () -> options.allowUnsignedRequests, it -> options.allowUnsignedRequests = it);
    root.add(allowUnsignedRequestsBox);
    return root;
  }

  @Override
  protected void afterApply() {
    BuiltInServerOptions.onBuiltInServerPortChanged();
  }
}
