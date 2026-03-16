package consulo.remoteServer.runtime.log;

import consulo.execution.ui.console.HyperlinkInfo;
import consulo.process.ProcessHandler;

public interface LoggingHandler {
    void print(String s);

    void printHyperlink(String url);

    void printHyperlink(String text, HyperlinkInfo info);

    void attachToProcess(ProcessHandler handler);

    void clear();

    void scrollTo(int offset);
}
