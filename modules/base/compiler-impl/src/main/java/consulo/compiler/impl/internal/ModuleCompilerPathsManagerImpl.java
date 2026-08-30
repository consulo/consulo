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
package consulo.compiler.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.compiler.CompilerConfiguration;
import consulo.compiler.ModuleCompilerPathsManager;
import consulo.component.persist.PersistentStateComponent;
import consulo.content.ContentFolderTypeProvider;
import consulo.module.Module;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2013-10-20
 */
@Singleton
@ServiceImpl
public class ModuleCompilerPathsManagerImpl extends ModuleCompilerPathsManager implements PersistentStateComponent<Element> {
    private static final String MODULE_OUTPUT_TAG = "module";
    private static final String EXCLUDE = "exclude";
    private static final String NAME = "name";
    private static final String URL = "url";
    private static final String TYPE = "type";
    private static final String OUTPUT_TAG = "output";

    private final Module myModule;

    private boolean myInheritOutput = true;
    private boolean myExcludeOutput = true;

    private final Map<String, String> myOutputUrls = new LinkedHashMap<>();
    private final CompilerConfiguration myCompilerConfiguration;

    @Inject
    public ModuleCompilerPathsManagerImpl(Module module, CompilerConfiguration compilerConfiguration) {
        myModule = module;
        myCompilerConfiguration = compilerConfiguration;
    }

    @Override
    public boolean isInheritedCompilerOutput() {
        return myInheritOutput;
    }

    @Override
    public void setInheritedCompilerOutput(boolean val) {
        myInheritOutput = val;
    }

    @Override
    public boolean isExcludeOutput() {
        return myExcludeOutput;
    }

    @Override
    public void setExcludeOutput(boolean val) {
        myExcludeOutput = val;
    }

    @Override
    public void setCompilerOutputUrl(ContentFolderTypeProvider contentFolderType, @Nullable String compilerOutputUrl) {
        if (myInheritOutput) {
            throw new IllegalArgumentException();
        }
        if (compilerOutputUrl == null) {
            return;
        }

        myOutputUrls.put(contentFolderType.getId(), compilerOutputUrl);
    }

    @Override
    public @Nullable String getCompilerOutputUrl(ContentFolderTypeProvider contentFolderType) {
        if (!myInheritOutput) {
            String url = myOutputUrls.get(contentFolderType.getId());
            if (url != null) {
                return url;
            }
        }

        return myCompilerConfiguration.getCompilerOutputUrl() + "/" + getRelativePathForProvider(contentFolderType, myModule);
    }

    @Override
    public @Nullable Element getState() {
        if (myInheritOutput) {
            return null;
        }

        Element moduleElement = new Element(MODULE_OUTPUT_TAG);
        moduleElement.setAttribute(NAME, myModule.getName());
        if (!isExcludeOutput()) {
            moduleElement.setAttribute(EXCLUDE, String.valueOf(isExcludeOutput()));
        }

        for (Map.Entry<String, String> tempEntry : myOutputUrls.entrySet()) {
            Element elementForOutput = new Element(OUTPUT_TAG);
            elementForOutput.setAttribute(URL, tempEntry.getValue());
            elementForOutput.setAttribute(TYPE, tempEntry.getKey());
            moduleElement.addContent(elementForOutput);
        }

        return moduleElement;
    }

    @Override
    public void loadState(Element element) {
        myInheritOutput = false;
        myExcludeOutput = Boolean.valueOf(element.getAttributeValue(EXCLUDE, "true"));
        for (Element child2 : element.getChildren()) {
            String moduleUrl = child2.getAttributeValue(URL);
            String type = child2.getAttributeValue(TYPE);

            myOutputUrls.put(type, moduleUrl);
        }
    }
}
