// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.codeInsight.codeVision;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.codeVision.CodeVisionAnchorKind;
import consulo.language.editor.codeVision.CodeVisionSettings;
import consulo.language.editor.codeVision.DaemonBoundCodeVisionProvider;
import consulo.language.editor.impl.internal.inlay.setting.ImmediateConfigurable;
import consulo.language.editor.impl.internal.inlay.setting.InlayProviderSettingsModel;
import consulo.language.editor.impl.internal.inlay.setting.InlaySettingsProvider;
import consulo.language.editor.inlay.InlayGroup;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import net.miginfocom.swing.MigLayout;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Plugs each code-vision <em>group</em> into the Inlay Hints settings panel as a single
 * {@link InlayProviderSettingsModel} under {@link InlayGroup#CODE_VISION_GROUP_NEW}.
 * <p>
 * Mirrors JB's {@code CodeVisionInlaySettingProvider.kt}: providers are grouped by
 * {@link DaemonBoundCodeVisionProvider#getGroupId()} and each group gets one settings row
 * with a per-group <em>Position</em> combo-box (Default / Top / Right) — matching
 * JB's {@code CodeVisionGroupDefaultSettingModel}.
 */
@ExtensionImpl
public class CodeVisionInlaySettingsProvider implements InlaySettingsProvider {

    @Override
    public Collection<Language> getSupportedLanguages(Project project) {
        return Collections.singletonList(Language.ANY);
    }

    @Override
    public List<InlayProviderSettingsModel> createModels(Project project, Language language) {
        if (language != Language.ANY) return Collections.emptyList();

        // Group providers by groupId, preserving encounter order (one entry per group)
        Map<String, DaemonBoundCodeVisionProvider> firstByGroup = new LinkedHashMap<>();
        for (DaemonBoundCodeVisionProvider provider : DaemonBoundCodeVisionProvider.EP_NAME.getExtensionList()) {
            firstByGroup.putIfAbsent(provider.getGroupId(), provider);
        }

        List<InlayProviderSettingsModel> result = new ArrayList<>();
        for (Map.Entry<String, DaemonBoundCodeVisionProvider> entry : firstByGroup.entrySet()) {
            result.add(new GroupModel(entry.getKey(), entry.getValue().getName()));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Per-group settings model (one per groupId)
    // -------------------------------------------------------------------------

    /**
     * One model per code-vision {@code groupId}, matching JB's
     * {@code CodeVisionGroupDefaultSettingModel}: checkbox + per-group position combo-box.
     */
    private static final class GroupModel extends InlayProviderSettingsModel {

        /** Positions offered in the per-group combo-box — mirrors JB's {@code supportedAnchors}. */
        private static final CodeVisionAnchorKind[] ANCHORS =
            {CodeVisionAnchorKind.Default, CodeVisionAnchorKind.Top, CodeVisionAnchorKind.Right};

        private final LocalizeValue myName;
        // Lazily created UI
        private @Nullable JComponent myComponent;
        private @Nullable JComboBox<CodeVisionAnchorKind> myPositionComboBox;

        GroupModel(String groupId, LocalizeValue name) {
            super(CodeVisionSettings.getInstance().isProviderEnabled(groupId),
                groupId,
                Language.ANY);
            myName = name;
        }

        @Override
        public InlayGroup getGroup() {
            return InlayGroup.CODE_VISION_GROUP_NEW;
        }

        @Override
        public LocalizeValue getName() {
            return myName;
        }

        @Override
        public @Nullable JComponent getComponent() {
            if (myComponent == null) {
                myComponent = buildComponent();
                reset();
            }
            return myComponent;
        }

        // -------------------------------------------------------------------------
        // apply / isModified / reset — mirrors JB CodeVisionGroupDefaultSettingModel
        // -------------------------------------------------------------------------

        @Override
        public void apply() {
            CodeVisionSettings settings = CodeVisionSettings.getInstance();
            settings.setProviderEnabled(getId(), isEnabled());
            if (myPositionComboBox != null) {
                CodeVisionAnchorKind chosen = (CodeVisionAnchorKind) myPositionComboBox.getSelectedItem();
                if (chosen != null) {
                    settings.setPositionForGroup(getId(), chosen);
                }
            }
        }

        @Override
        public boolean isModified() {
            CodeVisionSettings settings = CodeVisionSettings.getInstance();
            if (isEnabled() != settings.isProviderEnabled(getId())) return true;
            if (myPositionComboBox == null) return false;
            CodeVisionAnchorKind saved = settings.getPositionForGroup(getId());
            if (saved == null) saved = CodeVisionAnchorKind.Default;
            return saved != myPositionComboBox.getSelectedItem();
        }

        @Override
        public void reset() {
            CodeVisionSettings settings = CodeVisionSettings.getInstance();
            setEnabled(settings.isProviderEnabled(getId()));
            if (myPositionComboBox != null) {
                CodeVisionAnchorKind pos = settings.getPositionForGroup(getId());
                myPositionComboBox.setSelectedItem(pos != null ? pos : CodeVisionAnchorKind.Default);
            }
        }

        // -------------------------------------------------------------------------
        // Unused abstract stubs — no preview text for code-vision groups
        // -------------------------------------------------------------------------

        @Override public @Nullable String getDescription()                                       { return null; }
        @Override public @Nullable String getPreviewText()                                       { return null; }
        @Override public @Nullable String getCasePreview(ImmediateConfigurable.Case caze)        { return null; }
        @Override public @Nullable Language getCasePreviewLanguage(ImmediateConfigurable.Case c) { return null; }
        @Override public @Nullable String getCaseDescription(ImmediateConfigurable.Case c)      { return null; }
        @Override public List<ImmediateConfigurable.Case> getCases()                             { return Collections.emptyList(); }

        // -------------------------------------------------------------------------
        // UI construction
        // -------------------------------------------------------------------------

        private JComponent buildComponent() {
            myPositionComboBox = new JComboBox<>(ANCHORS);
            myPositionComboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value,
                                                               int index, boolean isSelected,
                                                               boolean cellHasFocus) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    if (value instanceof CodeVisionAnchorKind kind) {
                        setText(kind.name());
                    }
                    return this;
                }
            });

            JPanel panel = new JPanel(new MigLayout("insets 0, fillx", "[left][grow, fill]", "[]"));
            panel.add(new JLabel("Position:"));
            panel.add(myPositionComboBox);
            return panel;
        }
    }
}
