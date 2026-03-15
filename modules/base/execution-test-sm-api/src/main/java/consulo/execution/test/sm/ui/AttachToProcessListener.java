package consulo.execution.test.sm.ui;

import consulo.process.ProcessHandler;

/**
 * @author Sergey Simonchik
 */
public interface AttachToProcessListener {
    void onAttachToProcess(ProcessHandler processHandler);
}
