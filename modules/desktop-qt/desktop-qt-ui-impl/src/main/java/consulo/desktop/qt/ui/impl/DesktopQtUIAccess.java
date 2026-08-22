/*
 * Copyright 2013-2026 consulo.io
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
package consulo.desktop.qt.ui.impl;

import consulo.application.Application;
import consulo.application.ApplicationProperties;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.desktop.qt.ui.impl.clipboard.DesktopQtClipboardImpl;
import consulo.desktop.qt.ui.impl.font.DesktopQtFontRegistry;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.clipboard.Clipboard;
import consulo.ui.impl.BaseUIAccess;
import consulo.ui.impl.SingleUIAccessScheduler;
import io.qt.core.QCoreApplication;
import io.qt.core.QMetaObject;
import io.qt.core.QObject;
import io.qt.core.Qt;
import io.qt.gui.QGuiApplication;
import io.qt.widgets.QApplication;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtUIAccess extends BaseUIAccess implements UIAccess {
    public static final DesktopQtUIAccess INSTANCE = new DesktopQtUIAccess();

    private static final Logger LOG = Logger.getInstance(DesktopQtUIAccess.class);

    private static final String ourApplicationId = "consulo";
    private static final String ourSandboxApplicationId = "consulo-in-sandbox-qt";

    private QObject myContext;
    private Thread myThread;

    public DesktopQtUIAccess() {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        // this constructor runs from the class initializer of DesktopQtUIAccess, so the thread started here
        // must not touch a static member of this class - it would block on the initialization monitor while
        // the initializing thread waits on the latch below, and neither would ever move again
        String applicationId = ApplicationProperties.isInSandbox() ? ourSandboxApplicationId : ourApplicationId;
        Logger logger = Logger.getInstance(DesktopQtUIAccess.class);

        Thread thread = new Thread("Qt Event Queue") {
            @Override
            public void run() {
                DesktopQtNativePaths.applyBundledPluginPath();

                QApplication.initialize(new String[0]);
                QApplication.setQuitOnLastWindowClosed(false);

                // wayland reads the application id off a surface when the surface is created and never asks
                // again, and only setDesktopFileName decides it - the application and organization names leave
                // it as "java", the bucket every other jvm on the session shares. The sandbox is named apart
                // from the other frontends because getFrameClass() answers "consulo-sandbox" for all of them,
                // so the awt and the qt sandbox were handed each other's stored window geometry
                QGuiApplication.setDesktopFileName(applicationId);

                myContext = QCoreApplication.instance();

                // before anything measures a font, since the metrics of an editor are cached against whatever
                // the family resolved to the first time it was asked for
                DesktopQtFontRegistry.registerBundledFonts();

                DesktopQtStyleManagerImpl.INSTANCE.syncWithPlatform();
                QGuiApplication.styleHints().colorSchemeChanged.connect(
                    scheme -> DesktopQtStyleManagerImpl.INSTANCE.syncWithPlatform()
                );

                DesktopQtCurrentInput.install();

                countDownLatch.countDown();

                try {
                    QApplication.exec();
                }
                catch (Throwable e) {
                    logger.error(e);
                }
            }
        };
        thread.setDaemon(true);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();

        myThread = thread;

        try {
            countDownLatch.await();
        }
        catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public QObject getContext() {
        return myContext;
    }

    public boolean isUIThread() {
        return Thread.currentThread() == myThread;
    }

    @Override
    public <T> CompletableFuture<T> giveAsync(Supplier<T> supplier) {
        CompletableFuture<T> result = new CompletableFuture<>();
        give(() -> {
            try {
                result.complete(supplier.get());
            }
            catch (Throwable e) {
                LOG.error(e);
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    @Override
    public void give(Runnable runnable) {
        QMetaObject.invokeMethod(myContext, () -> {
            try {
                runnable.run();
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }, Qt.ConnectionType.QueuedConnection);
    }

    @Override
    public void giveAndWait(Runnable runnable) {
        if (isUIThread()) {
            runnable.run();
            return;
        }

        QMetaObject.invokeMethod(myContext, () -> {
            try {
                runnable.run();
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }, Qt.ConnectionType.BlockingQueuedConnection);
    }

    @Override
    protected Clipboard createClipboard() {
        return new DesktopQtClipboardImpl();
    }

    @Override
    protected SingleUIAccessScheduler createScheduler() {
        Application application = Application.get();
        ApplicationConcurrency concurrency = application.getInstance(ApplicationConcurrency.class);
        return new SingleUIAccessScheduler(this, concurrency.getScheduledExecutorService()) {
            @Override
            public void runWithModalityState(Runnable runnable, ModalityState modalityState) {
                Application.get().invokeLater(runnable, modalityState);
            }
        };
    }
}
