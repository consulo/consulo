package consulo.execution.impl.internal.ui;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.execution.BeforeRunFragment;
import consulo.execution.BeforeRunTask;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.layout.VerticalLayout;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Edits one before-run task in a dialog hosting its fragment's editor; the task is changed only on OK.
 */
final class BeforeRunTaskDialog {
    private BeforeRunTaskDialog() {
    }

    /**
     * Completes with {@code true} when the task was edited and confirmed, {@code false} when the task has no settings,
     * and exceptionally when the dialog was cancelled.
     */
    @RequiredUIAccess
    static <T extends BeforeRunTask> CompletableFuture<Boolean> configure(BeforeRunFragment<T> fragment, RunConfiguration configuration, T task) {
        @SuppressWarnings("unchecked")
        RunConfigurationFragmentEditor<T>[] editorHolder = new RunConfigurationFragmentEditor[1];

        DialogDescriptor descriptor = new DialogDescriptor(fragment.getName()) {
            @Override
            public WidthAndHeight getInitialSize() {
                return WidthAndHeight.ofFont(40, 25);
            }

            @RequiredUIAccess
            @Override
            public Component createCenterComponent(Disposable uiDisposable) {
                VerticalLayout layout = VerticalLayout.create(Space.MEDIUM);
                RunConfigurationFragmentEditor<T> editor = fragment.createEditor(configuration, uiDisposable);
                editorHolder[0] = editor;
                if (editor != null) {
                    RunConfigurationFragmentTable table = new RunConfigurationFragmentTable();
                    editor.addRows(table);
                    table.addTo(layout, List.of());
                    editor.reset(task);
                }
                return layout;
            }
        };

        if (!hasEditor(fragment, configuration)) {
            return CompletableFuture.completedFuture(false);
        }

        UIAccess uiAccess = UIAccess.current();
        return Application.get().getInstance(DialogService.class).build(descriptor).showAsync().thenApplyAsync(value -> {
            RunConfigurationFragmentEditor<T> editor = editorHolder[0];
            if (editor != null) {
                try {
                    editor.apply(task);
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
            return true;
        }, uiAccess);
    }

    static boolean hasEditor(@Nullable BeforeRunFragment<?> fragment, RunConfiguration configuration) {
        return fragment != null && fragment.isConfigurable();
    }
}
