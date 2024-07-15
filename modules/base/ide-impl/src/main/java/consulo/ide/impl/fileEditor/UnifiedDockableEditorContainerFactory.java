/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.fileEditor;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.impl.internal.DockableEditorContainerFactory;
import consulo.project.ui.wm.dock.DockContainer;
import consulo.project.ui.wm.dock.DockManager;
import consulo.project.ui.wm.dock.DockableContent;
import org.jdom.Element;

/**
 * @author VISTALL
 * @since 02/06/2023
 */
@ExtensionImpl
public class UnifiedDockableEditorContainerFactory implements DockableEditorContainerFactory {

  @Override
  public DockContainer loadContainerFrom(DockManager dockManager, Element element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public DockContainer createContainer(DockManager dockManager, DockableContent content) {
    throw new UnsupportedOperationException();
  }
}
