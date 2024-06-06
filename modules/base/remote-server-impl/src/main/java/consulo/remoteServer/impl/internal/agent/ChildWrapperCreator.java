package consulo.remoteServer.impl.internal.agent;

import java.lang.reflect.InvocationHandler;

/**
 * @author michael.golubev
 */
public interface ChildWrapperCreator {

  InvocationHandler createWrapperInvocationHandler(Object child);
}
