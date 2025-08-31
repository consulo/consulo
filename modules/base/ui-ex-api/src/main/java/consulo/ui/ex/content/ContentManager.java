/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.content;

import consulo.annotation.DeprecationInfo;
import consulo.component.util.BusyObject;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

public interface ContentManager extends Disposable, BusyObject {
    Key<ContentManager> KEY = Key.create(ContentManager.class);

    boolean canCloseContents();

    void addContent(@Nonnull Content content);

    void addContent(@Nonnull Content content, int order);

    void addContent(@Nonnull Content content, Object constraints);

    boolean removeContent(@Nonnull Content content, boolean dispose);

    @Nonnull
    AsyncResult<Void> removeContent(@Nonnull Content content, boolean dispose, boolean trackFocus, boolean forcedFocus);

    void setSelectedContent(@Nonnull Content content);

    @Nonnull
    AsyncResult<Void> setSelectedContentCB(@Nonnull Content content);

    void setSelectedContent(@Nonnull Content content, boolean requestFocus);

    @Nonnull
    AsyncResult<Void> setSelectedContentCB(@Nonnull Content content, boolean requestFocus);

    void setSelectedContent(@Nonnull Content content, boolean requestFocus, boolean forcedFocus);

    @Nonnull
    AsyncResult<Void> setSelectedContentCB(@Nonnull Content content, boolean requestFocus, boolean forcedFocus);

    @Nonnull
    AsyncResult<Void> setSelectedContent(@Nonnull Content content, boolean requestFocus, boolean forcedFocus, boolean implicit);

    void addSelectedContent(@Nonnull Content content);

    @Nullable
    Content getSelectedContent();

    @Nonnull
    Content[] getSelectedContents();

    void removeAllContents(boolean dispose);

    int getContentCount();

    @Nonnull
    Content[] getContents();

    //TODO[anton,vova] is this method needed?
    Content findContent(String displayName);

    @Nullable
    Content getContent(int index);

    int getIndexOfContent(Content content);

    @Nonnull
    String getCloseActionName();

    boolean canCloseAllContents();

    AsyncResult<Void> selectPreviousContent();

    AsyncResult<Void> selectNextContent();

    void addContentManagerListener(@Nonnull ContentManagerListener l, @Nonnull Disposable disposable);

    @Deprecated
    @DeprecationInfo("Prefer addContentManagerListener with Disposable")
    void addContentManagerListener(@Nonnull ContentManagerListener l);

    @Deprecated
    @DeprecationInfo("Prefer addContentManagerListener with Disposable")
    void removeContentManagerListener(@Nonnull ContentManagerListener l);

    /**
     * Returns the localized name of the "Close All but This" action.
     *
     * @return the action name.
     * @since 5.1
     */
    @Nonnull
    String getCloseAllButThisActionName();

    @Nonnull
    String getPreviousContentActionName();

    @Nonnull
    String getNextContentActionName();

    List<AnAction> getAdditionalPopupActions(@Nonnull Content content);

    void removeFromSelection(@Nonnull Content content);

    boolean isSelected(@Nonnull Content content);

    @Nonnull
    AsyncResult<Void> requestFocus(@Nullable Content content, boolean forced);

    void addDataProvider(@Nonnull DataProvider provider);

    @Nonnull
    default ContentFactory getFactory() {
        return ContentFactory.getInstance();
    }

    boolean isDisposed();

    boolean isSingleSelection();

    default Content getContent(@Nonnull Component component) {
        throw new AbstractMethodError();
    }

    @Nonnull
    @RequiredUIAccess
    default Component getUIComponent() {
        throw new AbstractMethodError();
    }

    // TODO [VISTALL] awt & swing dependency
    // region awt & swing dependency
    @Nonnull
    default javax.swing.JComponent getComponent() {
        throw new AbstractMethodError();
    }

    default Content getContent(javax.swing.JComponent component) {
        throw new AbstractMethodError();
    }
    // endregion
}
