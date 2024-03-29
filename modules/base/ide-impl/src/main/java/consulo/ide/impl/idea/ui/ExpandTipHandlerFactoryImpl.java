/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.ui.ex.ExpandableItemsHandler;
import consulo.ui.ex.TableCell;
import consulo.ui.ex.awt.ExpandableItemsHandlerFactory;
import jakarta.inject.Singleton;

import javax.swing.*;

@Singleton
@ServiceImpl
public class ExpandTipHandlerFactoryImpl extends ExpandableItemsHandlerFactory {
  @Override
  public ExpandableItemsHandler<Integer> doInstall(JList list) {
    return new ListExpandableItemsHandler(list);
  }

  @Override
  public ExpandableItemsHandler<Integer> doInstall(JTree tree) {
    return new TreeExpandableItemsHandler(tree);
  }

  @Override
  public ExpandableItemsHandler<TableCell> doInstall(JTable table) {
    return new TableExpandableItemsHandler(table);
  }
}
