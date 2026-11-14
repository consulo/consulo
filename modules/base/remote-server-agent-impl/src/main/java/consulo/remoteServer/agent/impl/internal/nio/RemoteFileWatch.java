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
package consulo.remoteServer.agent.impl.internal.nio;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.logging.Logger;
import consulo.platform.PlatformFileWatch;
import consulo.platform.PlatformFileWatchListener;
import consulo.remoteServer.agent.impl.internal.RemotePermissionException;
import consulo.remoteServer.agent.impl.internal.protocol.WatchChange;
import consulo.remoteServer.agent.impl.internal.protocol.WatchEvent;
import consulo.remoteServer.agent.impl.internal.protocol.WatchInput;
import consulo.remoteServer.agent.impl.internal.protocol.WatchRoot;
import io.grpc.stub.StreamObserver;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Changes of files of the agent's machine - its Watch stream. A lost stream is opened again; the roots are reset then, as the
 * changes in between are not known.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
final class RemoteFileWatch implements PlatformFileWatch {
    private static final Logger LOG = Logger.getInstance(RemoteFileWatch.class);

    private static final long REOPEN_DELAY_SECONDS = 5;

    private final RemoteNioFileSystem myFileSystem;
    private final PlatformFileWatchListener myListener;
    private final Consumer<RemoteFileWatch> myOnClose;
    private final Object myLock = new Object();

    private List<Path> myRecursiveRoots = List.of();
    private List<Path> myFlatRoots = List.of();
    private @Nullable StreamObserver<WatchInput> myRequests;
    // the receiver of the current stream - the end of an older stream (of a lost connection) changes nothing
    private @Nullable Events myEvents;
    private boolean myClosed;

    RemoteFileWatch(RemoteNioFileSystem fileSystem, PlatformFileWatchListener listener, Consumer<RemoteFileWatch> onClose) {
        myFileSystem = fileSystem;
        myListener = listener;
        myOnClose = onClose;
    }

    @Override
    public void setRoots(Collection<Path> recursiveRoots, Collection<Path> flatRoots) {
        synchronized (myLock) {
            if (myClosed) {
                return;
            }
            myRecursiveRoots = List.copyOf(recursiveRoots);
            myFlatRoots = List.copyOf(flatRoots);
            if (myRequests == null) {
                open();
            }
            else {
                sendRoots();
            }
        }
    }

    private void open() {
        Events events = new Events();
        myEvents = events;
        myRequests = myFileSystem.getConnection().watch(events);
        sendRoots();
    }

    /**
     * The file system has a new connection - the stream of the old one is dropped, a stream of the new one is opened and
     * the roots are refreshed, their changes in between are not known.
     */
    void reconnected() {
        StreamObserver<WatchInput> old;
        synchronized (myLock) {
            if (myClosed) {
                return;
            }
            old = myRequests;
            myRequests = null;
            myEvents = null;
        }
        if (old != null) {
            try {
                old.onCompleted();
            }
            catch (RuntimeException e) {
                LOG.debug(e);
            }
        }
        reopen();
    }

    private void sendRoots() {
        StreamObserver<WatchInput> requests = myRequests;
        if (requests == null) {
            return;
        }
        WatchInput.Builder input = WatchInput.newBuilder();
        for (Path root : myRecursiveRoots) {
            input.addRoots(WatchRoot.newBuilder().setPath(root.toString()).setRecursive(true));
        }
        for (Path root : myFlatRoots) {
            input.addRoots(WatchRoot.newBuilder().setPath(root.toString()).setRecursive(false));
        }
        requests.onNext(input.build());
    }

    private void reopen() {
        List<Path> roots = new ArrayList<>();
        synchronized (myLock) {
            if (myClosed || myRequests != null || !myFileSystem.isOpen()) {
                return;
            }
            try {
                open();
            }
            catch (RuntimeException e) {
                LOG.debug(e);
                scheduleReopen();
                return;
            }
            roots.addAll(myRecursiveRoots);
            roots.addAll(myFlatRoots);
        }
        // what changed while the stream was lost is not known
        myFileSystem.getMetadataCache().invalidateAll();
        for (Path root : roots) {
            myListener.reset(root);
        }
    }

    private void scheduleReopen() {
        AppExecutorUtil.getAppScheduledExecutorService().schedule(this::reopen, REOPEN_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        synchronized (myLock) {
            myClosed = true;
            StreamObserver<WatchInput> requests = myRequests;
            myRequests = null;
            myEvents = null;
            if (requests != null) {
                requests.onCompleted();
            }
        }
        myOnClose.accept(this);
    }

    private void onEvent(WatchEvent event) {
        switch (event.getEventCase()) {
            case CHANGES -> {
                RemoteFileMetadataCache cache = myFileSystem.getMetadataCache();
                for (WatchChange change : event.getChanges().getChangesList()) {
                    cache.invalidate(change.getPath());
                    Path path = myFileSystem.getPath(change.getPath());
                    if (change.getKind() == WatchChange.Kind.CHANGED) {
                        myListener.changed(path);
                    }
                    else {
                        myListener.createdOrDeleted(path);
                    }
                }
            }
            case OVERFLOW -> {
                myFileSystem.getMetadataCache().invalidateAll();
                myListener.reset(myFileSystem.getPath(event.getOverflow()));
            }
            case FAILED -> myListener.failed(myFileSystem.getPath(event.getFailed().getPath()), event.getFailed().getMessage());
            case EVENT_NOT_SET -> {
            }
        }
    }

    private void onEnd(Events source, @Nullable Throwable error) {
        synchronized (myLock) {
            if (source != myEvents) {
                return;
            }
            myRequests = null;
            myEvents = null;
            if (myClosed) {
                return;
            }
        }
        if (error != null && myFileSystem.getConnection().toException(error) instanceof RemotePermissionException permission) {
            // the agent does not allow watching - its files are refreshed by the user only
            LOG.info("Watching files is not permitted: " + permission.getMessage());
            return;
        }
        LOG.debug("Watch stream ended", error);
        scheduleReopen();
    }

    private class Events implements StreamObserver<WatchEvent> {
        @Override
        public void onNext(WatchEvent event) {
            try {
                onEvent(event);
            }
            catch (RuntimeException e) {
                LOG.warn(e);
            }
        }

        @Override
        public void onError(Throwable error) {
            onEnd(this, error);
        }

        @Override
        public void onCompleted() {
            onEnd(this, null);
        }
    }
}
