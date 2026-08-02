package consulo.ide.impl.wm.impl.status;

import consulo.application.internal.AbstractProgressIndicatorExBase;
import consulo.application.internal.ProgressIndicatorEx;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.TaskInfo;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.LabelStyle;
import consulo.ui.ProgressBar;
import consulo.ui.ProgressBarStyle;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.util.lang.Pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 16/08/2021
 */
public class UnifiedInfoAndProgressPanel implements Disposable {
    private final DockLayout myLayout;

    private final Label myStatusLabel;

    private final Map<ProgressIndicator, ProgressItem> myItems = new LinkedHashMap<>();

    private HorizontalLayout myProgressTarget;

    private record ProgressItem(TaskInfo info, ProgressBar bar, Label label) {
    }

    /**
     * The middle row of the bar, beside the status text. A layout of this panel's own ended up narrower than
     * its content and painted over its neighbours, and the row of widgets on the right has no room to spare -
     * a task added there ran past the edge of the window.
     */
    public void setProgressTarget(HorizontalLayout progressTarget) {
        myProgressTarget = progressTarget;
    }

    @RequiredUIAccess
    public UnifiedInfoAndProgressPanel() {
        myLayout = DockLayout.create();

        myStatusLabel = Label.create();

        myLayout.left(myStatusLabel);
    }

    @RequiredUIAccess
    public void setStatusText(String text) {
        myStatusLabel.setText(LocalizeValue.of(text));
    }

    /**
     * A task which runs in the background belongs here rather than in a window of its own - the same split the
     * awt frame makes between its progress panel and a modal progress.
     */
    @RequiredUIAccess
    public void addProgress(ProgressIndicator indicator, TaskInfo info) {
        UIAccess uiAccess = UIAccess.current();

        Label label = Label.create(LocalizeValue.of(info.getTitle()));
        label.addStyle(LabelStyle.TRANSPARENT_BACKGROUND);

        // the spinner of the awt status bar - a running task is shown by the fact that it runs, and the share it
        // is through belongs to the process popup rather than to a bar of this size
        ProgressBar bar = ProgressBar.create();
        bar.addStyle(ProgressBarStyle.SPINNER);
        bar.addStyle(ProgressBarStyle.TRANSPARENT_BACKGROUND);
        bar.setIndeterminate(true);

        // straight into the row of widgets - a layout of its own only added a level which stretches
        myItems.put(indicator, new ProgressItem(info, bar, label));

        if (myProgressTarget != null) {
            myProgressTarget.add(bar);
            myProgressTarget.add(label);
        }

        if (indicator instanceof ProgressIndicatorEx indicatorEx) {
            indicatorEx.addStateDelegate(new AbstractProgressIndicatorExBase() {
                @Override
                public void finish(TaskInfo task) {
                    super.finish(task);
                    uiAccess.give(() -> removeProgress(indicator));
                }

                @Override
                public void stop() {
                    super.stop();
                    uiAccess.give(() -> removeProgress(indicator));
                }

            });
        }
    }


    @RequiredUIAccess
    private void removeProgress(ProgressIndicator indicator) {
        ProgressItem item = myItems.remove(indicator);
        if (item != null) {
            if (myProgressTarget != null) {
                myProgressTarget.remove(item.bar());
                myProgressTarget.remove(item.label());
            }
        }
    }

    public List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses() {
        List<Pair<TaskInfo, ProgressIndicator>> result = new ArrayList<>();
        for (Map.Entry<ProgressIndicator, ProgressItem> entry : myItems.entrySet()) {
            result.add(Pair.create(entry.getValue().info(), entry.getKey()));
        }
        return result;
    }

    public Component getUIComponent() {
        return myLayout;
    }

    @Override
    public void dispose() {
    }
}
