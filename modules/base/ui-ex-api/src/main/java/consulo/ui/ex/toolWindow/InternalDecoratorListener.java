/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.toolWindow;

import java.util.EventListener;

/**
 * @author Vladimir Kondratyev
 */
public interface InternalDecoratorListener extends EventListener {
  void anchorChanged(ToolWindowInternalDecorator source, ToolWindowAnchor anchor);

  void autoHideChanged(ToolWindowInternalDecorator source, boolean autoHide);

  void hidden(ToolWindowInternalDecorator source);

  void hiddenSide(ToolWindowInternalDecorator source);

  void resized(ToolWindowInternalDecorator source);

  void activated(ToolWindowInternalDecorator source);

  void typeChanged(ToolWindowInternalDecorator source, ToolWindowType type);

  void sideStatusChanged(ToolWindowInternalDecorator source, boolean isSideTool);

  void contentUiTypeChanges(ToolWindowInternalDecorator sources, ToolWindowContentUiType type);

  void visibleStripeButtonChanged(ToolWindowInternalDecorator source, boolean visible);
}
