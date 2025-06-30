/*
 * Copyright 2013-2025 consulo.io
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
package consulo.ide.impl.execution;

import com.intellij.rt.coverage.data.LineCoverage;
import com.intellij.rt.coverage.data.LineData;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.colorScheme.TextAttributesKey;
import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.document.Document;
import consulo.execution.coverage.internal.ExecutionCoverageInternal;
import consulo.execution.coverage.localize.ExecutionCoverageLocalize;
import consulo.ide.impl.idea.application.options.colors.ColorAndFontOptions;
import consulo.ide.impl.idea.application.options.colors.ColorAndFontPanelFactory;
import consulo.ide.impl.idea.application.options.colors.NewColorAndFontPanel;
import consulo.ide.impl.idea.application.options.colors.SimpleEditorPreview;
import consulo.ide.impl.idea.codeInsight.hint.EditorFragmentComponent;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.codeInspection.export.ExportToHTMLDialog;
import consulo.ide.impl.idea.openapi.options.colors.pages.GeneralColorsPage;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.hint.HintHint;
import jakarta.annotation.Nonnull;
import jakarta.inject.Singleton;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2025-06-29
 */
@ServiceImpl
@Singleton
public class ExecutionCoverageInternalmpl implements ExecutionCoverageInternal {
    @RequiredUIAccess
    @Nonnull
    @Override
    public CompletableFuture<?> showExportDialog(@Nonnull Project project, @Nonnull String presentableName) {
        ExportToHTMLDialog dialog = new ExportToHTMLDialog(project, true);
        dialog.setTitle(ExecutionCoverageLocalize.generateCoverageReportFor(presentableName));
        dialog.reset();
        dialog.show();
        if (!dialog.isOK()) {
            return CompletableFuture.failedFuture(new CancellationException());
        }
        dialog.apply();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    @RequiredUIAccess
    public void showCoverageHit(JPanel panel,
                                Editor editor,
                                Point point,
                                LineData lineData,
                                String reportText) {
        final Editor uEditor;
        if (reportText != null) {
            EditorFactory factory = EditorFactory.getInstance();
            Document doc = factory.createDocument(reportText);
            doc.setReadOnly(true);
            uEditor = factory.createEditor(doc, editor.getProject());
            panel.add(
                EditorFragmentComponent.createEditorFragmentComponent(uEditor, 0, doc.getLineCount(), false, false),
                BorderLayout.CENTER
            );
        }
        else {
            uEditor = null;
        }

        LightweightHintImpl hint = new LightweightHintImpl(panel) {
            @Override
            public void hide() {
                if (uEditor != null) {
                    EditorFactory.getInstance().releaseEditor(uEditor);
                }
                super.hide();
            }
        };
        HintManagerImpl.getInstanceImpl().showEditorHint(
            hint,
            editor,
            point,
            HintManagerImpl.HIDE_BY_ANY_KEY | HintManagerImpl.HIDE_BY_TEXT_CHANGE
                | HintManagerImpl.HIDE_BY_OTHER_HINT | HintManagerImpl.HIDE_BY_SCROLLING,
            -1,
            false,
            new HintHint(editor.getContentComponent(), point)
        );
    }

    @Override
    @RequiredUIAccess
    public void showColorsSettings(@Nonnull Project project,
                                   @Nonnull LineData lineData,
                                   Function<LineData, TextAttributesKey> attributesKeyFunc) {
        ColorAndFontOptions colorAndFontOptions = new ColorAndFontOptions() {
            @Override
            protected List<ColorAndFontPanelFactory> createPanelFactories() {
                final GeneralColorsPage colorsPage = new GeneralColorsPage(project.getApplication());
                ColorAndFontPanelFactory panelFactory = new ColorAndFontPanelFactory() {
                    @Nonnull
                    @Override
                    @RequiredUIAccess
                    public NewColorAndFontPanel createPanel(@Nonnull ColorAndFontOptions options) {
                        SimpleEditorPreview preview = new SimpleEditorPreview(options, colorsPage);
                        return NewColorAndFontPanel.create(preview, colorsPage.getDisplayName(), options, null, colorsPage);
                    }

                    @Nonnull
                    @Override
                    public String getPanelDisplayName() {
                        return ExecutionCoverageLocalize.configurableNameEditorColorsPage(
                            getDisplayName(),
                            colorsPage.getDisplayName()
                        ).get();
                    }
                };
                return Collections.singletonList(panelFactory);
            }
        };
        Configurable[] configurables = colorAndFontOptions.buildConfigurables();
        try {
            SearchableConfigurable general = colorAndFontOptions.findSubConfigurable(GeneralColorsPage.class);
            if (general != null) {
                ShowSettingsUtil.getInstance().editConfigurable(
                    project,
                    general,
                    general.enableSearch(attributesKeyFunc.apply(lineData).getExternalName())
                );
            }
        }
        finally {
            for (Configurable configurable : configurables) {
                configurable.disposeUIResources();
            }
            colorAndFontOptions.disposeUIResources();
        }
    }
}
