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
package consulo.sandboxPlugin.ide.module.extension;

import consulo.annotation.access.RequiredReadAction;
import consulo.content.bundle.SdkType;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.extension.ModuleExtensionWithSdkBase;
import consulo.sandboxPlugin.ide.bundle.SandBundleType;
import org.jdom.Element;

import java.util.Set;
import java.util.TreeSet;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public class SandModuleExtension extends ModuleExtensionWithSdkBase<SandModuleExtension> {
  protected Set<String> myFlags = new TreeSet<>();

  public SandModuleExtension(String id, ModuleRootLayer rootModel) {
    super(id, rootModel);
  }


  @Override
  public Class<? extends SdkType> getSdkTypeClass() {
    return SandBundleType.class;
  }

  public Set<String> getFlags() {
    return Set.copyOf(myFlags);
  }

  @RequiredReadAction
  @Override
  public void commit(SandModuleExtension mutableModuleExtension) {
    super.commit(mutableModuleExtension);
    myFlags = new TreeSet<>(mutableModuleExtension.myFlags);
  }

  @Override
  protected void getStateImpl(Element element) {
    super.getStateImpl(element);
    for (String flag : myFlags) {
      element.addContent(new Element("flag").setAttribute("name", flag));
    }
  }

  @RequiredReadAction
  @Override
  protected void loadStateImpl(Element element) {
    super.loadStateImpl(element);
    myFlags.clear();
    for (Element child : element.getChildren("flag")) {
      String name = child.getAttributeValue("name");
      if (name != null) {
        myFlags.add(name);
      }
    }
  }
}
