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
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;
import consulo.ui.layout.Layout;

import java.util.Collection;
import java.util.List;

/**
 * Dims a layout except for a rounded hole around each of its targets - components inside it which have to
 * stand out, the way the hits of a search do.
 * <p>
 * It is laid over the layout rather than around it: not a layout of its own and not a wrapper, so the layout keeps
 * its place and its children. Nothing is measured by the caller. The holes follow their targets when those move,
 * change size, are scrolled or are hidden, and how dark the dim is, how round a hole is and what outlines it is
 * up to the style.
 * <p>
 * A target which is a {@linkplain Layout layout} stands for a whole section: its hole takes in the
 * frame and the caption of the layout as well. A tab is lit by its caption in the strip of its tabbed layout, see
 * {@link #setTargets(Collection, Collection)}.
 * <p>
 * It lives until it is disposed, so its owner registers it on a disposable of its own.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
public interface Spotlight extends Disposable {
    @RequiredUIAccess
    static Spotlight over(Layout<?> layout) {
        return UIInternal.get()._Spotlight_over(layout);
    }

    Layout<?> getLayout();

    /**
     * The components holes are cut for, in the order they were given.
     */
    List<Component> getTargets();

    /**
     * The tabs whose captions holes are cut for, in the order they were given.
     */
    List<Tab> getTabTargets();

    /**
     * Cuts a hole for each target and dims the rest of the layout. A target which is not inside the layout,
     * or is not shown, gets no hole. No targets at all means no dim.
     */
    @RequiredUIAccess
    default void setTargets(Collection<? extends Component> targets) {
        setTargets(targets, List.of());
    }

    /**
     * Cuts a hole for each target and for the caption of each tab, and dims the rest of the layout. A tab whose
     * tabbed layout is not inside the layout, or is not shown, gets no hole. No targets and no tabs means no dim.
     */
    @RequiredUIAccess
    void setTargets(Collection<? extends Component> targets, Collection<? extends Tab> tabTargets);

    @RequiredUIAccess
    default void clear() {
        setTargets(List.of(), List.of());
    }

    /**
     * Takes the dim away for good. Whatever is asked of the spotlight afterwards is ignored.
     */
    @Override
    void dispose();
}
