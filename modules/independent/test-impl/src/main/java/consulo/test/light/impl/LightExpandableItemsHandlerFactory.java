/*
 * Copyright 2013-2018 consulo.io
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
package consulo.test.light.impl;

import com.intellij.ui.ExpandableItemsHandler;
import com.intellij.ui.ExpandableItemsHandlerFactory;
import com.intellij.ui.TableCell;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 2018-08-25
 */
public class LightExpandableItemsHandlerFactory extends ExpandableItemsHandlerFactory
{
  @Override
  protected ExpandableItemsHandler<Integer> doInstall(JList list) {
    return ExpandableItemsHandlerFactory.NULL;
  }

  @Override
  protected ExpandableItemsHandler<Integer> doInstall(JTree tree) {
    return ExpandableItemsHandlerFactory.NULL;
  }

  @Override
  protected ExpandableItemsHandler<TableCell> doInstall(JTable table) {
    return ExpandableItemsHandlerFactory.NULL;
  }
}
