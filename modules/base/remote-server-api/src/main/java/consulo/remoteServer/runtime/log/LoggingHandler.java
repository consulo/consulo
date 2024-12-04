package consulo.remoteServer.runtime.log;

import consulo.execution.ui.console.HyperlinkInfo;
import consulo.process.ProcessHandler;
import jakarta.annotation.Nonnull;

public interface LoggingHandler {
    void print(@Nonnull String s);

    void printHyperlink(@Nonnull String url);

    void printHyperlink(@Nonnull String text, HyperlinkInfo info);

    void attachToProcess(@Nonnull ProcessHandler handler);

    void clear();

    void scrollTo(int offset);
}
