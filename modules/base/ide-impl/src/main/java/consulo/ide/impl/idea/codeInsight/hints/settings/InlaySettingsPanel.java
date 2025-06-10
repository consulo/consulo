// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.settings;

import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.hints.DeclarativeInlayHintsPassFactory;
import consulo.ide.impl.idea.codeInsight.hints.ParameterHintsPassFactory;
import consulo.ide.impl.idea.util.ui.SwingHelper;
import consulo.language.Language;
import consulo.language.editor.inlay.InlayGroup;
import consulo.language.file.LanguageFileType;
import consulo.language.plain.PlainTextFileType;
import consulo.language.util.LanguageUtil;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.ui.ex.awt.JBSplitter;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.CheckboxTree;
import consulo.ui.ex.awt.tree.CheckedTreeNode;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.util.dataholder.Key;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InlaySettingsPanel extends JPanel {
    public static final Key<ImmediateConfigurable.Case> CASE_KEY = Key.create("inlay.case.key");

    private final Project project;
    private final CheckboxTree tree;
    private final JPanel rightPanel = new JPanel(new MigLayout("wrap, insets 0 10 0 0, gapy 20, fillx"));
    private final Map<InlayGroup, List<InlayProviderSettingsModel>> groups;
    private Editor currentEditor;

    public InlaySettingsPanel(Project project) {
        super(new BorderLayout());
        this.project = project;
        List<InlayProviderSettingsModel> models = InlaySettingsProvider.getExtensions()
            .stream()
            .flatMap(provider -> provider.getSupportedLanguages(project).stream()
            .flatMap(lang -> provider.createModels(project, lang).stream()))
            .collect(Collectors.toList());
        this.groups = models.stream().collect(Collectors.groupingBy(InlayProviderSettingsModel::getGroup, TreeMap::new, Collectors.toList()));
        Map<InlayGroup, InlayGroupSettingProvider> globalSettings = new HashMap<>();
        for (InlayGroup group : groups.keySet()) {
            globalSettings.put(group, InlayGroupSettingProvider.findForGroup(group));
        }

        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        String lastSelected = InlayHintsSettings.instance().getLastViewedProviderId();
        DefaultMutableTreeNode nodeToSelect = null;

        if (Registry.is("editor.codeVision.new")) {
            groups.remove(InlayGroup.CODE_VISION_GROUP);
        }

        for (Map.Entry<InlayGroup, List<InlayProviderSettingsModel>> entry : groups.entrySet()) {
            InlayGroup group = entry.getKey();
            List<InlayProviderSettingsModel> list = entry.getValue();
            DefaultMutableTreeNode groupNode = new CheckedTreeNode(globalSettings.getOrDefault(group, group));
            root.add(groupNode);

            List<Language> primary = List.of();
            TreeMap<Language, List<InlayProviderSettingsModel>> sorted = new TreeMap<>(Comparator.comparing(Language::getDisplayName));
            for (InlayProviderSettingsModel model : list) {
                sorted.computeIfAbsent(model.getLanguage(), k -> new ArrayList<>()).add(model);
            }

            for (Map.Entry<Language, List<InlayProviderSettingsModel>> langEntry : sorted.entrySet()) {
                Language lang = langEntry.getKey();
                List<InlayProviderSettingsModel> langModels = langEntry.getValue();
                DefaultMutableTreeNode langNode;
                int start = 0;
                InlayProviderSettingsModel first = langModels.get(0);
                if ((langModels.size() == 1 || (group.toString().equals(first.getName()) && first.getLanguage() == sorted.firstKey()))
                    && InlayGroup.OTHER_GROUP != group) {
                    langNode = groupNode;
                    nodeToSelect = addModelNode(first, groupNode, lastSelected, nodeToSelect);
                    first.setMergedNode(true);
                    start = 1;
                }
                else if (lang == Language.ANY) {
                    langNode = groupNode;
                }
                else {
                    langNode = new CheckedTreeNode(lang);
                    groupNode.add(langNode);
                }
                for (int i = start; i < langModels.size(); i++) {
                    nodeToSelect = addModelNode(langModels.get(i), langNode, lastSelected, nodeToSelect);
                }
            }
        }

        this.tree = new CheckboxTree(new InlaySettingsTreeRenderer(), root, new CheckboxTree.CheckPolicy(true, true, true, false)) {
            @Override
            protected void installSpeedSearch() {
                TreeSpeedSearch.installOn(this, true,
                    node -> getNameImpl((DefaultMutableTreeNode) node.getLastPathComponent(),
                        (DefaultMutableTreeNode) node.getParentPath().getLastPathComponent()));
            }
        };
        this.tree.addTreeSelectionListener(e -> updateRightPanel((DefaultMutableTreeNode) e.getNewLeadSelectionPath().getLastPathComponent()));
        if (nodeToSelect == null) {
            TreeUtil.expand(tree, 1);
        }
        else {
            TreeUtil.selectNode(tree, nodeToSelect);
        }

        JBSplitter splitter = new JBSplitter(false, "inlay.settings.proportion.key", 0.45f);
        splitter.setHonorComponentsMinimumSize(false);
        splitter.setFirstComponent(ScrollPaneFactory.createScrollPane(tree, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
        splitter.setSecondComponent(rightPanel);
        add(splitter, BorderLayout.CENTER);
    }

    public CheckboxTree getTree() {
        return tree;
    }

    public static LanguageFileType getFileTypeForPreview(InlayProviderSettingsModel model) {
        LanguageFileType ft = model.getCasePreviewLanguage(null) != null
            ? model.getCasePreviewLanguage(null).getAssociatedFileType()
            : PlainTextFileType.INSTANCE;
        return ft;
    }

    protected String getNameImpl(DefaultMutableTreeNode node, DefaultMutableTreeNode parent) {
        Object obj = node.getUserObject();
        if (obj instanceof InlayGroupSettingProvider) {
            return ((InlayGroupSettingProvider) obj).getGroup().title().get();
        }
        if (obj instanceof InlayGroup) {
            return ((InlayGroup) obj).title().get();
        }
        if (obj instanceof Language) {
            return ((Language) obj).getDisplayName();
        }
        if (obj instanceof InlayProviderSettingsModel) {
            return (parent.getUserObject() instanceof InlayGroup)
                ? ((InlayProviderSettingsModel) obj).getLanguage().getDisplayName()
                : ((InlayProviderSettingsModel) obj).getName();
        }
        if (obj instanceof ImmediateConfigurable.Case) {
            return ((ImmediateConfigurable.Case) obj).getName();
        }
        return "";
    }

    private DefaultMutableTreeNode addModelNode(InlayProviderSettingsModel model,
                                                DefaultMutableTreeNode parent,
                                                String lastId,
                                                DefaultMutableTreeNode toSelect) {
        model.setOnChangeListener(() -> {
            if (currentEditor != null) {
                ImmediateConfigurable.Case c = (ImmediateConfigurable.Case) currentEditor.getUserData(PREVIEW_KEY);
                updateHints(currentEditor, model, c);
            }
        });
        CheckedTreeNode node = new CheckedTreeNode(model) {
            @Override
            public void setChecked(boolean checked) {
                super.setChecked(checked);
                model.setEnabled(checked);
                if (model.getOnChangeListener() != null) {
                    model.getOnChangeListener().settingsChanged();
                }
            }
        };
        parent.add(node);
        DefaultMutableTreeNode result = toSelect;
        for (ImmediateConfigurable.Case c : model.getCases()) {
            CaseCheckedNode cn = new CaseCheckedNode(c, () -> currentEditor, model);
            node.add(cn);
            if (result == null && getProviderId(cn).equals(lastId)) {
                result = cn;
            }
        }
        if (result == null && getProviderId(node).equals(lastId)) {
            result = node;
        }
        return result;
    }

    private static class CaseCheckedNode extends CheckedTreeNode {
        private final ImmediateConfigurable.Case caze;
        private final java.util.function.Supplier<Editor> editorProvider;
        private final InlayProviderSettingsModel model;

        CaseCheckedNode(ImmediateConfigurable.Case caze,
                        java.util.function.Supplier<Editor> editorProvider,
                        InlayProviderSettingsModel model) {
            super(caze);
            this.caze = caze;
            this.editorProvider = editorProvider;
            this.model = model;
        }

        @Override
        public void setChecked(boolean checked) {
            super.setChecked(checked);
            caze.setValue(checked);
            if (PREVIEW_KEY.get(editorProvider.get()) == this) {
                model.getOnChangeListener().settingsChanged();
            }
        }
    }

    private void updateRightPanel(DefaultMutableTreeNode node) {
        rightPanel.removeAll();
        currentEditor = null;
        Object obj = node.getUserObject();
        if (obj instanceof InlayGroup) {
            addDescription(((InlayGroup) obj).description().get());
        }
        else if (obj instanceof InlayGroupSettingProvider) {
            addDescription(((InlayGroupSettingProvider) obj).getGroup().description().get());
            rightPanel.add(((InlayGroupSettingProvider) obj).getComponent());
        }
        else if (obj instanceof Language) {
            configureLanguageNode(node);
            configurePreview((InlayProviderSettingsModel) ((CheckedTreeNode) node.getFirstChild()).getUserObject(), node);
        }
        else if (obj instanceof InlayProviderSettingsModel) {
            InlayProviderSettingsModel m = (InlayProviderSettingsModel) obj;
            if (m.isMergedNode() && m.getDescription() == null) {
                configureLanguageNode(node);
            }
            if (m.getDescription() != null) {
                addDescription(m.getDescription());
            }
            if (!(m.getComponent() instanceof JPanel) || m.getComponent().getComponentCount() > 0) {
                m.getComponent().setBorder(JBUI.Borders.empty());
                rightPanel.add(m.getComponent());
            }
            if (node.isLeaf()) {
                addPreview(m.getCasePreview(null) != null ? m.getCasePreview(null) : m.getPreviewText(), m, null, node);
            }
            else if (m.isMergedNode()) {
                configurePreview(m, node);
            }
        }
        else if (obj instanceof ImmediateConfigurable.Case) {
            ImmediateConfigurable.Case c = (ImmediateConfigurable.Case) obj;
            InlayProviderSettingsModel m = (InlayProviderSettingsModel) ((CheckedTreeNode) node.getParent()).getUserObject();
            addDescription(m.getCaseDescription(c));
            addPreview(m.getCasePreview(c), m, c, node);
        }
        InlayHintsSettings.instance().saveLastViewedProviderId(getProviderId(node));
        rightPanel.revalidate();
        rightPanel.repaint();
    }

    private void configurePreview(InlayProviderSettingsModel model, DefaultMutableTreeNode node) {
        String previewText = model.getCasePreview(null) != null ? model.getCasePreview(null) : model.getPreviewText();
        if (previewText != null) {
            addPreview(previewText, model, null, node);
        }
        else {
            for (ImmediateConfigurable.Case c : model.getCases()) {
                String p = model.getCasePreview(c);
                if (p != null) {
                    addPreview(p, model, c, node);
                    break;
                }
            }
        }
    }

    private void configureLanguageNode(DefaultMutableTreeNode node) {
        String desc = ((InlayGroup) ((DefaultMutableTreeNode) node.getParent()).getUserObject()).description().get();
        addDescription(desc);
    }

    private void addPreview(String text,
                            InlayProviderSettingsModel model,
                            ImmediateConfigurable.Case caze,
                            DefaultMutableTreeNode node) {
        if (text == null) {
            return;
        }
        Editor editorField = SettingsLanguageKt.createEditor(model.getCasePreviewLanguage(null) != null
            ? model.getCasePreviewLanguage(null) : model.getLanguage(), project, editor -> {
            currentEditor = editor;
            PREVIEW_KEY.set(editor, node);
            CASE_KEY.set(editor, caze);
            updateHints(editor, model, caze);
        });
        editorField.getDocument().setText(text);
        editorField.getSettings().setLineNumbersShown(false);
        editorField.getSettings().setCaretRowShown(false);
        editorField.getSettings().setRightMarginShown(false);
        rightPanel.add(ScrollPaneFactory.createScrollPane(editorField), "growx");
    }

    private void updateHints(Editor editor,
                             InlayProviderSettingsModel model,
                             ImmediateConfigurable.Case caze) {
        ReadAction.nonBlocking(() -> {
                var file = model.createFile(project, getFileTypeForPreview(model), editor.getDocument(), caze != null ? caze.getId() : null);
                return model.collectData(editor, file);
            })
            .finishOnUiThread(ModalityState.stateForComponent(this), continuation ->
                ApplicationManager.getApplication().runWriteAction(continuation::run))
            .expireWhen(editor::isDisposed)
            .inSmartMode(project)
            .submit(AppExecutorUtil.getAppExecutorService());
    }

    private void addDescription(@Nls String s) {
        if (s != null) {
            rightPanel.add(SwingHelper.createHtmlLabel(s), "growy");
        }
    }

    private String getProviderId(DefaultMutableTreeNode node) {
        Object obj = node.getUserObject();
        if (obj instanceof InlayProviderSettingsModel) {
            InlayProviderSettingsModel m = (InlayProviderSettingsModel) obj;
            return m.getLanguage().getID() + "." + m.getId();
        }
        if (obj instanceof ImmediateConfigurable.Case) {
            ImmediateConfigurable.Case c = (ImmediateConfigurable.Case) obj;
            InlayProviderSettingsModel m = (InlayProviderSettingsModel) ((CheckedTreeNode) node.getParent()).getUserObject();
            return m.getLanguage().getID() + "." + m.getId() + "." + c.getId();
        }
        return "";
    }

    public void reset() {
        reset((CheckedTreeNode) tree.getModel().getRoot(), InlayHintsSettings.instance());
    }

    private void reset(CheckedTreeNode node, InlayHintsSettings settings) {
        Object obj = node.getUserObject();
        if (obj instanceof InlayGroupSettingProvider) {
            ((InlayGroupSettingProvider) obj).reset();
            node.setChecked(((InlayGroupSettingProvider) obj).isEnabled());
        }
        else if (obj instanceof InlayProviderSettingsModel) {
            InlayProviderSettingsModel m = (InlayProviderSettingsModel) obj;
            m.reset();
            node.setChecked(isModelEnabled(m, settings));
        }
        else if (obj instanceof ImmediateConfigurable.Case) {
            node.setChecked(isCaseEnabled((ImmediateConfigurable.Case) obj, (CheckedTreeNode) node.getParent(), settings));
        }
        else if (obj instanceof Language) {
            node.setChecked(settings.hintsEnabled((Language) obj));
        }
        for (Enumeration<?> e = node.children(); e.hasMoreElements(); ) {
            reset((CheckedTreeNode) e.nextElement(), settings);
        }
        if (obj instanceof InlayGroup) {
            node.setChecked(groups.get(obj).stream().anyMatch(InlayProviderSettingsModel::isEnabled));
        }
    }

    private boolean isCaseEnabled(ImmediateConfigurable.Case caze, CheckedTreeNode parent, InlayHintsSettings settings) {
        return caze.getValue()
            && ((InlayProviderSettingsModel) parent.getUserObject()).isEnabled()
            && settings.hintsEnabledGlobally();
    }

    private boolean isModelEnabled(InlayProviderSettingsModel m, InlayHintsSettings settings) {
        return m.isEnabled()
            && (!m.isMergedNode() || settings.hintsEnabled(m.getLanguage()))
            && settings.hintsEnabledGlobally();
    }

    public void apply() {
        apply((CheckedTreeNode) tree.getModel().getRoot(), InlayHintsSettings.instance());
        ParameterHintsPassFactory.forceHintsUpdateOnNextPass();
        DeclarativeInlayHintsPassFactory.resetModificationStamp();
        InlayHintsPassFactoryInternal.restartDaemonUpdatingHints(project, "InlaySettingsPanel.apply()");
    }

    private void apply(CheckedTreeNode node, InlayHintsSettings settings) {
        if (!settings.hintsEnabledGlobally() && !isAnyCheckboxEnabled(node)) {
            return;
        }
        for (Enumeration<?> e = node.children(); e.hasMoreElements(); ) {
            apply((CheckedTreeNode) e.nextElement(), settings);
        }
        Object obj = node.getUserObject();
        if (obj instanceof InlayGroupSettingProvider) {
            ((InlayGroupSettingProvider) obj).setEnabled(node.isChecked());
            ((InlayGroupSettingProvider) obj).apply();
        }
        else if (obj instanceof InlayProviderSettingsModel) {
            InlayProviderSettingsModel m = (InlayProviderSettingsModel) obj;
            m.setEnabled(node.isChecked());
            m.apply();
            if (m.isMergedNode()) {
                settings.setHintsEnabledForLanguage(m.getLanguage(), true);
            }
        }
        else if (obj instanceof ImmediateConfigurable.Case) {
            ImmediateConfigurable.Case c = (ImmediateConfigurable.Case) obj;
            c.setValue(node.isChecked());
            if (node.isChecked()) {
                settings.setEnabledGlobally(true);
            }
        }
        else if (obj instanceof Language) {
            settings.setHintsEnabledForLanguage((Language) obj, node.isChecked());
        }
    }

    private boolean isAnyCheckboxEnabled(CheckedTreeNode node) {
        for (Enumeration<?> e = node.children(); e.hasMoreElements(); ) {
            if (isAnyCheckboxEnabled((CheckedTreeNode) e.nextElement())) {
                return true;
            }
        }
        Object obj = node.getUserObject();
        return (obj instanceof InlayGroupSettingProvider && node.isChecked())
            || (obj instanceof InlayProviderSettingsModel && node.isChecked())
            || (obj instanceof ImmediateConfigurable.Case && node.isChecked());
    }

    public boolean isModified() {
        return isModified((CheckedTreeNode) tree.getModel().getRoot(), InlayHintsSettings.instance());
    }

    private boolean isModified(CheckedTreeNode node, InlayHintsSettings settings) {
        Object obj = node.getUserObject();
        if (obj instanceof InlayGroupSettingProvider && ((InlayGroupSettingProvider) obj).isModified()) {
            return true;
        }
        if (obj instanceof InlayProviderSettingsModel) {
            InlayProviderSettingsModel m = (InlayProviderSettingsModel) obj;
            if (m.isModified() || node.isChecked() != isModelEnabled(m, settings)) {
                return true;
            }
        }
        if (obj instanceof ImmediateConfigurable.Case && node.isChecked() != isCaseEnabled((ImmediateConfigurable.Case) obj, (CheckedTreeNode) node.getParent(), settings)) {
            return true;
        }
        if (obj instanceof Language && settings.hintsEnabled((Language) obj) != node.isChecked()) {
            return true;
        }
        for (Enumeration<?> e = node.children(); e.hasMoreElements(); ) {
            if (isModified((CheckedTreeNode) e.nextElement(), settings)) {
                return true;
            }
        }
        return false;
    }

    public Runnable enableSearch(String option) {
        if (option == null) {
            return null;
        }
        return () -> {
            DefaultMutableTreeNode node = TreeUtil.findNode((DefaultMutableTreeNode) tree.getModel().getRoot(),
                n -> getNameImpl(n, (DefaultMutableTreeNode) n.getParent()).toLowerCase().startsWith(option.toLowerCase()));
            if (node != null) {
                TreeUtil.selectNode(tree, node);
            }
        };
    }

    public void selectModel(Language language, Predicate<InlayProviderSettingsModel> selector) {
        Set<Language> langs = new HashSet<>(LanguageUtil.getBaseLanguages(language).toList());
        DefaultMutableTreeNode node = TreeUtil.findNode((DefaultMutableTreeNode) tree.getModel().getRoot(),
            n -> {
                Object u = n.getUserObject();
                if (selector == null) {
                    return u instanceof InlayProviderSettingsModel && langs.contains(((InlayProviderSettingsModel) u).getLanguage());
                }
                else if (u instanceof InlayProviderSettingsModel) {
                    InlayProviderSettingsModel m = (InlayProviderSettingsModel) u;
                    return selector.test(m) && langs.contains(m.getLanguage());
                }
                return false;
            });
        if (node != null) {
            TreeUtil.selectNode(tree, node);
        }
    }

    private class InlaySettingsTreeRenderer extends CheckboxTree.CheckboxTreeCellRenderer {
        public InlaySettingsTreeRenderer() {
            super(true, true);
        }

        @Override
        public void customizeRenderer(JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
            if (!(value instanceof DefaultMutableTreeNode defaultMutableTreeNode)) {
                return;
            }
            String name = getNameImpl(defaultMutableTreeNode, (DefaultMutableTreeNode) ((DefaultMutableTreeNode) value).getParent());
            getTextRenderer().appendHTML(name, null);
        }
    }
}
