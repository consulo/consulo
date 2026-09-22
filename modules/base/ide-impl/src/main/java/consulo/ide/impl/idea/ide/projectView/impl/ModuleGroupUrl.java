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
package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.project.Project;
import consulo.project.ui.view.internal.AbstractUrl;
import consulo.project.ui.view.tree.ModuleGroup;
import org.jspecify.annotations.Nullable;

public class ModuleGroupUrl extends AbstractUrl {
    private static final String ELEMENT_TYPE = "module_group";

    public ModuleGroupUrl(String url) {
        super(url, null, ELEMENT_TYPE);
    }

    @Override
    public Object @Nullable [] createPath(Project project) {
        String[] groupPath = url.split(";");
        return new Object[]{new ModuleGroup(groupPath)};
    }

    @Override
    protected AbstractUrl createUrl(String moduleName, String url) {
        return new ModuleGroupUrl(url);
    }

    @Override
    public AbstractUrl createUrlByElement(Object element) {
        if (element instanceof ModuleGroup) {
            ModuleGroup group = (ModuleGroup) element;
            String[] groupPath = group.getGroupPath();
            StringBuilder sb = new StringBuilder();
            for (String s : groupPath) {
                sb.append(s).append(";");
            }
            return new ModuleGroupUrl(sb.toString());
        }
        return null;
    }
}
