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
package consulo.ide.impl.idea.ui;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.util.LowMemoryWatcher;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.language.psi.PsiModificationTrackerListener;
import consulo.project.Project;
import consulo.project.event.ProjectManagerListener;
import consulo.ui.UIAccess;
import consulo.ui.ex.IconDeferrer;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * An icon which is deferred because it is expensive - the run configurations combo answers its error mark by
 * resolving the configuration's main class through the psi.
 * <p/>
 * Deferring means the caller never pays: an action update runs on a background thread without any locks, by
 * contract, so the evaluator must not run there at all. What is handed back is a lazy image - the evaluator runs
 * when the icon is rendered, on the ui thread, which reads for free the way the awt dispatch thread does. The awt
 * deferrer keeps the same shape with a painted placeholder; the stand-in dummy before this one evaluated inline,
 * which was a full psi resolve per toolbar tick inside the lock-free update, and typing waited on every one.
 * <p/>
 * The map keeps one lazy image per key, so a presentation is handed the same object on every pass and nothing
 * downstream sees an icon that changed. The clears below produce a fresh lazy image on the next ask, which is how
 * an answer computed from the psi follows the psi.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedIconDeferrer extends IconDeferrer implements Disposable {
    private static final int CACHE_LIMIT = 1000;

    private final Object myLock = new Object();

    private final Map<Object, Image> myIconsCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Object, Image> eldest) {
            return size() > CACHE_LIMIT;
        }
    };

    @Inject
    public UnifiedIconDeferrer(Application application) {
        // the same invalidations the awt deferrer listens for - an icon computed from the psi holds until the psi
        // moves, a file changes under it, or the project it was asked for goes away
        MessageBusConnection connection = application.getMessageBus().connect();
        connection.subscribe(BulkFileListener.class, new BulkFileListener() {
            @Override
            public void after(List<? extends VFileEvent> events) {
                clear();
            }
        });
        connection.subscribe(PsiModificationTrackerListener.class, this::clear);
        connection.subscribe(ProjectManagerListener.class, new ProjectManagerListener() {
            @Override
            public void projectClosed(Project project, UIAccess uiAccess) {
                clear();
            }
        });
        LowMemoryWatcher.register(this::clear, this);
    }

    private void clear() {
        synchronized (myLock) {
            myIconsCache.clear();
        }
    }

    @Override
    public void dispose() {
        clear();
    }

    @Override
    public <T> Image defer(Image base, T param, Function<T, Image> f) {
        return deferImpl(param, f);
    }

    @Override
    public <T> Image deferAutoUpdatable(Image base, T param, Function<T, Image> f) {
        return deferImpl(param, f);
    }

    private <T> Image deferImpl(T param, Function<T, Image> evaluator) {
        synchronized (myLock) {
            Image cached = myIconsCache.get(param);
            if (cached == null) {
                // nothing expensive happens here - the evaluator is only wrapped, and runs when the icon is drawn
                cached = Image.lazy(() -> evaluator.apply(param));
                myIconsCache.put(param, cached);
            }

            return cached;
        }
    }
}
