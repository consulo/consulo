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
package consulo.ui;

import consulo.disposer.Disposable;

/**
 * A component which holds enough of its own to be worth taking down deliberately - the models it was built
 * from, whatever a caller hung on it. Everything registered on the hook is held by a hard reference for as
 * long as the component lives, so what is registered there is what has to be let go of when it dies.
 *
 * <p>The hook is the component itself as a {@link Disposable}, so it is used from both sides: a caller ties
 * the component to a scope with {@code Disposer.register(parent, component.destroyHook())}, and anything whose
 * life is the life of the component registers on it. A component nobody ties to a scope lives as long as it is
 * referenced - which is why the hook is offered rather than demanded when the component is created.
 *
 * @author VISTALL
 * @since 2026-08-29
 */
public interface HasDestroyHook {
    Disposable destroyHook();
}
