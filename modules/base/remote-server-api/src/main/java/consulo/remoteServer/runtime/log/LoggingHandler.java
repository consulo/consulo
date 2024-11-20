package consulo.remoteServer.runtime.log;

import consulo.execution.ui.console.HyperlinkInfo;
import consulo.process.ProcessHandler;
import org.jetbrains.annotations.NotNull;

public interface LoggingHandler {
    void print(@NotNull String s);

    void printHyperlink(@NotNull String url);

    void printHyperlink(@NotNull String text, HyperlinkInfo info);

    void attachToProcess(@NotNull ProcessHandler handler);

    void clear();

    void scrollTo(int offset);
}
