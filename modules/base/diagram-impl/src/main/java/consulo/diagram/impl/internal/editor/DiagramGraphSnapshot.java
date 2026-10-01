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
package consulo.diagram.impl.internal.editor;

import consulo.annotation.access.RequiredReadAction;
import consulo.diagram.AbstractDiagramNodeContentManager;
import consulo.diagram.DiagramCategory;
import consulo.diagram.DiagramDataModel;
import consulo.diagram.DiagramEdge;
import consulo.diagram.DiagramElementManager;
import consulo.diagram.DiagramExtras;
import consulo.diagram.DiagramNode;
import consulo.diagram.DiagramNodeContentManager;
import consulo.diagram.DiagramNodesGroup;
import consulo.diagram.DiagramProvider;
import consulo.diagram.DiagramRelationshipInfo;
import consulo.diagram.DiagramUtil;
import consulo.diagram.DiagramVisibilityManager;
import consulo.diagram.VisibilityLevel;
import consulo.diagram.presentation.DiagramArrow;
import consulo.diagram.presentation.DiagramLineType;
import consulo.ui.TextAttribute;
import consulo.ui.ex.SimpleColoredText;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.graph.GraphArrow;
import consulo.ui.graph.GraphGroup;
import consulo.ui.graph.GraphLineStyle;
import consulo.ui.graph.GraphModel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public final class DiagramGraphSnapshot implements GraphModel<DiagramGraphNode> {
    private final List<DiagramGraphNode> myNodes;
    private final Map<DiagramGraphNode, Map<DiagramGraphNode, DiagramGraphEdgeStyle>> myArrows;

    private DiagramGraphSnapshot(List<DiagramGraphNode> nodes, Map<DiagramGraphNode, Map<DiagramGraphNode, DiagramGraphEdgeStyle>> arrows) {
        myNodes = nodes;
        myArrows = arrows;
    }

    @RequiredReadAction
    public static <T> DiagramGraphSnapshot of(DiagramProvider<T> provider, DiagramDataModel<T> model) {
        Map<DiagramNodesGroup, DiagramGraphGroup> groups = new HashMap<>();
        Map<DiagramNode<T>, DiagramGraphNode> nodes = new LinkedHashMap<>();
        for (DiagramNode<T> node : model.getNodes()) {
            T element = node.getIdentifyingElement();
            String tooltip = provider.getElementManager().getNodeTooltip(element);
            DiagramNodesGroup nodesGroup = model.getGroup(node);
            DiagramGraphGroup group = nodesGroup == null ? null : groups.computeIfAbsent(nodesGroup, DiagramGraphGroup::new);
            nodes.put(node, new DiagramGraphNode(node,
                model.getNodeName(node),
                node.getIcon(),
                buildSections(provider, element),
                tooltip == null ? "" : tooltip,
                group));
        }

        Map<DiagramGraphNode, Map<DiagramGraphNode, DiagramGraphEdgeStyle>> arrows = new HashMap<>();
        for (DiagramEdge<T> edge : model.getEdges()) {
            DiagramGraphNode source = nodes.get(model.getSourceNode(edge));
            DiagramGraphNode target = nodes.get(model.getTargetNode(edge));
            if (source != null && target != null && source != target) {
                arrows.computeIfAbsent(target, it -> new LinkedHashMap<>()).put(source, buildEdgeStyle(provider, model, edge));
            }
        }
        return new DiagramGraphSnapshot(List.copyOf(nodes.values()), arrows);
    }

    private static <T> List<List<DiagramGraphRow>> buildSections(DiagramProvider<T> provider, T element) {
        DiagramElementManager<T> elementManager = provider.getElementManager();
        DiagramNodeContentManager contentManager = provider.getNodeContentManager();
        DiagramVisibilityManager visibilityManager = provider.getVisibilityManager();
        VisibilityLevel currentLevel = visibilityManager.getCurrentVisibilityLevel();
        Comparator<VisibilityLevel> comparator = visibilityManager.getComparator();

        List<List<DiagramGraphRow>> sections = new ArrayList<>();
        for (DiagramCategory category : contentManager.getContentCategories()) {
            if (contentManager instanceof AbstractDiagramNodeContentManager manager && !manager.isEnabled(category)) {
                continue;
            }

            List<DiagramGraphRow> rows = new ArrayList<>();
            for (Object item : DiagramUtil.getNodeItemsForCategory(provider, element, category)) {
                VisibilityLevel level = visibilityManager.getVisibilityLevel(item);
                if (currentLevel != null && level != null && comparator.compare(level, currentLevel) > 0) {
                    continue;
                }
                rows.add(buildRow(elementManager, item));
            }

            if (!rows.isEmpty()) {
                sections.add(rows);
            }
        }
        return sections;
    }

    private static DiagramGraphRow buildRow(DiagramElementManager<?> elementManager, Object item) {
        List<DiagramGraphFragment> fragments = new ArrayList<>();

        SimpleColoredText name = elementManager.getPresentableName(item);
        if (name == null) {
            fragments.add(new DiagramGraphFragment(String.valueOf(item), TextAttribute.REGULAR));
        }
        else {
            appendFragments(fragments, name);
        }

        SimpleColoredText type = elementManager.getPresentableType(item);
        if (type != null) {
            fragments.add(new DiagramGraphFragment(" : ", TextAttribute.GRAYED));
            appendFragments(fragments, type);
        }
        return new DiagramGraphRow(elementManager.getNodeElementIcon(item), fragments);
    }

    private static void appendFragments(List<DiagramGraphFragment> fragments, SimpleColoredText text) {
        List<String> texts = text.getTexts();
        List<SimpleTextAttributes> attributes = text.getAttributes();
        for (int i = 0; i < texts.size(); i++) {
            SimpleTextAttributes attribute = attributes.get(i);
            fragments.add(new DiagramGraphFragment(texts.get(i),
                new TextAttribute(attribute.getStyle(), attribute.foreground(), attribute.background())));
        }
    }

    private static <T> DiagramGraphEdgeStyle buildEdgeStyle(DiagramProvider<T> provider, DiagramDataModel<T> model, DiagramEdge<T> edge) {
        DiagramRelationshipInfo relationship = edge.getRelationship();

        String label = relationship.getLabel();
        if (label == null || label.isBlank()) {
            label = model.getEdgeName(edge);
        }

        DiagramExtras<T> extras = provider.getExtras();
        String tooltip = extras == null ? null : extras.getEdgeTooltip(edge);

        return new DiagramGraphEdgeStyle(toLineStyle(relationship.getLineType()),
            toArrow(relationship.getStartArrow()),
            toArrow(relationship.getEndArrow()),
            label,
            tooltip == null ? "" : tooltip,
            provider.getColorManager().getEdgeColor(edge));
    }

    private static GraphLineStyle toLineStyle(@Nullable DiagramLineType lineType) {
        if (lineType == null) {
            return GraphLineStyle.SOLID;
        }
        return switch (lineType) {
            case SOLID -> GraphLineStyle.SOLID;
            case DASHED -> GraphLineStyle.DASHED;
            case DOTTED -> GraphLineStyle.DOTTED;
        };
    }

    private static GraphArrow toArrow(@Nullable DiagramArrow arrow) {
        if (arrow == null) {
            return GraphArrow.NONE;
        }
        return switch (arrow) {
            case NONE -> GraphArrow.NONE;
            case ANGLE -> GraphArrow.OPEN;
            case DELTA -> GraphArrow.TRIANGLE;
            case DIAMOND -> GraphArrow.DIAMOND;
            case INNER_CLASS -> GraphArrow.CIRCLE;
        };
    }

    @Override
    public Collection<DiagramGraphNode> getNodes() {
        return myNodes;
    }

    @Override
    public Collection<DiagramGraphNode> getArrows(DiagramGraphNode node) {
        Map<DiagramGraphNode, DiagramGraphEdgeStyle> targets = myArrows.get(node);
        return targets == null ? List.of() : targets.keySet();
    }

    @Override
    public @Nullable GraphGroup getGroup(DiagramGraphNode node) {
        return node.getGroup();
    }

    public @Nullable DiagramGraphEdgeStyle getEdgeStyle(DiagramGraphNode source, DiagramGraphNode target) {
        Map<DiagramGraphNode, DiagramGraphEdgeStyle> targets = myArrows.get(source);
        return targets == null ? null : targets.get(target);
    }
}
