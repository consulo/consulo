/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.desktop.awt.ui.dnd;

import consulo.ide.impl.idea.ide.dnd.FileCopyPasteUtil;
import consulo.ide.impl.idea.ide.dnd.FileFlavorProvider;
import consulo.ide.impl.idea.ide.dnd.LinuxDragAndDropSupport;
import consulo.logging.Logger;
import consulo.ui.ex.awt.dnd.*;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.RelativeRectangle;
import consulo.ide.impl.idea.util.ArrayUtil;

import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class DnDEventImpl extends UserDataHolderBase implements Transferable, DnDEvent {
  private static final Logger LOG = Logger.getInstance(DnDEventImpl.class);

  public static final DataFlavor ourDataFlavor = FileCopyPasteUtil.createDataFlavor(DataFlavor.javaJVMLocalObjectMimeType);

  private DnDTarget myDelegatedTarget;
  private DnDManagerImpl myManager;
  private DnDAction myAction;
  private Object myAttachedObject;
  private boolean myDropPossible;
  private String myExpectedDropResult;
  private Point myPoint;
  private Point myOrgPoint;

  private int myHighlighting;

  private DropActionHandler myDropHandler;
  private Component myHandlerComponent;
  private boolean myShouldRemoveHighlighter = true;
  private Point myLocalPoint;
  private Cursor myCursor;

  public DnDEventImpl(DnDManagerImpl manager, DnDAction action, Object attachedObject, Point point) {
    myManager = manager;
    myAction = action;
    myAttachedObject = attachedObject;
    myPoint = point;
  }

  @Override
  public DnDAction getAction() {
    return myAction;
  }

  @Override
  public void updateAction(DnDAction action) {
    myAction = action;
  }

  @Override
  public Object getAttachedObject() {
    return myAttachedObject;
  }

  @Override
  public void setDropPossible(boolean possible, @Nullable String aExpectedResult) {
    myDropPossible = possible;
    myExpectedDropResult = aExpectedResult;
    clearDropHandler();
  }

  @Override
  public void setDropPossible(boolean possible) {
    setDropPossible(possible, null);
  }

  @Override
  public void setDropPossible(String aExpectedResult, DropActionHandler aHandler) {
    myDropPossible = true;
    myExpectedDropResult = aExpectedResult;
    myDropHandler = aHandler;
  }

  @Override
  public String getExpectedDropResult() {
    return myExpectedDropResult;
  }

  @Override
  public DataFlavor[] getTransferDataFlavors() {
    if (myAttachedObject instanceof Transferable) {
      return ((Transferable)myAttachedObject).getTransferDataFlavors();
    }
    else if (myAttachedObject instanceof FileFlavorProvider) {
      return new DataFlavor[]{ourDataFlavor, DataFlavor.javaFileListFlavor, LinuxDragAndDropSupport.uriListFlavor};
    }
    else if (myAttachedObject instanceof DnDNativeTarget.EventInfo) {
      return ((DnDNativeTarget.EventInfo)myAttachedObject).getFlavors();
    }
    return new DataFlavor[]{ourDataFlavor};
  }

  @Override
  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
    if (myAttachedObject instanceof Transferable) {
      return ((Transferable)myAttachedObject).getTransferData(flavor);
    }
    else if (myAttachedObject instanceof FileFlavorProvider && flavor == DataFlavor.javaFileListFlavor) {
      final List<File> files = ((FileFlavorProvider)myAttachedObject).asFileList();
      if (files != null) {
        return files;
      }
    }
    else if (myAttachedObject instanceof FileFlavorProvider && flavor == LinuxDragAndDropSupport.uriListFlavor) {
      final List<File> files = ((FileFlavorProvider)myAttachedObject).asFileList();
      if (files != null) {
        return LinuxDragAndDropSupport.toUriList(files);
      }
    }

    return getAttachedObject();
  }

  @Override
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    DataFlavor[] flavors = getTransferDataFlavors();
    return ArrayUtil.find(flavors, flavor) != -1;
  }

  @Override
  public boolean isDropPossible() {
    return myDropPossible;
  }

  @Override
  public Point getOrgPoint() {
    return myOrgPoint;
  }

  @Override
  public void setOrgPoint(Point orgPoint) {
    myOrgPoint = orgPoint;
  }

  void setPoint(Point aPoint) {
    myPoint = aPoint;
  }

  @Override
  public Point getPoint() {
    // TODO: it is better to return a new point every time
    return myPoint;
  }

  @Override
  public Point getPointOn(Component aComponent) {
    return SwingUtilities.convertPoint(myHandlerComponent, getPoint(), aComponent);
  }

  void clearDropHandler() {
    myDropHandler = null;
  }

  @Override
  public boolean canHandleDrop() {
    LOG.debug("canHandleDrop:" + myDropHandler);
    return myDropHandler != null;
  }

  protected void handleDrop() {
    myDropHandler.performDrop(this);
  }

  void setHandlerComponent(Component aOverComponent) {
    myHandlerComponent = aOverComponent;
  }

  @Override
  public Component getHandlerComponent() {
    return myHandlerComponent;
  }

  @Override
  public Component getCurrentOverComponent() {
    return getHandlerComponent().getComponentAt(getPoint());
  }

  @Override
  public void setHighlighting(Component aComponent, int aType) {
    myManager.showHighlighter(aComponent, aType, this);
    myHighlighting = aType;
  }

  @Override
  public void setHighlighting(RelativeRectangle rectangle, int aType) {
    getHandlerComponent();
    myManager.showHighlighter(rectangle, aType, this);
    myHighlighting = aType;
  }

  @Override
  public void setHighlighting(JLayeredPane layeredPane, RelativeRectangle rectangle, int aType) {
    myManager.showHighlighter(layeredPane, rectangle, aType, this);
  }

  boolean shouldRemoveHighlighting() {
    return myShouldRemoveHighlighter;
  }

  @Override
  public void setAutoHideHighlighterInDrop(boolean aValue) {
    myShouldRemoveHighlighter = aValue;
  }

  @Override
  public void hideHighlighter() {
    myManager.hideCurrentHighlighter();
    myHighlighting = 0;
  }

  @Override
  public void setLocalPoint(Point localPoint) {
    myLocalPoint = localPoint;
  }

  /**
   * Returns point relative to dnd target's origin
   */
  @Override
  public Point getLocalPoint() {
    return myLocalPoint;
  }

  @Override
  public RelativePoint getRelativePoint() {
    return new RelativePoint(getCurrentOverComponent(), getPoint());
  }

  @Override
  public void clearDelegatedTarget() {
    myDelegatedTarget = null;
  }

  @Override
  public boolean wasDelegated() {
    return myDelegatedTarget != null;
  }

  @Override
  public DnDTarget getDelegatedTarget() {
    return myDelegatedTarget;
  }

  @Override
  public boolean delegateUpdateTo(DnDTarget target) {
    myDelegatedTarget = target;
    return myDelegatedTarget.update(this);
  }

  @Override
  public void delegateDropTo(DnDTarget target) {
    myDelegatedTarget = target;
    target.drop(this);
  }

  @Override
  protected Object clone() {
    final DnDEventImpl result = new DnDEventImpl(myManager, myAction, myAttachedObject, myPoint);
    result.myDropHandler = myDropHandler;
    result.myDropPossible = myDropPossible;
    result.myExpectedDropResult = myExpectedDropResult;
    result.myHighlighting = myHighlighting;
    return result;
  }

  public boolean equals(Object o) {
    if( this == o ) {
      return true;
    }
    if( !(o instanceof DnDEventImpl) ) {
      return false;
    }

    final DnDEventImpl event = (DnDEventImpl) o;

    if( myDropPossible != event.myDropPossible ) {
      return false;
    }
    if( myHighlighting != event.myHighlighting ) {
      return false;
    }
    if( myAttachedObject != null? !myAttachedObject.equals(event.myAttachedObject): event.myAttachedObject != null ) {
      return false;
    }
    if( myExpectedDropResult != null? !myExpectedDropResult.equals(event.myExpectedDropResult): event.myExpectedDropResult != null ) {
      return false;
    }

    return true;
  }

  public int hashCode() {
    int result;
    result = (myAttachedObject != null ? myAttachedObject.hashCode() : 0);
    result = 29 * result + (myDropPossible ? 1 : 0);
    result = 29 * result + (myExpectedDropResult != null ? myExpectedDropResult.hashCode() : 0);
    result = 29 * result + myHighlighting;
    return result;
  }

  @Override
  public Cursor getCursor() {
    return myCursor;
  }

  public String toString() {
    return "DnDEvent[attachedObject: " + myAttachedObject + ", delegatedTarget: " + myDelegatedTarget + ", dropHandler: " + myDropHandler + "]";
  }

  @Override
  public void setCursor(Cursor cursor) {
    myCursor = cursor;
  }

  @Override
  public void cleanUp() {
    myAttachedObject = null;
    myDelegatedTarget = null;
    myDropHandler = null;
    myHandlerComponent = null;
    myManager = null;
  }
}
