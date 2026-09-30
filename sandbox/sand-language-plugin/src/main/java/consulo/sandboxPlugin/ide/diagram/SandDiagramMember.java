/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.ide.diagram;

import consulo.application.AllIcons;
import consulo.diagram.DiagramCategory;
import consulo.ui.image.Image;

import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public record SandDiagramMember(String name, String type, DiagramCategory category) {
    public static final DiagramCategory FIELDS = new DiagramCategory("Fields", AllIcons.Nodes.Field);
    public static final DiagramCategory METHODS = new DiagramCategory("Methods", AllIcons.Nodes.Method);

    private static final Map<String, List<SandDiagramMember>> MEMBERS = Map.of(
        "Object", List.of(method("hashCode()", "int"), method("equals(Object)", "boolean"), method("toString()", "String")),
        "Collection", List.of(method("size()", "int"), method("isEmpty()", "boolean"), method("add(E)", "boolean")),
        "List", List.of(method("get(int)", "E"), method("indexOf(Object)", "int")),
        "Set", List.of(method("contains(Object)", "boolean")),
        "ArrayList", List.of(field("elementData", "Object[]"), field("size", "int"), method("ensureCapacity(int)", "void")),
        "HashSet", List.of(field("map", "HashMap<E, Object>"))
    );

    public static List<SandDiagramMember> of(String className) {
        return MEMBERS.getOrDefault(className, List.of());
    }

    public Image icon() {
        return category == FIELDS ? AllIcons.Nodes.Field : AllIcons.Nodes.Method;
    }

    private static SandDiagramMember field(String name, String type) {
        return new SandDiagramMember(name, type, FIELDS);
    }

    private static SandDiagramMember method(String name, String type) {
        return new SandDiagramMember(name, type, METHODS);
    }
}
