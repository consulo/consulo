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
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface ContentManager extends Disposable, BusyObject {
    Key<ContentManager> KEY = Key.create(ContentManager.class);

    boolean canCloseContents();

    void addContent(Content content);

    void addContent(Content content, int order);

    void addContent(Content content, Object constraints);

    boolean removeContent(Content content, boolean dispose);

    AsyncResult<Void> removeContent(Content content, boolean dispose, boolean trackFocus, boolean forcedFocus);

    void setSelectedContent(Content content);

    AsyncResult<Void> setSelectedContentCB(Content content);

    void setSelectedContent(Content content, boolean requestFocus);

    AsyncResult<Void> setSelectedContentCB(Content content, boolean requestFocus);

    void setSelectedContent(Content content, boolean requestFocus, boolean forcedFocus);

    AsyncResult<Void> setSelectedContentCB(Content content, boolean requestFocus, boolean forcedFocus);

    AsyncResult<Void> setSelectedContent(Content content, boolean requestFocus, boolean forcedFocus, boolean implicit);

    void addSelectedContent(Content content);

    @Nullable Content getSelectedContent();

    Content[] getSelectedContents();

    void removeAllContents(boolean dispose);

    int getContentCount();

    Content[] getContents();

    //TODO[anton,vova] is this method needed?
    Content findContent(String displayName);

    @Nullable Content getContent(int index);

    int getIndexOfContent(Content content);

    String getCloseActionName();

    boolean canCloseAllContents();

    AsyncResult<Void> selectPreviousContent();

    AsyncResult<Void> selectNextContent();

    void addContentManagerListener(ContentManagerListener l, Disposable disposable);

    @Deprecated
    @DeprecationInfo("Prefer addContentManagerListener with Disposable")
    void addContentManagerListener(ContentManagerListener l);

    @Deprecated
    @DeprecationInfo("Prefer addContentManagerListener with Disposable")
    void removeContentManagerListener(ContentManagerListener l);

    /**
     * Returns the localized name of the "Close All but This" action.
     *
     * @return the action name.
     * @since 5.1
     */
    String getCloseAllButThisActionName();

    String getPreviousContentActionName();

    String getNextContentActionName();

    List<AnAction> getAdditionalPopupActions(Content content);

    void removeFromSelection(Content content);

    boolean isSelected(Content content);

    AsyncResult<Void> requestFocus(@Nullable Content content, boolean forced);

    void addUiDataProvider(UiDataProvider provider);

    default ContentFactory getFactory() {
        return ContentFactory.getInstance();
    }

    boolean isDisposed();

    boolean isSingleSelection();

    default Content getContent(Component component) {
        throw new AbstractMethodError();
    }

    @RequiredUIAccess
    default Component getUIComponent() {
        throw new AbstractMethodError();
    }

    // TODO [VISTALL] awt & swing dependency
    // region awt & swing dependency
    default javax.swing.JComponent getComponent() {
        throw new AbstractMethodError();
    }

    default Content getContent(javax.swing.JComponent component) {
        throw new AbstractMethodError();
    }
    // endregion
}
