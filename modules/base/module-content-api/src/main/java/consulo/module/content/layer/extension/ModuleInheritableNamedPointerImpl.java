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
package consulo.module.content.layer.extension;

import consulo.annotation.access.RequiredReadAction;
import consulo.component.util.pointer.Named;
import consulo.component.util.pointer.NamedPointer;
import consulo.module.Module;
import consulo.module.content.internal.ModuleRootLayerEx;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleInheritableNamedPointer;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.util.Objects;

/**
 * @author VISTALL
 * @since 19:31/15.06.13
 */
public abstract class ModuleInheritableNamedPointerImpl<T extends Named> implements MutableModuleInheritableNamedPointer<T> {
    private NamedPointer<Module> myModulePointer;
    private NamedPointer<T> myTargetPointer;
    private final ModuleRootLayer myRootLayer;
    private final String myXmlPrefix;

    protected ModuleInheritableNamedPointerImpl(ModuleRootLayer layer, String xmlPrefix) {
        myRootLayer = layer;
        myXmlPrefix = xmlPrefix;
    }

    @Nullable
    public abstract String getItemNameFromModule(@Nonnull Module module);

    @Nullable
    public abstract T getItemFromModule(@Nonnull Module module);

    @Nonnull
    public abstract NamedPointer<T> getPointer(@Nonnull ModuleRootLayer layer, @Nonnull String name);

    @Nullable
    @Override
    public T get() {
        if (myModulePointer != null) {
            Module module = myModulePointer.get();
            if (module == null) {
                return getDefaultValue();
            }
            return getItemFromModule(module);
        }
        return myTargetPointer == null ? getDefaultValue() : myTargetPointer.get();
    }

    @Nullable
    @Override
    public String getName() {
        if (myModulePointer != null) {
            Module module = myModulePointer.get();
            if (module == null) {
                return null;
            }
            return getItemNameFromModule(module);
        }
        return myTargetPointer == null ? null : myTargetPointer.getName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ModuleInheritableNamedPointerImpl) {
            ModuleInheritableNamedPointerImpl another = (ModuleInheritableNamedPointerImpl) obj;
            return Comparing.equal(myModulePointer, another.myModulePointer) && Comparing.equal(myTargetPointer, another.myTargetPointer);
        }
        else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(myModulePointer, myTargetPointer);
    }

    @RequiredReadAction
    @Override
    public void set(@Nonnull ModuleInheritableNamedPointer<T> anotherItem) {
        if (anotherItem.isNull()) {
            myModulePointer = null;
            myTargetPointer = null;
        }
        else {
            String moduleName = anotherItem.getModuleName();
            myModulePointer = moduleName == null ? null : createModulePointer(moduleName);

            if (myModulePointer == null) {
                String targetName = anotherItem.getName();
                myTargetPointer = getPointer(myRootLayer, targetName);
            }
        }
    }

    @RequiredReadAction
    @Override
    public void set(@Nullable String moduleName, @Nullable String name) {
        if (moduleName != null) {
            myModulePointer = createModulePointer(moduleName);
            myTargetPointer = null;
        }
        else if (name != null) {
            myTargetPointer = getPointer(myRootLayer, name);
            myModulePointer = null;
        }
        else {
            myModulePointer = null;
            myTargetPointer = null;
        }
    }

    @RequiredReadAction
    @Override
    public void set(@Nullable Module module, @Nullable T named) {
        if (module != null) {
            myModulePointer = createModulePointer(module.getName());
            myTargetPointer = null;
        }
        else if (named != null) {
            myTargetPointer = getPointer(myRootLayer, named.getName());
            myModulePointer = null;
        }
        else {
            myModulePointer = null;
            myTargetPointer = null;
        }
    }

    public void toXml(Element element) {
        if (myModulePointer != null) {
            element.setAttribute(myXmlPrefix + "-module-name", myModulePointer.getName());
        }
        else if (myTargetPointer != null) {
            element.setAttribute(myXmlPrefix + "-name", myTargetPointer.getName());
        }
    }

    @RequiredReadAction
    public void fromXml(Element element) {
        String moduleName = StringUtil.nullize(element.getAttributeValue(myXmlPrefix + "-module-name"));
        if (moduleName != null) {
            myModulePointer = createModulePointer(moduleName);
        }
        String itemName = StringUtil.nullize(element.getAttributeValue(myXmlPrefix + "-name"));
        if (itemName != null) {
            myTargetPointer = getPointer(myRootLayer, itemName);
        }
    }

    @Nonnull
    @RequiredReadAction
    private NamedPointer<Module> createModulePointer(String name) {
        return ((ModuleRootLayerEx) myRootLayer).getConfigurationAccessor().getModulePointer(myRootLayer.getProject(), name);
    }

    @Nullable
    public T getDefaultValue() {
        return null;
    }

    @Nullable
    @Override
    public Module getModule() {
        if (myModulePointer == null) {
            return null;
        }
        return myModulePointer.get();
    }

    @Nullable
    @Override
    public String getModuleName() {
        if (myModulePointer == null) {
            return null;
        }
        return myModulePointer.getName();
    }

    @Override
    public boolean isNull() {
        return myModulePointer == null && myTargetPointer == null;
    }
}
