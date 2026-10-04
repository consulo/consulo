// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ui.grid;

import consulo.disposer.Disposable;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.dataholder.UserDataHolder;

/**
 * @author gregsh
 */
public interface GridDataRequest extends UserDataHolder {
    AsyncPromise<Void> getPromise();

    interface Context {
    }

    //todo: marker interface to reduce module dependency
    //the only usage I consider invalid
    interface DatabaseContext extends Context {
    }

    interface GridDataRequestOwner extends Disposable {
        String getDisplayName();
    }
}
