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

import consulo.project.ui.view.tree.ModuleGroup;
import consulo.util.collection.ArrayUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Anna.Kozlova
 * @since 2006-07-16
 */
public class ModuleGroupUtil {
    private ModuleGroupUtil() {
    }

    public static <T> T buildModuleGroupPath(
        ModuleGroup group,
        T parentNode,
        Map<ModuleGroup, T> map,
        Consumer<ParentChildRelation<T>> insertNode,
        Function<ModuleGroup, T> createNewNode
    ) {
        List<String> path = new ArrayList<>();
        String[] groupPath = group.getGroupPath();
        for (String pathElement : groupPath) {
            path.add(pathElement);
            ModuleGroup moduleGroup = new ModuleGroup(ArrayUtil.toStringArray(path));
            T moduleGroupNode = map.get(moduleGroup);
            if (moduleGroupNode == null) {
                moduleGroupNode = createNewNode.apply(moduleGroup);
                map.put(moduleGroup, moduleGroupNode);
                insertNode.accept(new ParentChildRelation<>(parentNode, moduleGroupNode));
            }
            parentNode = moduleGroupNode;
        }
        return parentNode;
    }

    public static <T> T updateModuleGroupPath(
        ModuleGroup group,
        T parentNode,
        Function<ModuleGroup, T> needToCreateNode,
        Consumer<ParentChildRelation<T>> insertNode,
        Function<ModuleGroup, T> createNewNode
    ) {
        List<String> path = new ArrayList<>();
        String[] groupPath = group.getGroupPath();
        for (String pathElement : groupPath) {
            path.add(pathElement);
            ModuleGroup moduleGroup = new ModuleGroup(ArrayUtil.toStringArray(path));
            T moduleGroupNode = needToCreateNode.apply(moduleGroup);
            if (moduleGroupNode == null) {
                moduleGroupNode = createNewNode.apply(moduleGroup);
                insertNode.accept(new ParentChildRelation<>(parentNode, moduleGroupNode));
            }
            parentNode = moduleGroupNode;
        }
        return parentNode;
    }

    public static class ParentChildRelation<T> {
        private final T myParent;
        private final T myChild;

        public ParentChildRelation(T parent, T child) {
            myParent = parent;
            myChild = child;
        }

        public T getParent() {
            return myParent;
        }

        public T getChild() {
            return myChild;
        }
    }
}
