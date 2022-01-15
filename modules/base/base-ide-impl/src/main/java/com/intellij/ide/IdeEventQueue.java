/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.ide;

import com.intellij.ide.dnd.DnDManager;
import com.intellij.ide.dnd.DnDManagerImpl;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.idea.ApplicationStarter;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.impl.InvocationUtil2;
import com.intellij.openapi.application.impl.LaterInvocator;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.FrequentEventDetector;
import com.intellij.openapi.keymap.KeyboardSettingsExternalizable;
import com.intellij.openapi.keymap.impl.IdeKeyEventDispatcher;
import com.intellij.openapi.keymap.impl.IdeMouseEventDispatcher;
import com.intellij.openapi.keymap.impl.KeyState;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.FocusManagerImpl;
import com.intellij.util.Alarm;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.EDT;
import com.intellij.util.ui.UIUtil;
import consulo.application.TransactionGuardEx;
import consulo.application.internal.ApplicationWithIntentWriteLock;
import consulo.awt.TargetAWT;
import consulo.awt.hacking.InvocationUtil;
import consulo.awt.hacking.PostEventQueueHacking;
import consulo.awt.hacking.SequencedEventNestedFieldHolder;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import sun.awt.SunToolkit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.awt.event.MouseEvent.MOUSE_MOVED;
import static java.awt.event.MouseEvent.MOUSE_PRESSED;

/**
 * @author Vladimir Kondratyev
 * @author Anton Katilin
 */
public class IdeEventQueue extends EventQueue {
  private static final Logger LOG = Logger.getInstance(IdeEventQueue.class);

  private static final Logger FOCUS_AWARE_RUNNABLES_LOG = Logger.getInstance(IdeEventQueue.class.getName() + ".runnables");

  private static final Set<Class<? extends Runnable>> ourRunnablesWoWrite = Set.of(InvocationUtil.REPAINT_PROCESSING_CLASS);
  private static final Set<Class<? extends Runnable>> ourRunnablesWithWrite = Set.of(InvocationUtil2.FLUSH_NOW_CLASS);
  private static final boolean ourDefaultEventWithWrite = true;

  private static TransactionGuardEx ourTransactionGuard;
  private static ProgressManager ourProgressManager;

  /**
   * Adding/Removing of "idle" listeners should be thread safe.
   */
  private final Object myLock = new Object();

  private final List<Runnable> myIdleListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private final List<Runnable> myActivityListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private final Alarm myIdleRequestsAlarm = new Alarm();
  private final Alarm myIdleTimeCounterAlarm = new Alarm();
  private long myIdleTime;
  private final Map<Runnable, MyFireIdleRequest> myListener2Request = new HashMap<>();
  // IdleListener -> MyFireIdleRequest
  private final IdeKeyEventDispatcher myKeyEventDispatcher = new IdeKeyEventDispatcher(this);
  private final IdeMouseEventDispatcher myMouseEventDispatcher = new IdeMouseEventDispatcher();
  private final IdePopupManager myPopupManager = new IdePopupManager();
  private final ToolkitBugsProcessor myToolkitBugsProcessor = new ToolkitBugsProcessor();

  /**
   * Counter of processed events. It is used to assert that data context lives only inside single
   * <p/>
   * Swing event.
   */
  private int myEventCount;
  final AtomicInteger myKeyboardEventsPosted = new AtomicInteger();
  final AtomicInteger myKeyboardEventsDispatched = new AtomicInteger();
  private boolean myIsInInputEvent;
  private AWTEvent myCurrentEvent = new InvocationEvent(this, EmptyRunnable.getInstance());
  @Nullable
  private AWTEvent myCurrentSequencedEvent;

  private long myLastActiveTime;
  private long myLastEventTime = System.currentTimeMillis();
  private WindowManagerEx myWindowManager;
  private final List<EventDispatcher> myDispatchers = ContainerUtil.createLockFreeCopyOnWriteList();
  private final List<EventDispatcher> myPostProcessors = ContainerUtil.createLockFreeCopyOnWriteList();
  private final Set<Runnable> myReady = ContainerUtil.newHashSet();
  private boolean myKeyboardBusy;
  private boolean myWinMetaPressed;
  private int myInputMethodLock;
  private final com.intellij.util.EventDispatcher<PostEventHook> myPostEventListeners = com.intellij.util.EventDispatcher.create(PostEventHook.class);

  private final Map<AWTEvent, List<Runnable>> myRunnablesWaitingFocusChange = new LinkedHashMap<>();

  private final Queue<AWTEvent> focusEventsList = new ConcurrentLinkedQueue<>();

  public void executeWhenAllFocusEventsLeftTheQueue(@Nonnull Runnable runnable) {
    ifFocusEventsInTheQueue(e -> {
      List<Runnable> runnables = myRunnablesWaitingFocusChange.get(e);
      if (runnables != null) {
        if (FOCUS_AWARE_RUNNABLES_LOG.isDebugEnabled()) {
          FOCUS_AWARE_RUNNABLES_LOG.debug("We have already had a runnable for the event: " + e);
        }
        runnables.add(runnable);
      }
      else {
        runnables = new ArrayList<>();
        runnables.add(runnable);
        myRunnablesWaitingFocusChange.put(e, runnables);
      }
    }, runnable);
  }

  @Nonnull
  public String runnablesWaitingForFocusChangeState() {
    return StringUtil.join(focusEventsList, event -> "[" + event.getID() + "; " + event.getSource().getClass().getName() + "]", ", ");
  }

  private void ifFocusEventsInTheQueue(@Nonnull Consumer<? super AWTEvent> yes, @Nonnull Runnable no) {
    if (!focusEventsList.isEmpty()) {
      if (FOCUS_AWARE_RUNNABLES_LOG.isDebugEnabled()) {
        FOCUS_AWARE_RUNNABLES_LOG.debug("Focus event list (trying to execute runnable): " + runnablesWaitingForFocusChangeState());
      }

      // find the latest focus gained
      AWTEvent first = ContainerUtil.find(focusEventsList, e -> e.getID() == FocusEvent.FOCUS_GAINED);

      if (first != null) {
        if (FOCUS_AWARE_RUNNABLES_LOG.isDebugEnabled()) {
          FOCUS_AWARE_RUNNABLES_LOG.debug("    runnable saved for : [" + first.getID() + "; " + first.getSource() + "] -> " + no.getClass().getName());
        }
        yes.accept(first);
      }
      else {
        if (FOCUS_AWARE_RUNNABLES_LOG.isDebugEnabled()) {
          FOCUS_AWARE_RUNNABLES_LOG.debug("    runnable is run on EDT if needed : " + no.getClass().getName());
        }
        UIUtil.invokeLaterIfNeeded(no);
      }
    }
    else {
      if (FOCUS_AWARE_RUNNABLES_LOG.isDebugEnabled()) {
        FOCUS_AWARE_RUNNABLES_LOG.debug("Focus event list is empty: runnable is run right away : " + no.getClass().getName());
      }
      UIUtil.invokeLaterIfNeeded(no);
    }
  }

  private static IdeEventQueue ourInstance;

  public static void initialize() {
    if (ourInstance != null) {
      throw new Error();
    }

    ourInstance = new IdeEventQueue();
  }

  public static IdeEventQueue getInstance() {
    if (ourInstance == null) {
      throw new IllegalArgumentException("IdeEventQueue is not initialed. Probably wrote application frontend");
    }
    return ourInstance;
  }

  private IdeEventQueue() {
    EventQueue systemEventQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
    assert !(systemEventQueue instanceof IdeEventQueue) : systemEventQueue;
    systemEventQueue.push(this);
    addIdleTimeCounterRequest();

    EDT.updateEdt();

    KeyboardFocusManager keyboardFocusManager = IdeKeyboardFocusManager.replaceDefault();
    keyboardFocusManager.addPropertyChangeListener("permanentFocusOwner", e -> {
      final Application application = ApplicationManager.getApplication();
      if (application == null) {
        // We can get focus event before application is initialized
        return;
      }
      application.assertIsDispatchThread();
    });

    addDispatcher(new WindowsAltSuppressor(), null);
    addDispatcher(new EditingCanceller(), null);

    abracadabraDaberBoreh();
  }

  private void abracadabraDaberBoreh() {
    // we need to track if there are KeyBoardEvents in IdeEventQueue
    // so we want to intercept all events posted to IdeEventQueue and increment counters
    // However, we regular control flow goes like this:
    //    PostEventQueue.flush() -> EventQueue.postEvent() -> IdeEventQueue.postEventPrivate() -> AAAA we missed event, because postEventPrivate() can't be overridden.
    // Instead, we do following:
    //  - create new PostEventQueue holding our IdeEventQueue instead of old EventQueue
    //  - replace "PostEventQueue" value in AppContext with this new PostEventQueue
    // since that the control flow goes like this:
    //    PostEventQueue.flush() -> IdeEventQueue.postEvent() -> We intercepted event, incremented counters.
    PostEventQueueHacking.replacePostEventQueue(this);
  }

  public void setWindowManager(final WindowManagerEx windowManager) {
    myWindowManager = windowManager;
  }


  private void addIdleTimeCounterRequest() {
    if (isTestMode()) return;

    myIdleTimeCounterAlarm.cancelAllRequests();
    myLastActiveTime = System.currentTimeMillis();
    myIdleTimeCounterAlarm.addRequest(() -> {
      myIdleTime += System.currentTimeMillis() - myLastActiveTime;
      addIdleTimeCounterRequest();
    }, 20000, ModalityState.NON_MODAL);
  }

  /**
   * This class performs special processing in order to have {@link #getIdleTime()} return more or less up-to-date data.
   * <p/>
   * This method allows to stop that processing (convenient in non-intellij environment like upsource).
   */
  @SuppressWarnings("unused") // Used in upsource.
  public void stopIdleTimeCalculation() {
    myIdleTimeCounterAlarm.cancelAllRequests();
  }

  public void addIdleListener(@Nonnull final Runnable runnable, final int timeoutMillis) {
    if (timeoutMillis <= 0 || TimeUnit.MILLISECONDS.toHours(timeoutMillis) >= 24) {
      throw new IllegalArgumentException("This timeout value is unsupported: " + timeoutMillis);
    }
    synchronized (myLock) {
      myIdleListeners.add(runnable);
      final MyFireIdleRequest request = new MyFireIdleRequest(runnable, timeoutMillis);
      myListener2Request.put(runnable, request);
      UIUtil.invokeLaterIfNeeded(() -> myIdleRequestsAlarm.addRequest(request, timeoutMillis));
    }
  }

  public void removeIdleListener(@Nonnull final Runnable runnable) {
    synchronized (myLock) {
      final boolean wasRemoved = myIdleListeners.remove(runnable);
      if (!wasRemoved) {
        LOG.error("unknown runnable: " + runnable);
      }
      final MyFireIdleRequest request = myListener2Request.remove(runnable);
      LOG.assertTrue(request != null);
      myIdleRequestsAlarm.cancelRequest(request);
    }
  }

  /**
   * @deprecated use {@link #addActivityListener(Runnable, Disposable)} (to be removed in IDEA 17)
   */
  public void addActivityListener(@Nonnull final Runnable runnable) {
    synchronized (myLock) {
      myActivityListeners.add(runnable);
    }
  }

  public void addActivityListener(@Nonnull final Runnable runnable, Disposable parentDisposable) {
    synchronized (myLock) {
      DisposerUtil.add(runnable, myActivityListeners, parentDisposable);
    }
  }

  public void removeActivityListener(@Nonnull final Runnable runnable) {
    synchronized (myLock) {
      myActivityListeners.remove(runnable);
    }
  }

  public void addDispatcher(@Nonnull EventDispatcher dispatcher, Disposable parent) {
    _addProcessor(dispatcher, parent, myDispatchers);
  }

  public void removeDispatcher(@Nonnull EventDispatcher dispatcher) {
    myDispatchers.remove(dispatcher);
  }

  public boolean containsDispatcher(@Nonnull EventDispatcher dispatcher) {
    return myDispatchers.contains(dispatcher);
  }

  public void addPostprocessor(@Nonnull EventDispatcher dispatcher, @Nullable Disposable parent) {
    _addProcessor(dispatcher, parent, myPostProcessors);
  }

  public void removePostprocessor(@Nonnull EventDispatcher dispatcher) {
    myPostProcessors.remove(dispatcher);
  }

  private static void _addProcessor(@Nonnull EventDispatcher dispatcher, Disposable parent, @Nonnull Collection<EventDispatcher> set) {
    set.add(dispatcher);
    if (parent != null) {
      Disposer.register(parent, () -> set.remove(dispatcher));
    }
  }

  public int getEventCount() {
    return myEventCount;
  }

  public void setEventCount(int evCount) {
    myEventCount = evCount;
  }

  public AWTEvent getTrueCurrentEvent() {
    return myCurrentEvent;
  }

  private static boolean ourAppIsLoaded;

  private static boolean appIsLoaded() {
    if (ourAppIsLoaded) return true;
    boolean loaded = ApplicationStarter.isLoaded();
    if (loaded) {
      ourAppIsLoaded = true;
    }
    return loaded;
  }

  //Use for GuiTests to stop IdeEventQueue when application is disposed already
  public static void applicationClose() {
    ourAppIsLoaded = false;
  }

  @Override
  public void dispatchEvent(@Nonnull AWTEvent e) {
    // DO NOT ADD ANYTHING BEFORE fixNestedSequenceEvent is called

    fixNestedSequenceEvent(e);
    // Add code below if you need

    // Update EDT if it changes (might happen after Application disposal)
    EDT.updateEdt();

    if (e.getSource() instanceof TrayIcon) {
      dispatchTrayIconEvent(e);
      return;
    }

    checkForTimeJump();
    if (!appIsLoaded()) {
      try {
        super.dispatchEvent(e);
      }
      catch (Throwable t) {
        processException(t);
      }
      return;
    }

    e = fixNonEnglishKeyboardLayouts(e);

    e = mapEvent(e);
    AWTEvent metaEvent = mapMetaState(e);
    if (metaEvent != null && Registry.is("keymap.windows.as.meta")) {
      e = metaEvent;
    }

    boolean wasInputEvent = myIsInInputEvent;
    myIsInInputEvent = isInputEvent(e);
    AWTEvent oldEvent = myCurrentEvent;
    myCurrentEvent = e;

    AWTEvent finalE1 = e;
    Runnable runnable = InvocationUtil.extractRunnable(e);
    Class<? extends Runnable> runnableClass = runnable != null ? runnable.getClass() : Runnable.class;
    Runnable processEventRunnable = () -> {
      Application application = ApplicationManager.getApplication();
      ProgressManager progressManager = application != null && !application.isDisposed() ? ProgressManager.getInstance() : null;

      try (AccessToken ignored = startActivity(finalE1)) {
        if (progressManager != null) {
          progressManager.computePrioritized(() -> {
            _dispatchEvent(myCurrentEvent);
            return null;
          });
        }
        else {
          _dispatchEvent(myCurrentEvent);
        }
      }
      catch (Throwable t) {
        processException(t);
      }
      finally {
        myIsInInputEvent = wasInputEvent;
        myCurrentEvent = oldEvent;

        if (myCurrentSequencedEvent == finalE1) {
          myCurrentSequencedEvent = null;
        }

        for (EventDispatcher each : myPostProcessors) {
          each.dispatch(finalE1);
        }

        if (finalE1 instanceof KeyEvent) {
          maybeReady();
        }
        //if (eventWatcher != null && runnableClass != InvocationUtil.FLUSH_NOW_CLASS) {
        //  eventWatcher.logTimeMillis(runnableClass != Runnable.class ? runnableClass.getName() : finalE1.toString(), startedAt, runnableClass);
        //}
      }

      if (isFocusEvent(finalE1)) {
        onFocusEvent(finalE1);
      }
    };

    if (runnableClass != Runnable.class) {
      if (ourRunnablesWoWrite.contains(runnableClass)) {
        processEventRunnable.run();
        return;
      }
      if (ourRunnablesWithWrite.contains(runnableClass)) {
        ((ApplicationWithIntentWriteLock)Application.get()).runIntendedWriteActionOnCurrentThread(processEventRunnable);
        return;
      }
    }

    if (ourDefaultEventWithWrite) {
      ((ApplicationWithIntentWriteLock)Application.get()).runIntendedWriteActionOnCurrentThread(processEventRunnable);
    }
    else {
      processEventRunnable.run();
    }
  }

  // Fixes IDEA-218430: nested sequence events cause deadlock
  private void fixNestedSequenceEvent(@Nonnull AWTEvent e) {
    if (e.getClass() == SequencedEventNestedFieldHolder.SEQUENCED_EVENT_CLASS) {
      if (myCurrentSequencedEvent != null) {
        AWTEvent sequenceEventToDispose = myCurrentSequencedEvent;
        myCurrentSequencedEvent = null; // Set to null BEFORE dispose b/c `dispose` can dispatch events internally
        SequencedEventNestedFieldHolder.invokeDispose(sequenceEventToDispose);
      }
      myCurrentSequencedEvent = e;
    }
  }

  //As we rely on system time monotonicity in many places let's log anomalies at least.
  private void checkForTimeJump() {
    long now = System.currentTimeMillis();
    if (myLastEventTime > now + 1000) {
      LOG.warn("System clock's jumped back by ~" + (myLastEventTime - now) / 1000 + " sec");
    }
    myLastEventTime = now;
  }

  private static boolean isInputEvent(@Nonnull AWTEvent e) {
    return e instanceof InputEvent || e instanceof InputMethodEvent || e instanceof WindowEvent || e instanceof ActionEvent;
  }


  @Nullable
  private static ProgressManager obtainProgressManager() {
    ProgressManager manager = ourProgressManager;
    if (manager == null) {
      Application app = ApplicationManager.getApplication();
      if (app != null && !app.isDisposed()) {
        ourProgressManager = manager = ServiceManager.getService(ProgressManager.class);
      }
    }
    return manager;
  }

  @Override
  @Nonnull
  public AWTEvent getNextEvent() throws InterruptedException {
    AWTEvent event = appIsLoaded() ? ((ApplicationWithIntentWriteLock)Application.get()).runUnlockingIntendedWrite(() -> super.getNextEvent()) : super.getNextEvent();

    if (isKeyboardEvent(event) && myKeyboardEventsDispatched.incrementAndGet() > myKeyboardEventsPosted.get()) {
      throw new RuntimeException(event + "; posted: " + myKeyboardEventsPosted + "; dispatched: " + myKeyboardEventsDispatched);
    }
    return event;
  }

  @Nullable
  static AccessToken startActivity(@Nonnull AWTEvent e) {
    if (ourTransactionGuard == null && appIsLoaded()) {
      if (ApplicationManager.getApplication() != null && !ApplicationManager.getApplication().isDisposed()) {
        ourTransactionGuard = (TransactionGuardEx)TransactionGuard.getInstance();
      }
    }
    return ourTransactionGuard == null ? null : ourTransactionGuard.startActivity(isInputEvent(e) || e instanceof ItemEvent || e instanceof FocusEvent);
  }

  private void processException(@Nonnull Throwable t) {
    if (!myToolkitBugsProcessor.process(t)) {
      PluginManager.processException(t);
    }
  }

  @Nonnull
  private static AWTEvent fixNonEnglishKeyboardLayouts(@Nonnull AWTEvent e) {
    if (!(e instanceof KeyEvent)) return e;

    KeyboardSettingsExternalizable externalizable = KeyboardSettingsExternalizable.getInstance();
    if (!Registry.is("ide.non.english.keyboard.layout.fix") || externalizable == null || !externalizable.isNonEnglishKeyboardSupportEnabled()) return e;

    KeyEvent ke = (KeyEvent)e;

    switch (ke.getID()) {
      case KeyEvent.KEY_PRESSED:
        break;
      case KeyEvent.KEY_RELEASED:
        break;
    }

    //if (!leftAltIsPressed && KeyboardSettingsExternalizable.getInstance().isUkrainianKeyboard(sourceComponent)) {
    //  if ('ґ' == ke.getKeyChar() || ke.getKeyCode() == KeyEvent.VK_U) {
    //    ke = new KeyEvent(ke.getComponent(), ke.getID(), ke.getWhen(), 0,
    //                     KeyEvent.VK_UNDEFINED, 'ґ', ke.getKeyLocation());
    //    ke.setKeyCode(KeyEvent.VK_U);
    //    ke.setKeyChar('ґ');
    //    return ke;
    //  }
    //}

    // NB: Standard keyboard layout is an English keyboard layout. If such
    //     layout is active every KeyEvent that is received has
    //     a @{code KeyEvent.getKeyCode} key code corresponding to
    //     the @{code KeyEvent.getKeyChar} key char in the event.
    //     For  example, VK_MINUS key code and '-' character
    //
    // We have a key char. On some non standard layouts it does not correspond to
    // key code in the event.

    int keyCodeFromChar = CharToVKeyMap.get(ke.getKeyChar());

    // Now we have a correct key code as if we'd gotten  a KeyEvent for
    // standard English layout

    if (keyCodeFromChar == ke.getKeyCode() || keyCodeFromChar == KeyEvent.VK_UNDEFINED) {
      return e;
    }

    // Farther we handle a non standard layout
    // non-english layout
    ke.setKeyCode(keyCodeFromChar);

    return ke;
  }

  @Nonnull
  private static AWTEvent mapEvent(@Nonnull AWTEvent e) {
    return SystemInfo.isXWindow && e instanceof MouseEvent && ((MouseEvent)e).getButton() > 3 ? mapXWindowMouseEvent((MouseEvent)e) : e;
  }

  @Nonnull
  private static AWTEvent mapXWindowMouseEvent(MouseEvent src) {
    if (src.getButton() < 6) {
      // Convert these events(buttons 4&5 in are produced by touchpad, they must be converted to horizontal scrolling events
      return new MouseWheelEvent(src.getComponent(), MouseEvent.MOUSE_WHEEL, src.getWhen(), src.getModifiers() | InputEvent.SHIFT_DOWN_MASK, src.getX(), src.getY(), 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, src.getClickCount(), src.getButton() == 4 ? -1 : 1);
    }

    // Here we "shift" events with buttons 6 and 7 to similar events with buttons 4 and 5
    // See java.awt.InputEvent#BUTTON_DOWN_MASK, 1<<14 is 4th physical button, 1<<15 is 5th.
    //noinspection MagicConstant
    return new MouseEvent(src.getComponent(), src.getID(), src.getWhen(), src.getModifiers() | (1 << 8 + src.getButton()), src.getX(), src.getY(), 1, src.isPopupTrigger(), src.getButton() - 2);
  }

  /**
   * Here we try to use 'Windows' key like modifier, so we patch events with modifier 'Meta'
   * when 'Windows' key was pressed and still is not released.
   *
   * @param e event to be patched
   * @return new 'patched' event if need, otherwise null
   * <p>
   * Note: As side-effect this method tracks special flag for 'Windows' key state that is valuable on itself
   */
  @Nullable
  private AWTEvent mapMetaState(@Nonnull AWTEvent e) {
    if (myWinMetaPressed) {
      Application app = ApplicationManager.getApplication();

      boolean weAreNotActive = app == null || !app.isActive();
      weAreNotActive |= e instanceof FocusEvent && ((FocusEvent)e).getOppositeComponent() == null;
      if (weAreNotActive) {
        myWinMetaPressed = false;
        return null;
      }
    }

    if (e instanceof KeyEvent) {
      KeyEvent ke = (KeyEvent)e;
      if (ke.getKeyCode() == KeyEvent.VK_WINDOWS) {
        if (ke.getID() == KeyEvent.KEY_PRESSED) myWinMetaPressed = true;
        if (ke.getID() == KeyEvent.KEY_RELEASED) myWinMetaPressed = false;
        return null;
      }
      if (myWinMetaPressed) {
        return new KeyEvent(ke.getComponent(), ke.getID(), ke.getWhen(), ke.getModifiers() | ke.getModifiersEx() | InputEvent.META_MASK, ke.getKeyCode(), ke.getKeyChar(), ke.getKeyLocation());
      }
    }

    if (myWinMetaPressed && e instanceof MouseEvent && ((MouseEvent)e).getButton() != 0) {
      MouseEvent me = (MouseEvent)e;
      return new MouseEvent(me.getComponent(), me.getID(), me.getWhen(), me.getModifiers() | me.getModifiersEx() | InputEvent.META_MASK, me.getX(), me.getY(), me.getClickCount(), me.isPopupTrigger(),
                            me.getButton());
    }

    return null;
  }

  public void _dispatchEvent(@Nonnull AWTEvent e) {
    if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
      DnDManagerImpl dndManager = (DnDManagerImpl)DnDManager.getInstance();
      dndManager.setLastDropHandler(null);
    }

    myEventCount++;

    if (processAppActivationEvents(e)) return;

    myKeyboardBusy = e instanceof KeyEvent || myKeyboardEventsPosted.get() > myKeyboardEventsDispatched.get();

    if (e instanceof KeyEvent) {
      if (e.getID() == KeyEvent.KEY_RELEASED && ((KeyEvent)e).getKeyCode() == KeyEvent.VK_SHIFT) {
        myMouseEventDispatcher.resetHorScrollingTracker();
      }
    }

    if (e instanceof MouseWheelEvent) {
      final MenuElement[] selectedPath = MenuSelectionManager.defaultManager().getSelectedPath();
      if (selectedPath.length > 0 && !(selectedPath[0] instanceof ComboPopup)) {
        ((MouseWheelEvent)e).consume();
        Component component = selectedPath[0].getComponent();
        if (component instanceof JBPopupMenu) {
          ((JBPopupMenu)component).processMouseWheelEvent((MouseWheelEvent)e);
        }
        return;
      }
    }


    // Process "idle" and "activity" listeners
    if (e instanceof WindowEvent || e instanceof FocusEvent || e instanceof InputEvent) {
      ActivityTracker.getInstance().inc();

      synchronized (myLock) {
        myIdleRequestsAlarm.cancelAllRequests();
        for (Runnable idleListener : myIdleListeners) {
          final MyFireIdleRequest request = myListener2Request.get(idleListener);
          if (request == null) {
            LOG.error("There is no request for " + idleListener);
          }
          else {
            myIdleRequestsAlarm.addRequest(request, request.getTimeout(), ModalityState.NON_MODAL);
          }
        }
        if (KeyEvent.KEY_PRESSED == e.getID() ||
            KeyEvent.KEY_TYPED == e.getID() ||
            MouseEvent.MOUSE_PRESSED == e.getID() ||
            MouseEvent.MOUSE_RELEASED == e.getID() ||
            MouseEvent.MOUSE_CLICKED == e.getID()) {
          addIdleTimeCounterRequest();
          for (Runnable activityListener : myActivityListeners) {
            activityListener.run();
          }
        }
      }
    }

    if (myPopupManager.isPopupActive() && myPopupManager.dispatch(e)) {
      if (myKeyEventDispatcher.isWaitingForSecondKeyStroke()) {
        myKeyEventDispatcher.setState(KeyState.STATE_INIT);
      }

      return;
    }

    if (dispatchByCustomDispatchers(e)) return;

    if (e instanceof InputMethodEvent) {
      if (SystemInfo.isMac && myKeyEventDispatcher.isWaitingForSecondKeyStroke()) {
        return;
      }
    }

    if (e instanceof ComponentEvent && myWindowManager != null) {
      myWindowManager.dispatchComponentEvent((ComponentEvent)e);
    }

    if (e instanceof KeyEvent) {
      if (!myKeyEventDispatcher.dispatchKeyEvent((KeyEvent)e)) {
        defaultDispatchEvent(e);
      }
      else {
        ((KeyEvent)e).consume();
        defaultDispatchEvent(e);
      }
    }
    else if (e instanceof MouseEvent) {
      MouseEvent me = (MouseEvent)e;
      if (me.getID() == MOUSE_PRESSED && me.getModifiers() > 0 && me.getModifiersEx() == 0) {
        // In case of these modifiers java.awt.Container#LightweightDispatcher.processMouseEvent() uses a recent 'active' component
        // from inner WeakReference (see mouseEventTarget field) even if the component has been already removed from component hierarchy.
        // So we have to reset this WeakReference with synthetic event just before processing of actual event
        super.dispatchEvent(new MouseEvent(me.getComponent(), MOUSE_MOVED, me.getWhen(), 0, me.getX(), me.getY(), 0, false, 0));
      }
      if (IdeMouseEventDispatcher.patchClickCount(me) && me.getID() == MouseEvent.MOUSE_CLICKED) {
        final MouseEvent toDispatch = new MouseEvent(me.getComponent(), me.getID(), System.currentTimeMillis(), me.getModifiers(), me.getX(), me.getY(), 1, me.isPopupTrigger(), me.getButton());
        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(() -> dispatchEvent(toDispatch));
      }
      if (!myMouseEventDispatcher.dispatchMouseEvent(me)) {
        defaultDispatchEvent(e);
      }
    }
    else {
      defaultDispatchEvent(e);
    }
  }

  private boolean dispatchByCustomDispatchers(@Nonnull AWTEvent e) {
    for (EventDispatcher eachDispatcher : myDispatchers) {
      if (eachDispatcher.dispatch(e)) {
        return true;
      }
    }
    return false;
  }

  private static void fixStickyWindow(@Nonnull KeyboardFocusManager mgr, Window wnd, @Nonnull String resetMethod) {
    if (wnd != null && !wnd.isShowing()) {
      Window showingWindow = wnd;
      while (showingWindow != null) {
        if (showingWindow.isShowing()) break;
        showingWindow = (Window)showingWindow.getParent();
      }

      if (showingWindow == null) {
        final Frame[] allFrames = Frame.getFrames();
        for (Frame each : allFrames) {
          if (each.isShowing()) {
            showingWindow = each;
            break;
          }
        }
      }


      if (showingWindow != null && showingWindow != wnd) {
        final Method setActive = ReflectionUtil.findMethod(ReflectionUtil.getClassDeclaredMethods(KeyboardFocusManager.class, false), resetMethod, Window.class);
        if (setActive != null) {
          try {
            setActive.invoke(mgr, (Window)showingWindow);
          }
          catch (Exception exc) {
            LOG.info(exc);
          }
        }
      }
    }
  }

  public static boolean isMouseEventAhead(@Nullable AWTEvent e) {
    IdeEventQueue queue = getInstance();
    return e instanceof MouseEvent || queue.peekEvent(MouseEvent.MOUSE_PRESSED) != null || queue.peekEvent(MouseEvent.MOUSE_RELEASED) != null || queue.peekEvent(MouseEvent.MOUSE_CLICKED) != null;
  }

  private static boolean processAppActivationEvents(@Nonnull AWTEvent e) {
    if (e instanceof WindowEvent) {
      final WindowEvent we = (WindowEvent)e;

      ApplicationActivationStateManager.updateState(we);

      storeLastFocusedComponent(we);
    }

    return false;
  }

  private static void storeLastFocusedComponent(@Nonnull WindowEvent we) {
    final Window eventWindow = we.getWindow();

    if (we.getID() == WindowEvent.WINDOW_DEACTIVATED || we.getID() == WindowEvent.WINDOW_LOST_FOCUS) {
      Component frame = UIUtil.findUltimateParent(eventWindow);
      Component focusOwnerInDeactivatedWindow = eventWindow.getMostRecentFocusOwner();
      IdeFrame[] allProjectFrames = WindowManager.getInstance().getAllProjectFrames();

      if (focusOwnerInDeactivatedWindow != null) {
        for (IdeFrame ideFrame : allProjectFrames) {
          Window aFrame = TargetAWT.to(WindowManager.getInstance().getWindow(ideFrame.getProject()));
          if (aFrame.equals(frame)) {
            IdeFocusManager focusManager = IdeFocusManager.getGlobalInstance();
            if (focusManager instanceof FocusManagerImpl) {
              ((FocusManagerImpl)focusManager).setLastFocusedAtDeactivation(ideFrame, focusOwnerInDeactivatedWindow);
            }
          }
        }
      }
    }
  }

  private void defaultDispatchEvent(@Nonnull AWTEvent e) {
    try {
      maybeReady();
      fixStickyAlt(e);
      super.dispatchEvent(e);
    }
    catch (Throwable t) {
      processException(t);
    }
  }

  private static class FieldHolder {
    private static final Field ourStickyAltField;

    static {
      Field field;
      try {
        Class<?> aClass = Class.forName("com.sun.java.swing.plaf.windows.WindowsRootPaneUI$AltProcessor");
        field = ReflectionUtil.getDeclaredField(aClass, "menuCanceledOnPress");
      }
      catch (Exception e) {
        field = null;
      }
      ourStickyAltField = field;
    }
  }

  private static void fixStickyAlt(@Nonnull AWTEvent e) {
    if (Registry.is("actionSystem.win.suppressAlt.new")) {
      if (UIUtil.isUnderWindowsLookAndFeel() &&
          e instanceof InputEvent &&
          (((InputEvent)e).getModifiers() & (InputEvent.ALT_MASK | InputEvent.ALT_DOWN_MASK)) != 0 &&
          !(e instanceof KeyEvent && ((KeyEvent)e).getKeyCode() == KeyEvent.VK_ALT)) {
        try {
          if (FieldHolder.ourStickyAltField != null) {
            FieldHolder.ourStickyAltField.set(null, true);
          }
        }
        catch (Exception exception) {
          LOG.error(exception);
        }
      }
    }
    else if (SystemInfo.isWinXpOrNewer && !SystemInfo.isWinVistaOrNewer && e instanceof KeyEvent && ((KeyEvent)e).getKeyCode() == KeyEvent.VK_ALT) {
      ((KeyEvent)e).consume();  // IDEA-17359
    }
  }

  public void flushQueue() {
    while (true) {
      AWTEvent event = peekEvent();
      if (event == null) return;
      try {
        AWTEvent event1 = getNextEvent();
        dispatchEvent(event1);
      }
      catch (Exception e) {
        LOG.error(e); //?
      }
    }
  }

  public void pumpEventsForHierarchy(@Nonnull Component modalComponent, @Nonnull Future<?> exitCondition, @Nonnull Predicate<? super AWTEvent> isCancelEvent) {
    assert EventQueue.isDispatchThread();
    if (LOG.isDebugEnabled()) {
      LOG.debug("pumpEventsForHierarchy(" + modalComponent + ", " + exitCondition + ")");
    }
    while (!exitCondition.isDone()) {
      try {
        AWTEvent event = getNextEvent();
        boolean consumed = consumeUnrelatedEvent(modalComponent, event);
        if (!consumed) {
          dispatchEvent(event);
        }
        if (isCancelEvent.test(event)) {
          break;
        }
      }
      catch (Throwable e) {
        LOG.error(e);
      }
    }
    if (LOG.isDebugEnabled()) {
      LOG.debug("pumpEventsForHierarchy.exit(" + modalComponent + ", " + exitCondition + ")");
    }
  }

  // return true if consumed
  private static boolean consumeUnrelatedEvent(@Nonnull Component modalComponent, @Nonnull AWTEvent event) {
    boolean consumed = false;
    if (event instanceof InputEvent) {
      Object s = event.getSource();
      if (s instanceof Component) {
        Component c = (Component)s;
        Window modalWindow = SwingUtilities.windowForComponent(modalComponent);
        while (c != null && c != modalWindow) c = c.getParent();
        if (c == null) {
          consumed = true;
          if (LOG.isDebugEnabled()) {
            LOG.debug("pumpEventsForHierarchy.consumed: " + event);
          }
          ((InputEvent)event).consume();
        }
      }
    }
    return consumed;
  }

  @FunctionalInterface
  public interface EventDispatcher {
    boolean dispatch(@Nonnull AWTEvent e);
  }

  private final class MyFireIdleRequest implements Runnable {
    private final Runnable myRunnable;
    private final int myTimeout;


    MyFireIdleRequest(@Nonnull Runnable runnable, final int timeout) {
      myTimeout = timeout;
      myRunnable = runnable;
    }


    @Override
    public void run() {
      myRunnable.run();
      synchronized (myLock) {
        if (myIdleListeners.contains(myRunnable)) // do not reschedule if not interested anymore
        {
          myIdleRequestsAlarm.addRequest(this, myTimeout, ModalityState.NON_MODAL);
        }
      }
    }

    public int getTimeout() {
      return myTimeout;
    }

    @Override
    public String toString() {
      return "Fire idle request. delay: " + getTimeout() + "; runnable: " + myRunnable;
    }
  }

  public long getIdleTime() {
    return myIdleTime;
  }


  @Nonnull
  public IdePopupManager getPopupManager() {
    return myPopupManager;
  }

  @Nonnull
  public IdeKeyEventDispatcher getKeyEventDispatcher() {
    return myKeyEventDispatcher;
  }

  /**
   * Same as {@link #blockNextEvents(MouseEvent, IdeEventQueue.BlockMode)} with {@code blockMode} equal to {@code COMPLETE}.
   */
  public void blockNextEvents(@Nonnull MouseEvent e) {
    blockNextEvents(e, BlockMode.COMPLETE);
  }

  /**
   * When {@code blockMode} is {@code COMPLETE}, blocks following related mouse events completely, when {@code blockMode} is
   * {@code ACTIONS} only blocks performing actions bound to corresponding mouse shortcuts.
   */
  public void blockNextEvents(@Nonnull MouseEvent e, @Nonnull BlockMode blockMode) {
    myMouseEventDispatcher.blockNextEvents(e, blockMode);
  }

  public boolean hasFocusEventsPending() {
    return peekEvent(FocusEvent.FOCUS_GAINED) != null || peekEvent(FocusEvent.FOCUS_LOST) != null;
  }

  private boolean isReady() {
    return !myKeyboardBusy && myKeyEventDispatcher.isReady();
  }

  public void maybeReady() {
    if (myReady.isEmpty() || !isReady()) return;

    Runnable[] ready = myReady.toArray(new Runnable[myReady.size()]);
    myReady.clear();

    for (Runnable each : ready) {
      each.run();
    }
  }

  public void doWhenReady(@Nonnull Runnable runnable) {
    if (EventQueue.isDispatchThread()) {
      myReady.add(runnable);
      maybeReady();
    }
    else {
      //noinspection SSBasedInspection
      SwingUtilities.invokeLater(() -> {
        myReady.add(runnable);
        maybeReady();
      });
    }
  }

  public boolean isPopupActive() {
    return myPopupManager.isPopupActive();
  }

  private static class WindowsAltSuppressor implements EventDispatcher {
    private boolean myWaitingForAltRelease;
    private Robot myRobot;

    @Override
    public boolean dispatch(@Nonnull AWTEvent e) {
      boolean dispatch = true;
      if (e instanceof KeyEvent) {
        KeyEvent ke = (KeyEvent)e;
        final Component component = ke.getComponent();
        boolean pureAlt = ke.getKeyCode() == KeyEvent.VK_ALT && (ke.getModifiers() | InputEvent.ALT_MASK) == InputEvent.ALT_MASK;
        if (!pureAlt) {
          myWaitingForAltRelease = false;
        }
        else {
          UISettings uiSettings = UISettings.getInstanceOrNull();
          if (uiSettings == null || !SystemInfo.isWindows || !Registry.is("actionSystem.win.suppressAlt") || !(uiSettings.getHideToolStripes() || uiSettings.getPresentationMode())) {
            return false;
          }

          if (ke.getID() == KeyEvent.KEY_PRESSED) {
            dispatch = !myWaitingForAltRelease;
          }
          else if (ke.getID() == KeyEvent.KEY_RELEASED) {
            if (myWaitingForAltRelease) {
              myWaitingForAltRelease = false;
              dispatch = false;
            }
            else if (component != null) {
              //noinspection SSBasedInspection
              SwingUtilities.invokeLater(() -> {
                try {
                  final Window window = UIUtil.getWindow(component);
                  if (window == null || !window.isActive()) {
                    return;
                  }
                  myWaitingForAltRelease = true;
                  if (myRobot == null) {
                    myRobot = new Robot();
                  }
                  myRobot.keyPress(KeyEvent.VK_ALT);
                  myRobot.keyRelease(KeyEvent.VK_ALT);
                }
                catch (AWTException e1) {
                  LOG.debug(e1);
                }
              });
            }
          }
        }
      }

      return !dispatch;
    }
  }

  //We have to stop editing with <ESC> (if any) and consume the event to prevent any further processing (dialog closing etc.)
  private static class EditingCanceller implements EventDispatcher {
    @Override
    public boolean dispatch(@Nonnull AWTEvent e) {
      if (e instanceof KeyEvent && e.getID() == KeyEvent.KEY_PRESSED && ((KeyEvent)e).getKeyCode() == KeyEvent.VK_ESCAPE) {
        final Component owner =
                UIUtil.findParentByCondition(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(), component -> component instanceof JTable || component instanceof JTree);

        if (owner instanceof JTable && ((JTable)owner).isEditing()) {
          ((JTable)owner).editingCanceled(null);
          return true;
        }
        if (owner instanceof JTree && ((JTree)owner).isEditing()) {
          ((JTree)owner).cancelEditing();
          return true;
        }
      }
      return false;
    }
  }

  public boolean isInputMethodEnabled() {
    return !SystemInfo.isMac || myInputMethodLock == 0;
  }

  private void onFocusEvent(@Nonnull AWTEvent event) {
    if (FOCUS_AWARE_RUNNABLES_LOG.isDebugEnabled()) {
      FOCUS_AWARE_RUNNABLES_LOG.debug("Focus event list (execute on focus event): " + runnablesWaitingForFocusChangeState());
    }
    List<AWTEvent> events = new ArrayList<>();
    while (!focusEventsList.isEmpty()) {
      AWTEvent f = focusEventsList.poll();
      events.add(f);
      if (f.equals(event)) {
        break;
      }
    }

    for (AWTEvent entry : events) {
      List<Runnable> runnables = myRunnablesWaitingFocusChange.remove(entry);
      if (runnables == null) {
        continue;
      }

      for (Runnable r : runnables) {
        if (r != null && !(r instanceof ExpirableRunnable && ((ExpirableRunnable)r).isExpired())) {
          try {
            r.run();
          }
          catch (Throwable e) {
            processException(e);
          }
        }
      }
    }
  }

  public void disableInputMethods(@Nonnull Disposable parentDisposable) {
    myInputMethodLock++;
    Disposer.register(parentDisposable, () -> myInputMethodLock--);
  }

  private final FrequentEventDetector myFrequentEventDetector = new FrequentEventDetector(1009, 100);

  @Override
  public void postEvent(@Nonnull AWTEvent event) {
    doPostEvent(event);
  }

  // return true if posted, false if consumed immediately
  boolean doPostEvent(@Nonnull AWTEvent event) {
    for (PostEventHook listener : myPostEventListeners.getListeners()) {
      if (listener.consumePostedEvent(event)) return false;
    }

    myFrequentEventDetector.eventHappened(event);
    if (isKeyboardEvent(event)) {
      myKeyboardEventsPosted.incrementAndGet();
    }

    if (isFocusEvent(event)) {
      focusEventsList.add(event);
    }

    super.postEvent(event);
    return true;
  }

  private static boolean isFocusEvent(@Nonnull AWTEvent e) {
    return e.getID() == FocusEvent.FOCUS_GAINED ||
           e.getID() == FocusEvent.FOCUS_LOST ||
           e.getID() == WindowEvent.WINDOW_ACTIVATED ||
           e.getID() == WindowEvent.WINDOW_DEACTIVATED ||
           e.getID() == WindowEvent.WINDOW_LOST_FOCUS ||
           e.getID() == WindowEvent.WINDOW_GAINED_FOCUS;
  }

  private static boolean isKeyboardEvent(@Nonnull AWTEvent event) {
    return event instanceof KeyEvent;
  }

  @Override
  public AWTEvent peekEvent() {
    AWTEvent event = super.peekEvent();
    if (event != null) {
      return event;
    }
    if (isTestMode() && LaterInvocator.ensureFlushRequested()) {
      return super.peekEvent();
    }
    return null;
  }

  private Boolean myTestMode;

  private boolean isTestMode() {
    Boolean testMode = myTestMode;
    if (testMode != null) return testMode;

    Application application = ApplicationManager.getApplication();
    if (application == null) return false;

    testMode = application.isUnitTestMode();
    myTestMode = testMode;
    return testMode;
  }

  /**
   * @see IdeEventQueue#blockNextEvents(MouseEvent, IdeEventQueue.BlockMode)
   */
  public enum BlockMode {
    COMPLETE,
    ACTIONS
  }

  private static void dispatchTrayIconEvent(@Nonnull AWTEvent e) {
    if (e instanceof ActionEvent) {
      for (ActionListener listener : ((TrayIcon)e.getSource()).getActionListeners()) {
        listener.actionPerformed((ActionEvent)e);
      }
    }
  }

  public void flushDelayedKeyEvents() {
    //long startedAt = System.currentTimeMillis();
    //if (!isActionPopupShown() && delayKeyEvents.compareAndSet(true, false)) {
    //  postDelayedKeyEvents();
    //}
    //TransactionGuardImpl.logTimeMillis(startedAt, "IdeEventQueue#flushDelayedKeyEvents");
  }

  /**
   * An absolutely guru API, please avoid using it at all cost.
   */
  @FunctionalInterface
  public interface PostEventHook extends EventListener {
    /**
     * @return true if event is handled by the listener and should't be added to event queue at all
     */
    boolean consumePostedEvent(@Nonnull AWTEvent event);
  }

  public void addPostEventListener(@Nonnull PostEventHook listener, @Nonnull Disposable parentDisposable) {
    myPostEventListeners.addListener(listener, parentDisposable);
  }

  private static Ref<Method> unsafeNonBlockingExecuteRef;

  /**
   * Must be called on the Event Dispatching thread.
   * Executes the runnable so that it can perform a non-blocking invocation on the toolkit thread.
   * Not for general-purpose usage.
   *
   * @param r the runnable to execute
   */
  public static void unsafeNonblockingExecute(Runnable r) {
    assert EventQueue.isDispatchThread();
    if (unsafeNonBlockingExecuteRef == null) {
      // The method is available in JBSDK.
      unsafeNonBlockingExecuteRef = Ref.create(ReflectionUtil.getDeclaredMethod(SunToolkit.class, "unsafeNonblockingExecute", Runnable.class));
    }
    if (unsafeNonBlockingExecuteRef.get() != null) {
      try {
        unsafeNonBlockingExecuteRef.get().invoke(Toolkit.getDefaultToolkit(), r);
        return;
      }
      catch (Exception ignore) {
      }
    }
    r.run();
  }
}
