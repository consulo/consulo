/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.impl.bundle;

import consulo.content.bundle.SdkModel;
import consulo.application.content.impl.internal.bundle.SdkImpl;
import consulo.ide.impl.idea.openapi.projectRoots.ui.BaseSdkEditor;
import consulo.ide.ui.SdkPathEditor;
import consulo.content.OrderRootType;
import consulo.ide.ui.OrderRootTypeUIFactory;
import consulo.ui.ex.awt.TabbedPaneWrapper;
import consulo.disposer.Disposable;

import jakarta.annotation.Nonnull;
import javax.swing.*;

/**
 * @author VISTALL
 * @since 21.03.14
 */
public class SdkEditor extends BaseSdkEditor {
  public SdkEditor(@Nonnull SdkModel sdkModel, @Nonnull SdkImpl sdk) {
    super(sdkModel, sdk);
  }

  @Nonnull
  @Override
  protected JComponent createCenterComponent(Disposable parentUIDisposable) {
    TabbedPaneWrapper tabbedPane = new TabbedPaneWrapper(parentUIDisposable);
    for (OrderRootType type : OrderRootType.getAllTypes()) {
      if (showTabForType(type)) {
        OrderRootTypeUIFactory factory = OrderRootTypeUIFactory.forOrderType(type);
        if (factory == null) {
          continue;
        }

        SdkPathEditor pathEditor = getPathEditor(type);

        tabbedPane.addTab(pathEditor.getDisplayName(), pathEditor.createComponent());
      }
    }

    return tabbedPane.getComponent();
  }
}
