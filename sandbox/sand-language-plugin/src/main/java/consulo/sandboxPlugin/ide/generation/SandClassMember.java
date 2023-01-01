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
package consulo.sandboxPlugin.ide.generation;

import consulo.language.editor.generation.ClassMember;
import consulo.language.editor.generation.MemberChooserObject;
import consulo.language.editor.generation.MemberChooserObjectBase;
import consulo.platform.base.icon.PlatformIconGroup;

/**
 * @author VISTALL
 * @since 20-Aug-22
 */
public class SandClassMember extends MemberChooserObjectBase implements ClassMember {
  private final SandClassNode myParent;

  public SandClassMember(SandClassNode parent) {
    super("Sand", PlatformIconGroup.nodesMethod());
    myParent = parent;
  }

  @Override
  public MemberChooserObject getParentNodeDelegate() {
    return myParent;
  }
}
