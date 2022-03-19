/*
 * Copyright 2013-2022 consulo.io
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
package consulo.codeEditor;

import consulo.util.dataholder.Key;

/**
 * @author VISTALL
 * @since 18-Mar-22
 */
public interface EditorKeys {
  /**
   * This key can be used to obtain reference to host editor instance, in case {@link #EDITOR} key is referring to an injected editor.
   */
  Key<Editor> HOST_EDITOR = Key.create("host.editor");

  /**
   * Returns Editor even if focus currently is in find bar
   */
  Key<Editor> EDITOR_EVEN_IF_INACTIVE = Key.create("editor.even.if.inactive");
  /**
   * This key can be used to check if the current context relates to a virtual space in editor.
   *
   * @see EditorSettings#setVirtualSpace(boolean)
   */
  Key<Boolean> EDITOR_VIRTUAL_SPACE = Key.create("editor.virtual.space");
}
