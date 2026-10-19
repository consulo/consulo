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
package consulo.sandboxPlugin.editor.benchmark;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.application.Application;
import consulo.application.WriteAction;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.internal.benchmark.EditorBenchmarkDriver;
import consulo.codeEditor.internal.benchmark.EditorBenchmarkView;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
@ActionImpl(id = "EditorBenchmarkAction", parents = @ActionParentRef(@ActionRef(id = "Internal.UI")))
public class EditorBenchmarkAction extends AnAction implements DumbAware, AnActionWithSyncUpdate {
    private static final int METHOD_COUNT = 500;
    private static final long SEED = 20261019L;
    private static final long START_DELAY_MS = 1500;

    private final Application myApplication;
    private final DialogService myDialogService;
    private final EditorFactory myEditorFactory;

    @Inject
    public EditorBenchmarkAction(Application application, DialogService dialogService, EditorFactory editorFactory) {
        super(LocalizeValue.localizeTODO("Editor Benchmark"));
        myApplication = application;
        myDialogService = dialogService;
        myEditorFactory = editorFactory;
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.getData(Project.KEY) != null);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        UIAccess uiAccess = UIAccess.current();

        myDialogService.build(new DialogDescriptor(LocalizeValue.localizeTODO("Editor Benchmark")) {
            @RequiredUIAccess
            @Override
            public Component createCenterComponent(Disposable uiDisposable) {
                EditorBenchmarkReport report = new EditorBenchmarkReport();

                long start = System.nanoTime();
                EditorBenchmarkSource source = new EditorBenchmarkSourceBuilder(SEED).build(METHOD_COUNT);
                report.setup("generate source", System.nanoTime() - start);

                start = System.nanoTime();
                Document document = myEditorFactory.createDocument(source.text());
                report.setup("create document", System.nanoTime() - start);

                start = System.nanoTime();
                Editor editor = myEditorFactory.createEditor(document, project, EditorKind.MAIN_EDITOR);
                Disposer.register(uiDisposable, () -> myEditorFactory.releaseEditor(editor));
                report.setup("create editor", System.nanoTime() - start);

                EditorBenchmarkView view = myApplication.getExtensionPoint(EditorBenchmarkDriver.class)
                    .computeSafeIfAny(driver -> driver.createView(editor));
                if (view == null || !(editor instanceof EditorEx editorEx)) {
                    return Label.create(LocalizeValue.localizeTODO("No benchmark driver for this editor"));
                }

                EditorBenchmarkRunner runner = new EditorBenchmarkRunner(project, editorEx, view, source, report);
                runner.decorate();

                Document reportDocument = myEditorFactory.createDocument("The benchmark starts once the editor is shown");
                Editor reportViewer = myEditorFactory.createViewer(reportDocument, project, EditorKind.UNTYPED);
                Disposer.register(uiDisposable, () -> myEditorFactory.releaseEditor(reportViewer));

                Runnable runAndShow = () -> {
                    if (editor.isDisposed()) {
                        return;
                    }
                    runner.run();
                    String text = report.toText();
                    WriteAction.run(() -> reportDocument.setText(text));
                };

                DockLayout reportLayout = DockLayout.create();
                reportLayout.top(HorizontalLayout.create().add(Button.create(LocalizeValue.localizeTODO("Run Again"), event -> runAndShow.run())));
                reportLayout.center(reportViewer.getUIComponent());

                TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.VERTICAL);
                split.setFirstComponent(editor.getUIComponent());
                split.setSecondComponent(reportLayout);
                split.setProportion(70);

                uiAccess.getScheduler().schedule(runAndShow, ModalityState.any(), START_DELAY_MS, TimeUnit.MILLISECONDS);
                return split;
            }

            @Override
            public boolean isModal() {
                return false;
            }

            @Override
            public boolean hasDefaultContentBorder() {
                return false;
            }

            @Override
            public @Nullable WidthAndHeight getInitialSize() {
                return WidthAndHeight.ofFont(150, 55);
            }
        }).showAsync();
    }
}
