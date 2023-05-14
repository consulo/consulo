package consulo.util.socketConnection.impl;

import consulo.util.collection.MultiValuesMap;
import consulo.util.collection.SmartList;
import consulo.util.collection.primitive.ints.IntMaps;
import consulo.util.collection.primitive.ints.IntObjectMap;
import consulo.util.lang.ref.Ref;
import consulo.util.socketConnection.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author nik
 */
public class ResponseProcessor<R extends AbstractResponse> {
  private static final Logger LOG = LoggerFactory.getLogger(ResponseProcessor.class);
  private final IntObjectMap<AbstractResponseToRequestHandler<?>> myHandlers = IntMaps.newIntObjectHashMap();
  private final MultiValuesMap<Class<? extends R>, AbstractResponseHandler<? extends R>> myClassHandlers = new MultiValuesMap<>();
  private final IntObjectMap<TimeoutHandler> myTimeoutHandlers = IntMaps.newIntObjectHashMap();
  private boolean myStopped;
  private final Object myLock = new Object();
  private Thread myThread;

  private final ScheduledExecutorService myScheduledExecutorService;

  private Future<?> myTimeoutTask;

  public ResponseProcessor(@Nonnull ScheduledExecutorService executor, @Nonnull SocketConnection<?, R> connection) {
    myScheduledExecutorService = executor;
  }

  public void startReading(final ResponseReader<R> reader) {
    myScheduledExecutorService.execute(new Runnable() {
      public void run() {
        myThread = Thread.currentThread();
        try {
          while (true) {
            final R r = reader.readResponse();
            if (r == null) break;
            if (r instanceof ResponseToRequest) {
              final int requestId = ((ResponseToRequest)r).getRequestId();
              processResponse(requestId, r);
            }
            else {
              processResponse(r);
            }
          }
        }
        catch (InterruptedException ignored) {
        }
        catch (IOException e) {
          LOG.info(e.getMessage(), e);
        }
        finally {
          synchronized (myLock) {
            myStopped = true;
          }
        }
      }
    });
  }

  private void processResponse(int requestId, R response) {
    synchronized (myLock) {
      myTimeoutHandlers.remove(requestId);
    }

    final AbstractResponseToRequestHandler handler;
    synchronized (myLock) {
      handler = myHandlers.remove(requestId);
      if (handler == null) return;
    }

    //noinspection unchecked
    if (!handler.processResponse(response)) {
      synchronized (myLock) {
        myHandlers.put(requestId, handler);
      }
    }
  }


  private void processResponse(R response) throws IOException {
    //noinspection unchecked
    final Class<R> responseClass = (Class<R>)response.getClass();

    List<AbstractResponseHandler<?>> handlers;
    synchronized (myLock) {
      final Collection<AbstractResponseHandler<? extends R>> responseHandlers = myClassHandlers.get(responseClass);
      if (responseHandlers == null) return;
      handlers = new SmartList<>(responseHandlers);
    }

    for (AbstractResponseHandler handler : handlers) {
      //noinspection unchecked
      handler.processResponse(response);
    }
  }

  public void stopReading() {
    synchronized (myLock) {
      if (myStopped) return;
      myStopped = true;
    }

    if (myThread != null) {
      myThread.interrupt();
    }
  }

  public <T extends R> void registerHandler(@Nonnull Class<T> responseClass, @Nonnull AbstractResponseHandler<T> handler) {
    synchronized (myLock) {
      myClassHandlers.put(responseClass, handler);
    }
  }

  public void registerHandler(int id, @Nonnull AbstractResponseToRequestHandler<?> handler) {
    synchronized (myLock) {
      myHandlers.put(id, handler);
    }
  }

  public void checkTimeout() {
    LOG.debug("Checking timeout");
    final List<IntObjectMap.IntObjectEntry<TimeoutHandler>> timedOut = new ArrayList<>();
    synchronized (myLock) {
      final long time = System.currentTimeMillis();

      myTimeoutHandlers.entrySet().forEach(e -> {
        if (time > e.getValue().myLastTime) {
          timedOut.add(e);
        }
      });

      for (IntObjectMap.IntObjectEntry<TimeoutHandler> entry : timedOut) {
        myTimeoutHandlers.remove(entry.getKey());
      }
    }
    for (IntObjectMap.IntObjectEntry<TimeoutHandler> entry : timedOut) {
      TimeoutHandler handler = entry.getValue();
      LOG.debug("performing timeout action: " + handler.myAction);
      handler.myAction.run();
    }
    scheduleTimeoutCheck();
  }

  private void scheduleTimeoutCheck() {
    final Ref<Long> nextTime = Ref.create(Long.MAX_VALUE);
    synchronized (myLock) {
      if (myTimeoutHandlers.isEmpty()) return;

      myTimeoutHandlers.forEach((param1, handler) -> nextTime.set(Math.min(nextTime.get(), handler.myLastTime)));
    }
    final int delay = (int)(nextTime.get() - System.currentTimeMillis() + 100);
    LOG.debug("schedule timeout check in " + delay + "ms");
    if (delay > 10) {
      if (myTimeoutTask != null) {
        myTimeoutTask.cancel(false);
      }

      myTimeoutTask = myScheduledExecutorService.schedule(() -> checkTimeout(), delay, TimeUnit.MILLISECONDS);
    }
    else {
      checkTimeout();
    }
  }

  public void registerTimeoutHandler(int commandId, int timeout, Runnable onTimeout) {
    synchronized (myLock) {
      myTimeoutHandlers.put(commandId, new TimeoutHandler(onTimeout, System.currentTimeMillis() + timeout));
    }
    scheduleTimeoutCheck();
  }

  private static class TimeoutHandler {
    private final Runnable myAction;
    private final long myLastTime;

    private TimeoutHandler(Runnable action, long lastTime) {
      myAction = action;
      myLastTime = lastTime;
    }
  }
}
