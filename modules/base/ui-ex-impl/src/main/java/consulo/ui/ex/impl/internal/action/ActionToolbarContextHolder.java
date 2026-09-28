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
package consulo.ui.ex.impl.internal.action;

import consulo.dataContext.DataContext;
import consulo.logging.Logger;
import consulo.ui.ex.action.ActionToolbarContexts;

import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-09-29
 */
public final class ActionToolbarContextHolder {
    private static final Logger LOG = Logger.getInstance(ActionToolbarContextHolder.class);

    private final Throwable myCreationTrace = new Throwable("toolbar creation trace");

    private final String myPlace;

    private Supplier<DataContext> myProvider = ActionToolbarContexts.empty();

    private boolean myProviderSet;

    private boolean myWarned;

    public ActionToolbarContextHolder(String place) {
        myPlace = place;
    }

    public boolean setProvider(Supplier<DataContext> provider) {
        myProviderSet = true;

        if (myProvider == provider) {
            return false;
        }

        myProvider = provider;
        return true;
    }

    public DataContext getDataContext() {
        if (!myProviderSet && !myWarned) {
            myWarned = true;

            LOG.warn(
                "'" + myPlace + "' toolbar has no data context provider, its actions are updated against an empty context. " +
                    "Please call toolbar.setDataContextProvider() or toolbar.setTargetComponent() explicitly.",
                myCreationTrace
            );
        }

        return myProvider.get();
    }
}
