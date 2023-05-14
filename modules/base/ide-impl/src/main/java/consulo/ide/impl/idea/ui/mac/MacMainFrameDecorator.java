/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.mac;

import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.application.internal.ApplicationInfo;
import consulo.application.ApplicationManager;
import consulo.component.util.BuildNumber;
import consulo.application.util.SystemInfo;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.ide.impl.idea.openapi.wm.impl.IdeFrameDecorator;
import consulo.ide.impl.idea.ui.CustomProtocolHandler;
import consulo.application.util.mac.foundation.Foundation;
import consulo.application.util.mac.foundation.ID;
import consulo.ide.impl.idea.ui.mac.foundation.MacUtil;
import consulo.ide.impl.idea.util.EventDispatcher;
import java.util.function.Function;
import com.sun.jna.Callback;
import com.sun.jna.Pointer;
import consulo.eawt.wrapper.ApplicationWrapper;
import consulo.eawt.wrapper.FullScreenUtilitiesWrapper;
import consulo.eawt.wrapper.event.AppFullScreenEventWrapper;
import consulo.eawt.wrapper.event.FullScreenListenerWrapper;
import consulo.logging.Logger;
import consulo.util.concurrent.ActionCallback;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.util.EventListener;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

import static consulo.application.util.mac.foundation.Foundation.invoke;

/**
 * User: spLeaner
 */
public class MacMainFrameDecorator extends IdeFrameDecorator implements UISettingsListener {
  private static final Logger LOG = Logger.getInstance(MacMainFrameDecorator.class);

  private final FullscreenQueue<Runnable> myFullscreenQueue = new FullscreenQueue<>();

  private final EventDispatcher<FSListener> myDispatcher = EventDispatcher.create(FSListener.class);

  private interface FSListener extends EventListener {
    default void windowEnteringFullScreen() {
    }

    default void windowEnteredFullScreen() {
    }

    default void windowExitingFullScreen() {
    }

    default void windowExitedFullScreen() {
    }
  }

  private static class FullscreenQueue<T extends Runnable> {

    private boolean waitingForAppKit = false;
    private LinkedList<Runnable> queueModel = new LinkedList<>();

    synchronized void runOrEnqueue(final T runnable) {
      if (waitingForAppKit) {
        enqueue(runnable);
      }
      else {
        ApplicationManager.getApplication().invokeLater(runnable);
        waitingForAppKit = true;
      }
    }

    synchronized private void enqueue(final T runnable) {
      queueModel.add(runnable);
    }

    synchronized void runFromQueue() {
      if (!queueModel.isEmpty()) {
        queueModel.remove().run();
        waitingForAppKit = true;
      }
      else {
        waitingForAppKit = false;
      }
    }
  }


  // Fullscreen listener delivers event too late,
  // so we use method swizzling here
  private final Callback windowWillEnterFullScreenCallBack = new Callback() {
    public void callback(ID self, ID nsNotification) {
      invoke(self, "oldWindowWillEnterFullScreen:", nsNotification);
      enterFullscreen();
    }
  };

  private void enterFullscreen() {
    myInFullScreen = true;
    myIdeFrame.storeFullScreenStateIfNeeded(true);
    myFullscreenQueue.runFromQueue();
  }

  private final Callback windowWillExitFullScreenCallBack = new Callback() {
    public void callback(ID self, ID nsNotification) {
      invoke(self, "oldWindowWillExitFullScreen:", nsNotification);
      exitFullscreen();
    }
  };

  private void exitFullscreen() {
    myInFullScreen = false;
    myIdeFrame.storeFullScreenStateIfNeeded(false);

    JRootPane rootPane = getJFrame().getRootPane();
    if (rootPane != null) rootPane.putClientProperty(FULL_SCREEN, null);
    myFullscreenQueue.runFromQueue();
  }

  public static final String FULL_SCREEN = "Idea.Is.In.FullScreen.Mode.Now";

  private static boolean SHOWN = false;

  private static Callback SET_VISIBLE_CALLBACK = new Callback() {
    public void callback(ID caller, ID selector, ID value) {
      SHOWN = value.intValue() == 1;
      SwingUtilities.invokeLater(CURRENT_SETTER);
    }
  };

  private static Callback IS_VISIBLE = new Callback() {
    public boolean callback(ID caller) {
      return SHOWN;
    }
  };

  private static AtomicInteger UNIQUE_COUNTER = new AtomicInteger(0);

  public static final Runnable TOOLBAR_SETTER = new Runnable() {
    @Override
    public void run() {
      final UISettings settings = UISettings.getInstance();
      settings.SHOW_MAIN_TOOLBAR = SHOWN;
      settings.fireUISettingsChanged();
    }
  };

  public static final Runnable NAVBAR_SETTER = new Runnable() {
    @Override
    public void run() {
      final UISettings settings = UISettings.getInstance();
      settings.SHOW_NAVIGATION_BAR = SHOWN;
      settings.fireUISettingsChanged();
    }
  };

  public static final Function<Object, Boolean> NAVBAR_GETTER = new Function<>() {
    @Override
    public Boolean apply(Object o) {
      return UISettings.getInstance().SHOW_NAVIGATION_BAR;
    }
  };

  public static final Function<Object, Boolean> TOOLBAR_GETTER = new Function<>() {
    @Override
    public Boolean apply(Object o) {
      return UISettings.getInstance().SHOW_MAIN_TOOLBAR;
    }
  };

  private static Runnable CURRENT_SETTER = null;
  private static Function<Object, Boolean> CURRENT_GETTER = null;
  private static CustomProtocolHandler ourProtocolHandler = null;

  private boolean myInFullScreen;

  public MacMainFrameDecorator(@Nonnull final IdeFrameEx frame, final boolean navBar) {
    super(frame);

    if (CURRENT_SETTER == null) {
      CURRENT_SETTER = navBar ? NAVBAR_SETTER : TOOLBAR_SETTER;
      CURRENT_GETTER = navBar ? NAVBAR_GETTER : TOOLBAR_GETTER;
      SHOWN = CURRENT_GETTER.apply(null);
    }

    UISettings.getInstance().addUISettingsListener(this, this);

    final ID pool = invoke("NSAutoreleasePool", "new");

    //if (ORACLE_BUG_ID_8003173) {
    //  replaceNativeFullscreenListenerCallback();
    //}

    int v = UNIQUE_COUNTER.incrementAndGet();

    JFrame jFrame = getJFrame();
    assert jFrame != null;

    try {
      if (SystemInfo.isMac) {
        FullScreenUtilitiesWrapper.setWindowCanFullScreen(jFrame, true);
        // Native fullscreen listener can be set only once
        FullScreenUtilitiesWrapper.addFullScreenListenerTo(jFrame, new FullScreenListenerWrapper() {
          @Override
          public void windowEnteringFullScreen(AppFullScreenEventWrapper event) {
            myDispatcher.getMulticaster().windowEnteringFullScreen();
          }

          @Override
          public void windowEnteredFullScreen(AppFullScreenEventWrapper event) {
            myDispatcher.getMulticaster().windowEnteredFullScreen();
          }

          @Override
          public void windowExitingFullScreen(AppFullScreenEventWrapper event) {
            myDispatcher.getMulticaster().windowExitingFullScreen();
          }

          @Override
          public void windowExitedFullScreen(AppFullScreenEventWrapper event) {
            myDispatcher.getMulticaster().windowExitedFullScreen();
          }
        });
        myDispatcher.addListener(new FSListener() {
          @Override
          public void windowEnteredFullScreen() {
            JFrame temp = getJFrame();

            // We can get the notification when the frame has been disposed
            JRootPane rootPane = temp.getRootPane();
            if (rootPane != null) rootPane.putClientProperty(FULL_SCREEN, Boolean.TRUE);
            enterFullscreen();
            temp.validate();
          }

          @Override
          public void windowExitedFullScreen() {
            // We can get the notification when the frame has been disposed
            JFrame temp = getJFrame();
            if (temp == null/* || ORACLE_BUG_ID_8003173*/) return;
            exitFullscreen();
            temp.validate();
          }
        });
      }
      else {
        final ID window = MacUtil.findWindowForTitle(jFrame.getTitle());
        if (window == null) return;

        // toggle toolbar
        String className = "IdeaToolbar" + v;
        final ID ownToolbar = Foundation.allocateObjcClassPair(Foundation.getObjcClass("NSToolbar"), className);
        Foundation.registerObjcClassPair(ownToolbar);

        final ID toolbar = invoke(invoke(className, "alloc"), "initWithIdentifier:", Foundation.nsString(className));
        Foundation.cfRetain(toolbar);

        invoke(toolbar, "setVisible:", 0); // hide native toolbar by default

        Foundation.addMethod(ownToolbar, Foundation.createSelector("setVisible:"), SET_VISIBLE_CALLBACK, "v*");
        Foundation.addMethod(ownToolbar, Foundation.createSelector("isVisible"), IS_VISIBLE, "B*");

        Foundation.executeOnMainThread(true, true, new Runnable() {
          @Override
          public void run() {
            invoke(window, "setToolbar:", toolbar);
            invoke(window, "setShowsToolbarButton:", 1);
          }
        });
      }
    }
    finally {
      invoke(pool, "release");
    }

    createProtocolHandler();
  }

  private static void createProtocolHandler() {
    if (ourProtocolHandler == null) {
      // install uri handler
      final ID mainBundle = invoke("NSBundle", "mainBundle");
      final ID urlTypes = invoke(mainBundle, "objectForInfoDictionaryKey:", Foundation.nsString("CFBundleURLTypes"));
      final ApplicationInfo info = ApplicationInfo.getInstance();
      final BuildNumber build = info != null ? info.getBuild() : null;
      if (urlTypes.equals(ID.NIL) && build != null && !build.isSnapshot()) {
        LOG.warn("no url bundle present. \n" +
                 "To use platform protocol handler to open external links specify required protocols in the mac app layout section of the build file\n" +
                 "Example: args.urlSchemes = [\"your-protocol\"] will handle following links: your-protocol://open?file=file&line=line");
        return;
      }
      ourProtocolHandler = new CustomProtocolHandler();

      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_URI)) {
        Desktop.getDesktop().setOpenURIHandler(event -> {
          ourProtocolHandler.openLink(event.getURI());
        });
      }
    }
  }

  private void replaceNativeFullscreenListenerCallback() {
    ID awtWindow = Foundation.getObjcClass("AWTWindow");

    Pointer windowWillEnterFullScreenMethod = Foundation.createSelector("windowWillEnterFullScreen:");
    ID originalWindowWillEnterFullScreen = Foundation.class_replaceMethod(awtWindow, windowWillEnterFullScreenMethod, windowWillEnterFullScreenCallBack, "v@::@");

    Foundation.addMethodByID(awtWindow, Foundation.createSelector("oldWindowWillEnterFullScreen:"), originalWindowWillEnterFullScreen, "v@::@");

    Pointer windowWillExitFullScreenMethod = Foundation.createSelector("windowWillExitFullScreen:");
    ID originalWindowWillExitFullScreen = Foundation.class_replaceMethod(awtWindow, windowWillExitFullScreenMethod, windowWillExitFullScreenCallBack, "v@::@");

    Foundation.addMethodByID(awtWindow, Foundation.createSelector("oldWindowWillExitFullScreen:"), originalWindowWillExitFullScreen, "v@::@");
  }

  @Override
  public void uiSettingsChanged(final UISettings source) {
    if (CURRENT_GETTER != null) {
      SHOWN = CURRENT_GETTER.apply(null);
    }
  }

  @Override
  public boolean isInFullScreen() {
    return myInFullScreen;
  }

  @Nonnull
  @Override
  public ActionCallback toggleFullScreen(final boolean state) {
    JFrame jFrame = getJFrame();
    if (!SystemInfo.isMacOSLion || jFrame == null || myInFullScreen == state) return ActionCallback.REJECTED;
    final ActionCallback callback = new ActionCallback();
    myDispatcher.addListener(new FSListener() {
      @Override
      public void windowExitedFullScreen() {
        callback.setDone();
        myDispatcher.removeListener(this);
      }

      @Override
      public void windowEnteredFullScreen() {
        callback.setDone();
        myDispatcher.removeListener(this);
      }
    });

    myFullscreenQueue.runOrEnqueue(() -> ApplicationWrapper.getApplication().requestToggleFullScreen(getJFrame()));
    return callback;
  }

  public void toggleFullScreenNow() {
    ApplicationWrapper.getApplication().requestToggleFullScreen(getJFrame());
  }
}
