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
package consulo.language.editor.impl.internal.intention.setting;

import consulo.language.editor.internal.intention.IntentionActionMetaData;
import consulo.language.editor.internal.intention.IntentionManagerSettings;
import consulo.language.editor.internal.intention.TextDescriptor;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.internal.SearchHighlightUtil;
import consulo.ui.style.ComponentColors;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
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
public class IntentionTreeModel implements TreeModel<IntentionTreeNode> {
    private static final Logger LOG = Logger.getInstance(IntentionTreeModel.class);

    private static final Pattern HTML_TAG = Pattern.compile("<[^<>]*>");

    private static final Comparator<TreeNode<IntentionTreeNode>> COMPARATOR = Comparator
        .comparing((TreeNode<IntentionTreeNode> node) -> node.getValue() instanceof IntentionLeafNode)
        .thenComparing(node -> {
            IntentionTreeNode value = node.getValue();
            return value == null ? LocalizeValue.empty() : value.getText();
        }, LocalizeValue.comparator());

    private final Map<IntentionActionMetaData, String> myDescriptionTexts = new ConcurrentHashMap<>();

    private volatile @Nullable String myFilter;
    private volatile List<IntentionActionMetaData> myVisible = List.of();

    public void setFilter(@Nullable String filter) {
        myFilter = SearchHighlightUtil.normalizeQuery(filter);
    }

    public @Nullable String getFilter() {
        return myFilter;
    }

    public List<IntentionActionMetaData> getVisible() {
        return myVisible;
    }

    @Override
    public void buildChildren(Function<IntentionTreeNode, TreeNode<IntentionTreeNode>> nodeFactory, @Nullable IntentionTreeNode parentValue) {
        if (parentValue instanceof IntentionLeafNode) {
            return;
        }

        String filter = myFilter;
        if (parentValue == null) {
            myVisible = filter(IntentionManagerSettings.getInstance().getMetaData(), filter);
        }

        List<String> path = parentValue instanceof IntentionGroupNode group ? group.path() : List.of();

        Set<String> groups = new LinkedHashSet<>();
        List<IntentionActionMetaData> leaves = new ArrayList<>();
        for (IntentionActionMetaData metaData : myVisible) {
            String[] category = metaData.myCategory;
            if (!startsWith(category, path)) {
                continue;
            }

            if (category.length == path.size()) {
                leaves.add(metaData);
            }
            else {
                groups.add(category[path.size()]);
            }
        }

        for (String group : groups) {
            List<String> groupPath = new ArrayList<>(path);
            groupPath.add(group);

            TreeNode<IntentionTreeNode> node = nodeFactory.apply(new IntentionGroupNode(List.copyOf(groupPath)));
            node.setLeaf(false);
            node.setRenderer((value, presentation) -> appendHighlighted(presentation, value.getText(), filter, TextAttribute.STYLE_BOLD));
        }

        for (IntentionActionMetaData metaData : leaves) {
            TreeNode<IntentionTreeNode> node = nodeFactory.apply(new IntentionLeafNode(metaData));
            node.setLeaf(true);
            node.setRenderer((value, presentation) -> appendHighlighted(presentation, value.getText(), filter, TextAttribute.STYLE_PLAIN));
        }
    }

    @Override
    public @Nullable Comparator<TreeNode<IntentionTreeNode>> getNodeComparator() {
        return COMPARATOR;
    }

    public static boolean startsWith(String[] category, List<String> path) {
        if (category.length < path.size()) {
            return false;
        }

        for (int i = 0; i < path.size(); i++) {
            if (!path.get(i).equals(category[i])) {
                return false;
            }
        }
        return true;
    }

    private List<IntentionActionMetaData> filter(List<IntentionActionMetaData> metaData, @Nullable String filter) {
        if (filter == null) {
            return List.copyOf(metaData);
        }

        List<String> words = new ArrayList<>();
        for (String word : SearchHighlightUtil.words(filter)) {
            words.add(word.toLowerCase(Locale.ROOT));
        }

        List<IntentionActionMetaData> result = new ArrayList<>();
        for (IntentionActionMetaData each : metaData) {
            if (accepts(each, words)) {
                result.add(each);
            }
        }
        return List.copyOf(result);
    }

    private boolean accepts(IntentionActionMetaData metaData, List<String> words) {
        String text = searchText(metaData);
        for (String word : words) {
            if (!text.contains(word)) {
                return false;
            }
        }
        return true;
    }

    private String searchText(IntentionActionMetaData metaData) {
        StringBuilder builder = new StringBuilder(metaData.getActionText().get());
        for (String category : metaData.myCategory) {
            builder.append('\n').append(category);
        }
        builder.append('\n').append(myDescriptionTexts.computeIfAbsent(metaData, IntentionTreeModel::descriptionText));
        return builder.toString().toLowerCase(Locale.ROOT);
    }

    private static String descriptionText(IntentionActionMetaData metaData) {
        try {
            TextDescriptor description = metaData.getDescription();
            return description == null ? "" : HTML_TAG.matcher(description.getText()).replaceAll(" ");
        }
        catch (IOException e) {
            LOG.warn(e);
            return "";
        }
    }

    public static void appendHighlighted(TextItemPresentation presentation, LocalizeValue text, @Nullable String filter, int style) {
        TextAttribute plain = new TextAttribute(style, null, null);
        TextAttribute matched = new TextAttribute(style, ComponentColors.SEARCH_MATCH_FOREGROUND, ComponentColors.SEARCH_MATCH_BACKGROUND);

        SearchHighlightUtil.forEachFragment(
            text.get(),
            filter,
            (fragment, isMatch) -> presentation.append(LocalizeValue.of(fragment), isMatch ? matched : plain)
        );
    }
}
