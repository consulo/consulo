// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.clipboard;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ui.UISettings;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.ide.impl.idea.openapi.editor.CaretStateTransferableData;
import consulo.ide.impl.idea.openapi.ide.CutElementMarker;
import consulo.util.lang.Comparing;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.ui.ex.awt.CopyPasteManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jspecify.annotations.Nullable;

import java.awt.datatransfer.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Singleton
@ServiceImpl
public class DesktopAWTCopyPasteManagerImpl extends CopyPasteManager implements ClipboardOwner {
    private final List<Transferable> myData = new ArrayList<>();
    private final EventDispatcher<ContentChangedListener> myDispatcher = EventDispatcher.create(ContentChangedListener.class);
    private final ClipboardSynchronizer myClipboardSynchronizer;
    private boolean myOwnContent;

    @Inject
    public DesktopAWTCopyPasteManagerImpl(ClipboardSynchronizer clipboardSynchronizer) {
        myClipboardSynchronizer = clipboardSynchronizer;
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        myOwnContent = false;
        myClipboardSynchronizer.resetContent();
        fireContentChanged(contents, null);
    }

    private void fireContentChanged(@Nullable Transferable oldContent, @Nullable Transferable newContent) {
        myDispatcher.getMulticaster().contentChanged(oldContent, newContent);
    }

    @Override
    public void addContentChangedListener(ContentChangedListener listener) {
        myDispatcher.addListener(listener);
    }

    @Override
    public void addContentChangedListener(ContentChangedListener listener, Disposable parentDisposable) {
        myDispatcher.addListener(listener, parentDisposable);
    }

    @Override
    public void removeContentChangedListener(ContentChangedListener listener) {
        myDispatcher.removeListener(listener);
    }

    @Override
    public boolean areDataFlavorsAvailable(DataFlavor... flavors) {
        return flavors.length > 0 && myClipboardSynchronizer.areDataFlavorsAvailable(flavors);
    }

    @Override
    public void setContents(Transferable content) {
        Transferable oldContent = myOwnContent && !myData.isEmpty() ? myData.get(0) : null;

        Transferable contentToUse = addNewContentToStack(content);
        setSystemClipboardContent(contentToUse);

        fireContentChanged(oldContent, contentToUse);
    }

    @Override
    public boolean isCutElement(@Nullable Object element) {
        for (CutElementMarker marker : CutElementMarker.EP_NAME.getExtensions()) {
            if (marker.isCutElement(element)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void stopKillRings() {
    }

    private void setSystemClipboardContent(Transferable content) {
        myClipboardSynchronizer.setContent(content, this);
        myOwnContent = true;
    }

    /**
     * Stores given content within the current manager. It is merged with already stored ones
     *
     * @param content content to store
     * @return content that is either the given one or the one that was assembled from it and already stored one
     */
    private Transferable addNewContentToStack(Transferable content) {
        String clipString = getStringContent(content);
        if (clipString == null) {
            return content;
        }

        CaretStateTransferableData caretData = CaretStateTransferableData.getFrom(content);
        for (int i = 0; i < myData.size(); i++) {
            Transferable old = myData.get(i);
            if (clipString.equals(getStringContent(old))
                && CaretStateTransferableData.areEquivalent(caretData, CaretStateTransferableData.getFrom(old))) {
                myData.remove(i);
                myData.add(0, content);
                return content;
            }
        }

        addToTheTopOfTheStack(content);
        return content;
    }

    private void addToTheTopOfTheStack(Transferable content) {
        myData.add(0, content);
        deleteAfterAllowedMaximum();
    }

    private static String getStringContent(Transferable content) {
        try {
            return (String)content.getTransferData(DataFlavor.stringFlavor);
        }
        catch (UnsupportedFlavorException | IOException ignore) {
        }
        return null;
    }

    private void deleteAfterAllowedMaximum() {
        int max = UISettings.getInstance().MAX_CLIPBOARD_CONTENTS;
        for (int i = myData.size() - 1; i >= max; i--) {
            myData.remove(i);
        }
    }

    @Override
    public Transferable getContents() {
        return myClipboardSynchronizer.getContents();
    }

    @Override
    public <T> @Nullable T getContents(DataFlavor flavor) {
        if (areDataFlavorsAvailable(flavor)) {
            //noinspection unchecked
            return (T)myClipboardSynchronizer.getData(flavor);
        }
        return null;
    }

    
    @Override
    public Transferable[] getAllContents() {
        String clipString = getContents(DataFlavor.stringFlavor);
        if (clipString != null && (myData.isEmpty() || !Comparing.equal(clipString, getStringContent(myData.get(0))))) {
            addToTheTopOfTheStack(new StringSelection(clipString));
        }
        return myData.toArray(new Transferable[0]);
    }

    public void removeContent(Transferable t) {
        Transferable current = myData.isEmpty() ? null : myData.get(0);
        myData.remove(t);
        if (Comparing.equal(t, current)) {
            Transferable newContent = !myData.isEmpty() ? myData.get(0) : new StringSelection("");
            setSystemClipboardContent(newContent);
            fireContentChanged(current, newContent);
        }
    }

    public void moveContentToStackTop(Transferable t) {
        Transferable current = myData.isEmpty() ? null : myData.get(0);
        if (!Comparing.equal(t, current)) {
            myData.remove(t);
            myData.add(0, t);
            setSystemClipboardContent(t);
            fireContentChanged(current, t);
        }
        else {
            setSystemClipboardContent(t);
        }
    }
}