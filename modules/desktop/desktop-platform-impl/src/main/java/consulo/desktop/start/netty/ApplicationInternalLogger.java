package consulo.desktop.start.netty;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import io.netty.util.internal.logging.AbstractInternalLogger;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.Slf4JLoggerFactory;

import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2019-10-09
 */
public class ApplicationInternalLogger extends AbstractInternalLogger {
  private static final Slf4JLoggerFactory SLF4J_INSTANCE = (Slf4JLoggerFactory)Slf4JLoggerFactory.INSTANCE;

  private InternalLogger myDelegateLogger;

  public ApplicationInternalLogger(String name) {
    super(name);
  }

  private void executeVoid(@Nonnull Consumer<InternalLogger> consumer) {
    if (myDelegateLogger != null) {
      consumer.accept(myDelegateLogger);
      return;
    }

    Application application = ApplicationManager.getApplication();
    if (application != null) {
      myDelegateLogger = SLF4J_INSTANCE.newInstance(name());
      consumer.accept(myDelegateLogger);
    }
  }

  private boolean executeBool(@Nonnull Function<InternalLogger, Boolean> f) {
    if (myDelegateLogger != null) {
      return f.apply(myDelegateLogger);
    }

    Application application = ApplicationManager.getApplication();
    if (application != null) {
      myDelegateLogger = SLF4J_INSTANCE.newInstance(name());
      return f.apply(myDelegateLogger);
    }

    return false;
  }

  @Override
  public boolean isTraceEnabled() {
    return executeBool(InternalLogger::isTraceEnabled);
  }

  @Override
  public void trace(String msg) {
    executeVoid(it -> it.trace(msg));
  }

  @Override
  public void trace(String format, Object arg) {
    executeVoid(it -> it.trace(format, arg));
  }

  @Override
  public void trace(String format, Object argA, Object argB) {
    executeVoid(it -> it.trace(format, argA, argB));
  }

  @Override
  public void trace(String format, Object... arguments) {
    executeVoid(it -> it.trace(format, arguments));
  }

  @Override
  public void trace(String msg, Throwable t) {
    executeVoid(it -> it.trace(msg, t));
  }

  @Override
  public boolean isDebugEnabled() {
    return executeBool(InternalLogger::isDebugEnabled);
  }

  @Override
  public void debug(String msg) {
    executeVoid(it -> it.debug(msg));
  }

  @Override
  public void debug(String format, Object arg) {
    executeVoid(it -> it.debug(format, arg));
  }

  @Override
  public void debug(String format, Object argA, Object argB) {
    executeVoid(it -> it.debug(format, argA, argB));
  }

  @Override
  public void debug(String format, Object... arguments) {
    executeVoid(it -> it.debug(format, arguments));
  }

  @Override
  public void debug(String msg, Throwable t) {
    executeVoid(it -> it.debug(msg, t));
  }

  @Override
  public boolean isInfoEnabled() {
    return executeBool(InternalLogger::isInfoEnabled);
  }

  @Override
  public void info(String msg) {
    executeVoid(it -> it.info(msg));
  }

  @Override
  public void info(String format, Object arg) {
    executeVoid(it -> it.info(format, arg));
  }

  @Override
  public void info(String format, Object argA, Object argB) {
    executeVoid(it -> it.info(format, argA, argB));
  }

  @Override
  public void info(String format, Object... arguments) {
    executeVoid(it -> it.info(format, arguments));
  }

  @Override
  public void info(String msg, Throwable t) {
    executeVoid(it -> it.info(msg, t));
  }

  @Override
  public boolean isWarnEnabled() {
    return executeBool(InternalLogger::isWarnEnabled);
  }

  @Override
  public void warn(String msg) {
    executeVoid(it -> it.warn(msg));
  }

  @Override
  public void warn(String format, Object arg) {
    executeVoid(it -> it.warn(format, arg));
  }

  @Override
  public void warn(String format, Object argA, Object argB) {
    executeVoid(it -> it.warn(format, argA, argB));
  }

  @Override
  public void warn(String format, Object... arguments) {
    executeVoid(it -> it.warn(format, arguments));
  }

  @Override
  public void warn(String msg, Throwable t) {
    executeVoid(it -> it.warn(msg, t));
  }

  @Override
  public boolean isErrorEnabled() {
    return executeBool(InternalLogger::isErrorEnabled);
  }

  @Override
  public void error(String msg) {
    executeVoid(it -> it.error(msg));
  }

  @Override
  public void error(String format, Object arg) {
    executeVoid(it -> it.error(format, arg));
  }

  @Override
  public void error(String format, Object argA, Object argB) {
    executeVoid(it -> it.error(format, argA, argB));
  }

  @Override
  public void error(String format, Object... arguments) {
    executeVoid(it -> it.error(format, arguments));
  }

  @Override
  public void error(String msg, Throwable t) {
    executeVoid(it -> it.error(msg, t));
  }
}
