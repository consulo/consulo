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
package consulo.ide.plugins;

import com.intellij.CommonBundle;
import consulo.logging.Logger;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ex.SingleConfigurableEditor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.FilterComponent;
import consulo.ide.base.BaseShowSettingsUtil;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 04-Sep-16
 */
public class AvailablePluginsDialog extends SingleConfigurableEditor {
  public static final Logger LOGGER = Logger.getInstance(AvailablePluginsDialog.class);

  public AvailablePluginsDialog(Component parent, SearchableConfigurable configurable, FilterComponent myFilter) {
    super(parent, configurable, BaseShowSettingsUtil.createDimensionKey(configurable), false);

    setOKButtonText(CommonBundle.message("close.action.name"));
    setOKButtonMnemonic('C');
    final String filter = myFilter.getFilter();

    if (!StringUtil.isEmptyOrSpaces(filter)) {
      final Runnable searchRunnable = configurable.enableSearch(filter);
      LOGGER.assertTrue(searchRunnable != null);
      searchRunnable.run();
    }
  }

  @Nonnull
  @Override
  protected Action[] createActions() {
    return new Action[]{getOKAction()};
  }
}
