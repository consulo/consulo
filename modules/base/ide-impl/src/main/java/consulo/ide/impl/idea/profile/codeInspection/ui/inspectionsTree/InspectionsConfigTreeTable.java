/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.profile.codeInspection.ui.inspectionsTree;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.profile.codeInspection.ui.InspectionsAggregationUtil;
import consulo.ide.impl.idea.profile.codeInspection.ui.ToolDescriptors;
import consulo.ide.impl.idea.profile.codeInspection.ui.table.ScopesAndSeveritiesTable;
import consulo.ide.impl.idea.profile.codeInspection.ui.table.ThreeStateCheckBoxRenderer;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.inspection.scheme.InspectionProfileImpl;
import consulo.language.editor.impl.internal.inspection.scheme.ToolsImpl;
import consulo.language.editor.internal.inspection.ScopeToolState;
import consulo.language.editor.rawHighlight.HighlightDisplayKey;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.transferable.TextTransferable;
import consulo.ui.ex.awt.tree.table.TreeTable;
import consulo.ui.ex.awt.tree.table.TreeTableModel;
import consulo.ui.ex.awt.tree.table.TreeTableTree;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Comparing;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.*;

/**
 * @author Dmitry Batkovich
 */
public class InspectionsConfigTreeTable extends TreeTable {
    private final static Logger LOG = Logger.getInstance(InspectionsConfigTreeTable.class);

    private final static int TREE_COLUMN = 0;
    private final static int SEVERITIES_COLUMN = 1;
    private final static int IS_ENABLED_COLUMN = 2;

    public static int getAdditionalPadding() {
        return Platform.current().os().isMac() ? 10 : 0;
    }

    public static InspectionsConfigTreeTable create(InspectionsConfigTreeTableSettings settings, Disposable parentDisposable) {
        return new InspectionsConfigTreeTable(new InspectionsConfigTreeTableModel(settings, parentDisposable));
    }

    public InspectionsConfigTreeTable(final InspectionsConfigTreeTableModel model) {
        super(model, false);

        TableColumn severitiesColumn = getColumnModel().getColumn(SEVERITIES_COLUMN);
        severitiesColumn.setMaxWidth(JBUI.scale(20));

        TableColumn isEnabledColumn = getColumnModel().getColumn(IS_ENABLED_COLUMN);
        isEnabledColumn.setMaxWidth(JBUI.scale(20 + getAdditionalPadding()));
        isEnabledColumn.setCellRenderer(new ThreeStateCheckBoxRenderer());
        isEnabledColumn.setCellEditor(new ThreeStateCheckBoxRenderer());

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent event) {
                TreePath path = getTree().getPathForRow(getTree().getLeadSelectionRow());
                if (path != null) {
                    InspectionConfigTreeNode node = (InspectionConfigTreeNode) path.getLastPathComponent();
                    if (node.isLeaf()) {
                        model.swapInspectionEnableState();
                    }
                }
                return true;
            }
        }.installOn(this);

        setTransferHandler(new TransferHandler() {
            @Nullable
            @Override
            protected Transferable createTransferable(JComponent c) {
                TreePath path = getTree().getPathForRow(getTree().getLeadSelectionRow());
                if (path != null) {
                    return new TextTransferable(StringUtil.join(
                        ContainerUtil.mapNotNull(path.getPath(), o -> o == path.getPath()[0] ? null : o.toString()),
                        " | "
                    ));
                }
                return null;
            }

            @Override
            public int getSourceActions(JComponent c) {
                return COPY;
            }
        });

        registerKeyboardAction(
            e -> {
                model.swapInspectionEnableState();
                updateUI();
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
            JComponent.WHEN_FOCUSED
        );

        getEmptyText().setText("No enabled inspections available");
    }

    public abstract static class InspectionsConfigTreeTableSettings {
        private final TreeNode myRoot;
        private final Project myProject;

        public InspectionsConfigTreeTableSettings(TreeNode root, Project project) {
            myRoot = root;
            myProject = project;
        }

        public TreeNode getRoot() {
            return myRoot;
        }

        public Project getProject() {
            return myProject;
        }

        protected abstract InspectionProfileImpl getInspectionProfile();

        protected abstract void onChanged(InspectionConfigTreeNode node);

        public abstract void updateRightPanel();
    }

    private static class InspectionsConfigTreeTableModel extends DefaultTreeModel implements TreeTableModel {
        private final InspectionsConfigTreeTableSettings mySettings;
        private final Runnable myUpdateRunnable;
        private TreeTable myTreeTable;

        private Alarm myUpdateAlarm;

        public InspectionsConfigTreeTableModel(InspectionsConfigTreeTableSettings settings, Disposable parentDisposable) {
            super(settings.getRoot());
            mySettings = settings;
            myUpdateRunnable = () -> {
                settings.updateRightPanel();
                ((AbstractTableModel) myTreeTable.getModel()).fireTableDataChanged();
            };
            myUpdateAlarm = new Alarm(Alarm.ThreadToUse.SWING_THREAD, parentDisposable);
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Nullable
        @Override
        public String getColumnName(int column) {
            return null;
        }

        @Override
        public Class getColumnClass(int column) {
            return switch (column) {
                case TREE_COLUMN -> TreeTableModel.class;
                case SEVERITIES_COLUMN -> Icon.class;
                case IS_ENABLED_COLUMN -> Boolean.class;
                default -> throw new IllegalArgumentException();
            };
        }

        @Nullable
        @Override
        public Object getValueAt(Object node, int column) {
            if (column == TREE_COLUMN) {
                return null;
            }
            InspectionConfigTreeNode treeNode = (InspectionConfigTreeNode) node;
            List<HighlightDisplayKey> inspectionsKeys = InspectionsAggregationUtil.getInspectionsKeys(treeNode);
            if (column == SEVERITIES_COLUMN) {
                if (treeNode.getGroupName() != null) {
                    return null;
                }

                MultiColoredHighlightSeverityIconSink sink = new MultiColoredHighlightSeverityIconSink();
                for (HighlightDisplayKey selectedInspectionsNode : inspectionsKeys) {
                    String toolId = selectedInspectionsNode.toString();
                    if (mySettings.getInspectionProfile().getTools(toolId, mySettings.getProject()).isEnabled()) {
                        sink.put(
                            mySettings.getInspectionProfile().getToolDefaultState(toolId, mySettings.getProject()),
                            mySettings.getInspectionProfile().getNonDefaultTools(toolId, mySettings.getProject())
                        );
                    }
                }
                return TargetAWT.to(sink.constructIcon(mySettings.getInspectionProfile()));
            }
            else if (column == IS_ENABLED_COLUMN) {
                return isEnabled(inspectionsKeys);
            }
            throw new IllegalArgumentException();
        }

        @Nullable
        private Boolean isEnabled(List<HighlightDisplayKey> selectedInspectionsNodes) {
            Boolean isPreviousEnabled = null;
            for (HighlightDisplayKey key : selectedInspectionsNodes) {
                ToolsImpl tools = mySettings.getInspectionProfile().getTools(key.toString(), mySettings.getProject());
                for (ScopeToolState state : tools.getTools()) {
                    boolean enabled = state.isEnabled();
                    if (isPreviousEnabled == null) {
                        isPreviousEnabled = enabled;
                    }
                    else if (!isPreviousEnabled.equals(enabled)) {
                        return null;
                    }
                }
            }
            return isPreviousEnabled;
        }

        @Override
        public boolean isCellEditable(Object node, int column) {
            return column == IS_ENABLED_COLUMN;
        }

        @Override
        public void setValueAt(Object aValue, Object node, int column) {
            LOG.assertTrue(column == IS_ENABLED_COLUMN);
            if (aValue == null) {
                return;
            }
            boolean doEnable = (Boolean) aValue;
            InspectionProfileImpl profile = mySettings.getInspectionProfile();
            for (InspectionConfigTreeNode aNode : InspectionsAggregationUtil.getInspectionsNodes((InspectionConfigTreeNode) node)) {
                setToolEnabled(doEnable, profile, aNode.getKey());
                aNode.dropCache();
                mySettings.onChanged(aNode);
            }
            updateRightPanel();
        }

        public void swapInspectionEnableState() {
            LOG.assertTrue(myTreeTable != null);

            Boolean state = null;
            Set<HighlightDisplayKey> tools = new HashSet<>();
            List<InspectionConfigTreeNode> nodes = new ArrayList<>();

            for (TreePath selectionPath : myTreeTable.getTree().getSelectionPaths()) {
                InspectionConfigTreeNode node = (InspectionConfigTreeNode) selectionPath.getLastPathComponent();
                collectInspectionFromNodes(node, tools, nodes);
            }

            int[] selectedRows = myTreeTable.getSelectedRows();
            for (int selectedRow : selectedRows) {
                Boolean value = (Boolean) myTreeTable.getValueAt(selectedRow, IS_ENABLED_COLUMN);
                if (state == null) {
                    state = value;
                }
                else if (!state.equals(value)) {
                    state = null;
                    break;
                }
            }
            boolean newState = !Boolean.TRUE.equals(state);

            InspectionProfileImpl profile = mySettings.getInspectionProfile();
            for (HighlightDisplayKey tool : tools) {
                setToolEnabled(newState, profile, tool);
            }

            for (InspectionConfigTreeNode node : nodes) {
                node.dropCache();
                mySettings.onChanged(node);
            }

            updateRightPanel();
        }

        private void updateRightPanel() {
            if (myTreeTable != null) {
                if (!myUpdateAlarm.isDisposed()) {
                    myUpdateAlarm.cancelAllRequests();
                    myUpdateAlarm.addRequest(myUpdateRunnable, 10, IdeaModalityState.stateForComponent(myTreeTable));
                }
            }
        }

        private void setToolEnabled(boolean newState, InspectionProfileImpl profile, HighlightDisplayKey tool) {
            String toolId = tool.toString();
            if (newState) {
                profile.enableTool(toolId, mySettings.getProject());
            }
            else {
                profile.disableTool(toolId, mySettings.getProject());
            }
            for (ScopeToolState scopeToolState : profile.getTools(toolId, mySettings.getProject()).getTools()) {
                scopeToolState.setEnabled(newState);
            }
        }

        private static void collectInspectionFromNodes(
            InspectionConfigTreeNode node,
            Set<HighlightDisplayKey> tools,
            List<InspectionConfigTreeNode> nodes
        ) {
            if (node == null) {
                return;
            }
            nodes.add(node);

            ToolDescriptors descriptors = node.getDescriptors();
            if (descriptors == null) {
                for (int i = 0; i < node.getChildCount(); i++) {
                    collectInspectionFromNodes((InspectionConfigTreeNode) node.getChildAt(i), tools, nodes);
                }
            }
            else {
                HighlightDisplayKey key = descriptors.getDefaultDescriptor().getKey();
                tools.add(key);
            }
        }

        @Override
        public void setTree(JTree tree) {
            myTreeTable = ((TreeTableTree) tree).getTreeTable();
        }
    }

    private static class SeverityAndOccurrences {
        private HighlightSeverity myPrimarySeverity;
        private final Map<String, HighlightSeverity> myOccurrences = new HashMap<>();

        public void setSeverityToMixed() {
            myPrimarySeverity = ScopesAndSeveritiesTable.MIXED_FAKE_SEVERITY;
        }

        public SeverityAndOccurrences incOccurrences(String toolName, HighlightSeverity severity) {
            if (myPrimarySeverity == null) {
                myPrimarySeverity = severity;
            }
            else if (!Comparing.equal(severity, myPrimarySeverity)) {
                myPrimarySeverity = ScopesAndSeveritiesTable.MIXED_FAKE_SEVERITY;
            }
            myOccurrences.put(toolName, severity);
            return this;
        }

        public HighlightSeverity getPrimarySeverity() {
            return myPrimarySeverity;
        }

        public int getOccurrencesSize() {
            return myOccurrences.size();
        }

        public Map<String, HighlightSeverity> getOccurrences() {
            return myOccurrences;
        }
    }

    private static class MultiColoredHighlightSeverityIconSink {
        private final Map<String, SeverityAndOccurrences> myScopeToAverageSeverityMap = new HashMap<>();

        private String myDefaultScopeName;

        public Image constructIcon(InspectionProfileImpl inspectionProfile) {
            Map<String, HighlightSeverity> computedSeverities = computeSeverities(inspectionProfile);

            if (computedSeverities == null) {
                return null;
            }

            boolean allScopesHasMixedSeverity = true;
            for (HighlightSeverity severity : computedSeverities.values()) {
                if (!severity.equals(ScopesAndSeveritiesTable.MIXED_FAKE_SEVERITY)) {
                    allScopesHasMixedSeverity = false;
                    break;
                }
            }
            return allScopesHasMixedSeverity
                ? ScopesAndSeveritiesTable.MIXED_FAKE_LEVEL.getIcon()
                : MultiScopeSeverityIcon.create(computedSeverities, myDefaultScopeName, inspectionProfile);
        }

        @Nullable
        private Map<String, HighlightSeverity> computeSeverities(InspectionProfileImpl inspectionProfile) {
            if (myScopeToAverageSeverityMap.isEmpty()) {
                return null;
            }
            Map<String, HighlightSeverity> result = new HashMap<>();
            Map.Entry<String, SeverityAndOccurrences> entry = ContainerUtil.getFirstItem(myScopeToAverageSeverityMap.entrySet());
            result.put(entry.getKey(), entry.getValue().getPrimarySeverity());
            if (myScopeToAverageSeverityMap.size() == 1) {
                return result;
            }

            SeverityAndOccurrences defaultSeveritiesAndOccurrences = myScopeToAverageSeverityMap.get(myDefaultScopeName);
            if (defaultSeveritiesAndOccurrences == null) {
                for (Map.Entry<String, SeverityAndOccurrences> e : myScopeToAverageSeverityMap.entrySet()) {
                    HighlightSeverity primarySeverity = e.getValue().getPrimarySeverity();
                    if (primarySeverity != null) {
                        result.put(e.getKey(), primarySeverity);
                    }
                }
                return result;
            }
            int allInspectionsCount = defaultSeveritiesAndOccurrences.getOccurrencesSize();
            Map<String, HighlightSeverity> allScopes = defaultSeveritiesAndOccurrences.getOccurrences();
            for (String currentScope : myScopeToAverageSeverityMap.keySet()) {
                SeverityAndOccurrences currentSeverityAndOccurrences = myScopeToAverageSeverityMap.get(currentScope);
                if (currentSeverityAndOccurrences == null) {
                    continue;
                }
                HighlightSeverity currentSeverity = currentSeverityAndOccurrences.getPrimarySeverity();
                if (currentSeverity == ScopesAndSeveritiesTable.MIXED_FAKE_SEVERITY ||
                    currentSeverityAndOccurrences.getOccurrencesSize() == allInspectionsCount ||
                    myDefaultScopeName.equals(currentScope)) {
                    result.put(currentScope, currentSeverity);
                }
                else {
                    Set<String> toolsToCheck = new HashSet<>(allScopes.keySet());
                    toolsToCheck.removeAll(currentSeverityAndOccurrences.getOccurrences().keySet());
                    boolean doContinue = false;
                    Map<String, HighlightSeverity> lowerScopeOccurrences =
                        myScopeToAverageSeverityMap.get(myDefaultScopeName).getOccurrences();
                    for (String toolName : toolsToCheck) {
                        HighlightSeverity currentToolSeverity = lowerScopeOccurrences.get(toolName);
                        if (currentToolSeverity != null) {
                            if (!currentSeverity.equals(currentToolSeverity)) {
                                result.put(currentScope, ScopesAndSeveritiesTable.MIXED_FAKE_SEVERITY);
                                doContinue = true;
                                break;
                            }
                        }
                    }
                    if (doContinue) {
                        continue;
                    }
                    result.put(currentScope, currentSeverity);
                }
            }

            return result;
        }

        public void put(@Nonnull ScopeToolState defaultState, @Nonnull List<ScopeToolState> nonDefault) {
            putOne(defaultState);
            if (myDefaultScopeName == null) {
                myDefaultScopeName = defaultState.getScopeId();
            }
            for (ScopeToolState scopeToolState : nonDefault) {
                putOne(scopeToolState);
            }
        }

        private void putOne(ScopeToolState state) {
            if (!state.isEnabled()) {
                return;
            }
            String scopeName = state.getScopeId();
            SeverityAndOccurrences severityAndOccurrences = myScopeToAverageSeverityMap.get(scopeName);
            String inspectionName = state.getTool().getShortName();
            if (severityAndOccurrences == null) {
                myScopeToAverageSeverityMap.put(
                    scopeName,
                    new SeverityAndOccurrences().incOccurrences(inspectionName, state.getLevel().getSeverity())
                );
            }
            else {
                severityAndOccurrences.incOccurrences(inspectionName, state.getLevel().getSeverity());
            }
        }
    }
}
