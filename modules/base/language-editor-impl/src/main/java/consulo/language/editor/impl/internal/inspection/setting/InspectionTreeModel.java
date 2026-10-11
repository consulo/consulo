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
package consulo.language.editor.impl.internal.inspection.setting;

import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.color.ColorValue;
import consulo.ui.internal.SearchHighlightUtil;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class InspectionTreeModel implements TreeModel<InspectionTreeNode> {
    private static final Pattern HTML_TAG = Pattern.compile("<[^<>]*>");

    private static final Comparator<TreeNode<InspectionTreeNode>> COMPARATOR = Comparator
        .comparing((TreeNode<InspectionTreeNode> node) -> node.getValue() instanceof InspectionLeafNode)
        .thenComparing(node -> {
            InspectionTreeNode value = node.getValue();
            return value == null ? LocalizeValue.empty() : value.getText();
        }, LocalizeValue.comparator());

    private final Function<String, @Nullable String> myDescriptionLoader;
    private final Map<String, String> mySearchTexts = new ConcurrentHashMap<>();
    private final Map<InspectionTreeNode, TreeNode<InspectionTreeNode>> myNodes = new ConcurrentHashMap<>();

    private volatile List<InspectionRow> myRows = List.of();
    private volatile Map<String, InspectionRow> myRowsByName = Map.of();
    private volatile List<InspectionRow> myVisible = List.of();
    private volatile Set<List<LocalizeValue>> myModifiedGroups = Set.of();
    private volatile @Nullable String myFilter;

    public InspectionTreeModel(Function<String, @Nullable String> descriptionLoader) {
        myDescriptionLoader = descriptionLoader;
    }

    public void setFilter(@Nullable String filter) {
        myFilter = SearchHighlightUtil.normalizeQuery(filter);
    }

    public @Nullable String getFilter() {
        return myFilter;
    }

    public void setRows(List<InspectionRow> rows) {
        Map<String, InspectionRow> byName = new LinkedHashMap<>();
        for (InspectionRow row : rows) {
            byName.put(row.shortName(), row);
        }
        myRows = List.copyOf(rows);
        myRowsByName = byName;
    }

    public void updateRows(Collection<InspectionRow> rows) {
        Map<String, InspectionRow> byName = new LinkedHashMap<>(myRowsByName);
        for (InspectionRow row : rows) {
            if (byName.containsKey(row.shortName())) {
                byName.put(row.shortName(), row);
            }
        }
        myRowsByName = byName;
        myRows = List.copyOf(byName.values());

        List<InspectionRow> visible = new ArrayList<>(myVisible.size());
        for (InspectionRow row : myVisible) {
            visible.add(byName.getOrDefault(row.shortName(), row));
        }
        setVisible(visible);
    }

    public List<InspectionRow> getRows() {
        return myRows;
    }

    public List<InspectionRow> getVisible() {
        return myVisible;
    }

    public @Nullable InspectionRow getRow(String shortName) {
        return myRowsByName.get(shortName);
    }

    public @Nullable TreeNode<InspectionTreeNode> findNode(InspectionTreeNode value) {
        return myNodes.get(value);
    }

    public List<InspectionRow> getVisibleRows(InspectionTreeNode value) {
        if (value instanceof InspectionLeafNode leaf) {
            InspectionRow row = myRowsByName.get(leaf.shortName());
            return row == null ? List.of() : List.of(row);
        }

        InspectionGroupNode group = (InspectionGroupNode) value;
        List<InspectionRow> result = new ArrayList<>();
        for (InspectionRow row : myVisible) {
            if (group.contains(row)) {
                result.add(row);
            }
        }
        return result;
    }

    public Set<InspectionGroupNode> getVisibleGroups() {
        Set<InspectionGroupNode> groups = new LinkedHashSet<>();
        for (InspectionRow row : myVisible) {
            List<LocalizeValue> path = row.groupPath();
            for (int i = 1; i <= path.size(); i++) {
                groups.add(new InspectionGroupNode(List.copyOf(path.subList(0, i))));
            }
        }
        return groups;
    }

    public @Nullable String loadDescription(String shortName) {
        return myDescriptionLoader.apply(shortName);
    }

    @Override
    public void buildChildren(Function<InspectionTreeNode, TreeNode<InspectionTreeNode>> nodeFactory, @Nullable InspectionTreeNode parentValue) {
        if (parentValue instanceof InspectionLeafNode) {
            return;
        }

        if (parentValue == null) {
            myNodes.clear();
            setVisible(filter(myRows, myFilter));
        }

        List<LocalizeValue> path = parentValue instanceof InspectionGroupNode group ? group.path() : List.of();

        Set<LocalizeValue> groups = new LinkedHashSet<>();
        List<InspectionRow> leaves = new ArrayList<>();
        for (InspectionRow row : myVisible) {
            List<LocalizeValue> groupPath = row.groupPath();
            if (groupPath.size() < path.size() || !groupPath.subList(0, path.size()).equals(path)) {
                continue;
            }

            if (groupPath.size() == path.size()) {
                leaves.add(row);
            }
            else {
                groups.add(groupPath.get(path.size()));
            }
        }

        for (LocalizeValue group : groups) {
            List<LocalizeValue> groupPath = new ArrayList<>(path);
            groupPath.add(group);

            InspectionGroupNode value = new InspectionGroupNode(List.copyOf(groupPath));
            TreeNode<InspectionTreeNode> node = nodeFactory.apply(value);
            node.setLeaf(false);
            node.setRenderer(this::render);
            myNodes.put(value, node);
        }

        for (InspectionRow row : leaves) {
            InspectionLeafNode value = row.toNode();
            TreeNode<InspectionTreeNode> node = nodeFactory.apply(value);
            node.setLeaf(true);
            node.setRenderer(this::render);
            myNodes.put(value, node);
        }
    }

    @Override
    public @Nullable Comparator<TreeNode<InspectionTreeNode>> getNodeComparator() {
        return COMPARATOR;
    }

    private void render(InspectionTreeNode value, TextItemPresentation presentation) {
        String filter = myFilter;
        if (value instanceof InspectionGroupNode group) {
            ColorValue foreground = myModifiedGroups.contains(group.path()) ? ComponentColors.LINK_FOREGROUND : null;
            appendHighlighted(presentation, group.getText(), filter, TextAttribute.STYLE_BOLD, foreground);
            return;
        }

        InspectionLeafNode leaf = (InspectionLeafNode) value;
        InspectionRow row = myRowsByName.get(leaf.shortName());
        ColorValue foreground = row != null && row.modified() ? ComponentColors.LINK_FOREGROUND : null;
        appendHighlighted(presentation, leaf.getText(), filter, TextAttribute.STYLE_PLAIN, foreground);
        if (row == null) {
            return;
        }

        if (row.batchOnly()) {
            presentation.append(LocalizeValue.of(" "), TextAttribute.GRAYED);
            presentation.append(InspectionLocalize.inspectionToolAvailabilityInTreeNode1(), TextAttribute.GRAYED);
        }

        if (row.severityIcon() != null) {
            presentation.withSuffix(LocalizeValue.empty(), row.severityIcon());
        }
    }

    private void setVisible(List<InspectionRow> visible) {
        Set<List<LocalizeValue>> modifiedGroups = new HashSet<>();
        for (InspectionRow row : visible) {
            if (!row.modified()) {
                continue;
            }

            List<LocalizeValue> path = row.groupPath();
            for (int i = 1; i <= path.size(); i++) {
                modifiedGroups.add(List.copyOf(path.subList(0, i)));
            }
        }

        myVisible = List.copyOf(visible);
        myModifiedGroups = modifiedGroups;
    }

    private List<InspectionRow> filter(List<InspectionRow> rows, @Nullable String filter) {
        if (filter == null) {
            return rows;
        }

        List<String> words = new ArrayList<>();
        for (String word : SearchHighlightUtil.words(filter)) {
            words.add(word.toLowerCase(Locale.ROOT));
        }

        List<InspectionRow> result = new ArrayList<>();
        for (InspectionRow row : rows) {
            String text = searchText(row);
            boolean accepted = true;
            for (String word : words) {
                if (!text.contains(word)) {
                    accepted = false;
                    break;
                }
            }

            if (accepted) {
                result.add(row);
            }
        }
        return result;
    }

    private String searchText(InspectionRow row) {
        return mySearchTexts.computeIfAbsent(row.shortName(), shortName -> {
            StringBuilder builder = new StringBuilder(row.text().get());
            for (LocalizeValue group : row.groupPath()) {
                builder.append('\n').append(group.get());
            }
            String description = myDescriptionLoader.apply(shortName);
            if (description != null) {
                builder.append('\n').append(HTML_TAG.matcher(description).replaceAll(" "));
            }
            return builder.toString().toLowerCase(Locale.ROOT);
        });
    }

    private static void appendHighlighted(TextItemPresentation presentation, LocalizeValue text, @Nullable String filter, int style, @Nullable ColorValue foreground) {
        TextAttribute plain = new TextAttribute(style, foreground, null);
        TextAttribute matched = new TextAttribute(style, ComponentColors.SEARCH_MATCH_FOREGROUND, ComponentColors.SEARCH_MATCH_BACKGROUND);

        SearchHighlightUtil.forEachFragment(
            text.get(),
            filter,
            (fragment, isMatch) -> presentation.append(LocalizeValue.of(fragment), isMatch ? matched : plain)
        );
    }
}
