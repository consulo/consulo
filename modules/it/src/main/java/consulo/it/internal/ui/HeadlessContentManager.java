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
package consulo.it.internal.ui;

import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.ContentUI;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.util.concurrent.AsyncResult;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Headless {@link ContentManager}: a plain list of {@link Content} with trivial selection tracking and
 * no events/UI. Enough for {@code UnifiedToolWindowImpl} and tool-window content bookkeeping in tests.
 *
 * @author VISTALL
 */
public class HeadlessContentManager implements ContentManager {
    private final @Nullable ContentUI myContentUI;
    private final boolean myCanCloseContents;

    private final List<Content> myContents = new CopyOnWriteArrayList<>();
    private final List<ContentManagerListener> myListeners = new CopyOnWriteArrayList<>();

    private @Nullable Content mySelected;
    private boolean myDisposed;

    public HeadlessContentManager(@Nullable ContentUI contentUI, boolean canCloseContents) {
        myContentUI = contentUI;
        myCanCloseContents = canCloseContents;
    }

    @Override
    public boolean canCloseContents() {
        return myCanCloseContents;
    }

    @Override
    public void addContent(Content content) {
        myContents.add(content);
        if (content instanceof HeadlessContent headlessContent) {
            headlessContent.setManager(this);
        }
    }

    @Override
    public void addContent(Content content, int order) {
        addContent(content);
    }

    @Override
    public void addContent(Content content, Object constraints) {
        addContent(content);
    }

    @Override
    public boolean removeContent(Content content, boolean dispose) {
        boolean removed = myContents.remove(content);
        if (mySelected == content) {
            mySelected = null;
        }
        if (removed && dispose) {
            Disposer.dispose(content);
        }
        return removed;
    }

    @Override
    public AsyncResult<Void> removeContent(Content content, boolean dispose, boolean trackFocus, boolean forcedFocus) {
        removeContent(content, dispose);
        return AsyncResult.resolved(null);
    }

    @Override
    public void setSelectedContent(Content content) {
        mySelected = content;
    }

    @Override
    public AsyncResult<Void> setSelectedContentCB(Content content) {
        mySelected = content;
        return AsyncResult.resolved(null);
    }

    @Override
    public void setSelectedContent(Content content, boolean requestFocus) {
        mySelected = content;
    }

    @Override
    public AsyncResult<Void> setSelectedContentCB(Content content, boolean requestFocus) {
        return setSelectedContentCB(content);
    }

    @Override
    public void setSelectedContent(Content content, boolean requestFocus, boolean forcedFocus) {
        mySelected = content;
    }

    @Override
    public AsyncResult<Void> setSelectedContentCB(Content content, boolean requestFocus, boolean forcedFocus) {
        return setSelectedContentCB(content);
    }

    @Override
    public AsyncResult<Void> setSelectedContent(Content content, boolean requestFocus, boolean forcedFocus, boolean implicit) {
        return setSelectedContentCB(content);
    }

    @Override
    public void addSelectedContent(Content content) {
        mySelected = content;
    }

    @Override
    public @Nullable Content getSelectedContent() {
        return mySelected;
    }

    @Override
    public Content[] getSelectedContents() {
        Content selected = mySelected;
        return selected == null ? new Content[0] : new Content[]{selected};
    }

    @Override
    public void removeAllContents(boolean dispose) {
        for (Content content : new ArrayList<>(myContents)) {
            removeContent(content, dispose);
        }
    }

    @Override
    public int getContentCount() {
        return myContents.size();
    }

    @Override
    public Content[] getContents() {
        return myContents.toArray(new Content[0]);
    }

    @Override
    public @Nullable Content findContent(String displayName) {
        for (Content content : myContents) {
            if (displayName.equals(content.getDisplayName())) {
                return content;
            }
        }
        return null;
    }

    @Override
    public @Nullable Content getContent(int index) {
        return index >= 0 && index < myContents.size() ? myContents.get(index) : null;
    }

    @Override
    public @Nullable Content getContent(Component component) {
        for (Content content : myContents) {
            if (content.getUIComponent() == component) {
                return content;
            }
        }
        return null;
    }

    @Override
    public int getIndexOfContent(Content content) {
        return myContents.indexOf(content);
    }

    @Override
    public String getCloseActionName() {
        return "";
    }

    @Override
    public boolean canCloseAllContents() {
        return false;
    }

    @Override
    public AsyncResult<Void> selectPreviousContent() {
        return AsyncResult.resolved(null);
    }

    @Override
    public AsyncResult<Void> selectNextContent() {
        return AsyncResult.resolved(null);
    }

    @Override
    public void addContentManagerListener(ContentManagerListener l, Disposable disposable) {
        myListeners.add(l);
        Disposer.register(disposable, () -> myListeners.remove(l));
    }

    @Override
    public void addContentManagerListener(ContentManagerListener l) {
        myListeners.add(l);
    }

    @Override
    public void removeContentManagerListener(ContentManagerListener l) {
        myListeners.remove(l);
    }

    @Override
    public String getCloseAllButThisActionName() {
        return "";
    }

    @Override
    public String getPreviousContentActionName() {
        return "";
    }

    @Override
    public String getNextContentActionName() {
        return "";
    }

    @Override
    public List<AnAction> getAdditionalPopupActions(Content content) {
        return List.of();
    }

    @Override
    public void removeFromSelection(Content content) {
        if (mySelected == content) {
            mySelected = null;
        }
    }

    @Override
    public boolean isSelected(Content content) {
        return mySelected == content;
    }

    @Override
    public AsyncResult<Void> requestFocus(@Nullable Content content, boolean forced) {
        return AsyncResult.resolved(null);
    }

    @Override
    public void addUiDataProvider(UiDataProvider provider) {
    }

    @Override
    public boolean isDisposed() {
        return myDisposed;
    }

    @Override
    public boolean isSingleSelection() {
        return true;
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component getUIComponent() {
        return myContentUI == null ? null : myContentUI.getUIComponent();
    }

    @Override
    public AsyncResult<Void> getReady(Object requestor) {
        return AsyncResult.resolved(null);
    }

    @Override
    public void dispose() {
        myDisposed = true;
        removeAllContents(true);
        myListeners.clear();
    }
}
