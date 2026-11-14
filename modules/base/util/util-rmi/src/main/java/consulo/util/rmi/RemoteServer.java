/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.util.rmi;

import org.jspecify.annotations.Nullable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RemoteServer {
  private static final Pattern REF_ENDPOINT_PATTERN = Pattern.compile("endpoint:\\[.*:(\\d+)]");

  private static Remote ourRemote;

  @SuppressWarnings("UseOfSystemOutOrSystemErr")
  protected static void start(Remote remote) throws Exception {
    setupRMI();
    banJNDI();

    if (ourRemote != null) throw new AssertionError("Already started");
    ourRemote = remote;

    Registry registry;
    int port;
    for (Random random = new Random(); ;) {
      port = random.nextInt(0xffff);
      if (port < 4000) continue;
      try {
        registry = LocateRegistry.createRegistry(port);
        break;
      }
      catch (ExportException ignored) { }
    }

    try {
      Remote stub = UnicastRemoteObject.exportObject(ourRemote, 0);
      String name = remote.getClass().getSimpleName() + Integer.toHexString(stub.hashCode());
      registry.bind(name, stub);

      String id = port + "/" + name;
      // the first used port is reused for all further exported objects - a client on another machine forwards both
      System.out.println("Port/ServicesPort/ID: " + port + "/" + getEndpointPort(stub) + "/" + name);

      long waitTime = 2 * 60 * 1000L;
      Object lock = new Object();
      //noinspection InfiniteLoopStatement
      while (true) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (lock) {
          lock.wait(waitTime);
        }
        RemoteDeadHand deadHand = (RemoteDeadHand)registry.lookup(RemoteDeadHand.BINDING_NAME);
        waitTime = deadHand.ping(id);
      }
    }
    catch (Throwable e) {
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }

  public static void setupRMI() {
    // this properties are necessary for RMI servers to work in some cases:
    // if we are behind a firewall, if the network connection is lost, etc.

    // do not use domain or http address for server; a loopback address, not a name - a forwarded port listens on it
    if (System.getProperty("java.rmi.server.hostname") == null) {
      System.setProperty("java.rmi.server.hostname", "127.0.0.1");
    }
    // do not use HTTP tunnelling
    System.setProperty("java.rmi.server.disableHttp", "true");
  }

  /**
   * The port the stub calls - the port of the objects exported by its machine, {@code 0} when it is not known.
   */
  public static int getEndpointPort(Remote stub) {
    Matcher matcher = REF_ENDPOINT_PATTERN.matcher(stub.toString());
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
  }

  private static void banJNDI() {
    if (System.getProperty(Context.INITIAL_CONTEXT_FACTORY) == null) {
      System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "consulo.util.rmi.RemoteServer$Jndi");
    }
  }

  @SuppressWarnings("UnusedDeclaration")
  public static class Jndi implements InitialContextFactory, InvocationHandler {
    @Override
    public Context getInitialContext(Hashtable<?, ?> environment) throws NamingException {
      return (Context)Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Context.class}, this);
    }

    @Override
    public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      return null;
    }
  }
}
