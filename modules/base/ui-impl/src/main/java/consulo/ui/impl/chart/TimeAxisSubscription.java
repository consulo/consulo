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
package consulo.ui.impl.chart;

import consulo.ui.impl.chart.model.updater.Updatable;
import consulo.ui.impl.chart.model.updater.Updater;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class TimeAxisSubscription {
    private final TimeAxisImpl myAxis;
    private final List<Updatable> myUpdatables;

    private boolean myActive;

    public TimeAxisSubscription(TimeAxisImpl axis, List<Updatable> updatables) {
        myAxis = axis;
        myUpdatables = List.copyOf(updatables);
    }

    public boolean isActive() {
        return myActive;
    }

    public void activate() {
        if (myActive) {
            return;
        }
        myActive = true;
        myAxis.retain();
        Updater updater = myAxis.getUpdater();
        for (Updatable updatable : myUpdatables) {
            updater.register(updatable);
        }
    }

    public void deactivate() {
        if (!myActive) {
            return;
        }
        myActive = false;
        Updater updater = myAxis.getUpdater();
        for (Updatable updatable : myUpdatables) {
            updater.unregister(updatable);
        }
        myAxis.release();
    }
}
