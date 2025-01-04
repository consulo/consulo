/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package consulo.desktop.awt.editor.impl;

import consulo.application.Application;
import consulo.application.ui.RemoteDesktopService;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.event.VisibleAreaEvent;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.codeEditor.impl.CodeEditorScrollingModelBase;
import consulo.codeEditor.impl.VisibleEditorsTracker;
import consulo.disposer.Disposer;
import consulo.document.event.DocumentAdapter;
import consulo.document.event.DocumentEvent;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.openapi.fileEditor.impl.text.AsyncEditorLoader;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Animator;
import consulo.ui.ex.awt.Interpolable;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.SystemProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ArrayList;

public class DesktopScrollingModelImpl extends CodeEditorScrollingModelBase {
    private AnimatedScrollingRunnable myCurrentAnimationRequest = null;
    private boolean myAnimationDisabled = false;

    private int myAccumulatedXOffset = -1;
    private int myAccumulatedYOffset = -1;
    private boolean myAccumulateViewportChanges;
    private boolean myViewportPositioned;

    private final DocumentAdapter myDocumentListener = new DocumentAdapter() {
        @Override
        @RequiredUIAccess
        public void beforeDocumentChange(DocumentEvent e) {
            if (!myEditor.getDocument().isInBulkUpdate()) {
                cancelAnimatedScrolling(true);
            }
        }
    };

    private final ChangeListener myViewportChangeListener = new ChangeListener() {
        private Rectangle myLastViewRect;

        @Override
        @RequiredUIAccess
        public void stateChanged(ChangeEvent event) {
            Rectangle viewRect = getVisibleArea();
            VisibleAreaEvent visibleAreaEvent = new VisibleAreaEvent(myEditor, myLastViewRect, viewRect);
            if (!myViewportPositioned && viewRect.height > 0) {
                myViewportPositioned = true;
                if (adjustVerticalOffsetIfNecessary()) {
                    return;
                }
            }
            myLastViewRect = viewRect;
            for (VisibleAreaListener listener : myVisibleAreaListeners) {
                listener.visibleAreaChanged(visibleAreaEvent);
            }
        }
    };

    public DesktopScrollingModelImpl(DesktopEditorImpl editor) {
        super(editor);
    }

    public void registerListeners() {
        myEditor.getScrollPane().getViewport().addChangeListener(myViewportChangeListener);
        myEditor.getDocument().addDocumentListener(myDocumentListener);
    }

    /**
     * Corrects viewport position if necessary on initial editor showing.
     *
     * @return <code>true</code> if the vertical viewport position has been adjusted; <code>false</code> otherwise
     */
    @RequiredUIAccess
    private boolean adjustVerticalOffsetIfNecessary() {
        // There is a possible case that the editor is configured to show virtual space at file bottom and requested position is located
        // somewhere around. We don't want to position viewport in a way that most of its area is used to represent that virtual empty space.
        // So, we tweak vertical offset if necessary.
        int maxY = Math.max(myEditor.getLineHeight(), myEditor.getDocument().getLineCount() * myEditor.getLineHeight());
        int minPreferredY = maxY - getVisibleArea().height * 2 / 3;
        final int currentOffset = getVerticalScrollOffset();
        int offsetToUse = Math.min(minPreferredY, currentOffset);
        if (offsetToUse != currentOffset) {
            scroll(getHorizontalScrollOffset(), offsetToUse);
            return true;
        }
        return false;
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public Rectangle getVisibleArea() {
        assertIsDispatchThread();
        return myEditor.getScrollPane().getViewport().getViewRect();
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public Rectangle getVisibleAreaOnScrollingFinished() {
        assertIsDispatchThread();
        if (SystemProperties.isTrueSmoothScrollingEnabled()) {
            Rectangle viewRect = myEditor.getScrollPane().getViewport().getViewRect();
            return new Rectangle(getOffset(getHorizontalScrollBar()), getOffset(getVerticalScrollBar()), viewRect.width, viewRect.height);
        }
        if (myCurrentAnimationRequest != null) {
            return myCurrentAnimationRequest.getTargetVisibleArea();
        }
        return getVisibleArea();
    }

    @Override
    @RequiredUIAccess
    public void scrollToCaret(@Nonnull ScrollType scrollType) {
        assertIsDispatchThread();
        myEditor.validateSize();
        AsyncEditorLoader.performWhenLoaded(myEditor, () -> scrollTo(myEditor.getCaretModel().getVisualPosition(), scrollType));
    }

    @RequiredUIAccess
    private void scrollTo(@Nonnull VisualPosition pos, @Nonnull ScrollType scrollType) {
        Point targetLocation = myEditor.visualPositionToXY(pos);
        scrollTo(targetLocation, scrollType);
    }

    @RequiredUIAccess
    private void scrollTo(@Nonnull Point targetLocation, @Nonnull ScrollType scrollType) {
        AnimatedScrollingRunnable canceledThread = cancelAnimatedScrolling(false);
        Rectangle viewRect = canceledThread != null ? canceledThread.getTargetVisibleArea() : getVisibleArea();
        Point p = calcOffsetsToScroll(targetLocation, scrollType, viewRect);
        scroll(p.x, p.y);
    }

    @Override
    @RequiredUIAccess
    public void scrollTo(@Nonnull LogicalPosition pos, @Nonnull ScrollType scrollType) {
        assertIsDispatchThread();

        AsyncEditorLoader.performWhenLoaded(myEditor, () -> scrollTo(myEditor.logicalPositionToXY(pos), scrollType));
    }

    @RequiredUIAccess
    private static void assertIsDispatchThread() {
        Application.get().assertIsDispatchThread();
    }

    @Override
    @RequiredUIAccess
    public void runActionOnScrollingFinished(@Nonnull Runnable action) {
        assertIsDispatchThread();

        if (myCurrentAnimationRequest != null) {
            myCurrentAnimationRequest.addPostRunnable(action);
            return;
        }

        action.run();
    }

    @Override
    public boolean isAnimationEnabled() {
        return !myAnimationDisabled;
    }

    @Override
    public void disableAnimation() {
        myAnimationDisabled = true;
    }

    @Override
    public void enableAnimation() {
        myAnimationDisabled = false;
    }

    private Point calcOffsetsToScroll(Point targetLocation, ScrollType scrollType, Rectangle viewRect) {
        if (myEditor.getSettings().isRefrainFromScrolling() && viewRect.contains(targetLocation)) {
            if (scrollType == ScrollType.CENTER ||
                scrollType == ScrollType.CENTER_DOWN ||
                scrollType == ScrollType.CENTER_UP) {
                scrollType = ScrollType.RELATIVE;
            }
        }

        int spaceWidth = EditorUtil.getSpaceWidth(Font.PLAIN, myEditor);
        int xInsets = myEditor.getSettings().getAdditionalColumnsCount() * spaceWidth;

        int hOffset = scrollType == ScrollType.CENTER ||
            scrollType == ScrollType.CENTER_DOWN ||
            scrollType == ScrollType.CENTER_UP ? 0 : viewRect.x;
        if (targetLocation.x < hOffset) {
            int inset = 4 * spaceWidth;
            if (scrollType == ScrollType.MAKE_VISIBLE && targetLocation.x < viewRect.width - inset) {
                // if we need to scroll to the left to make target position visible,
                // let's scroll to the leftmost position (if that will make caret visible)
                hOffset = 0;
            }
            else {
                hOffset = Math.max(0, targetLocation.x - inset);
            }
        }
        else if (viewRect.width > 0 && targetLocation.x >= hOffset + viewRect.width) {
            hOffset = targetLocation.x - Math.max(0, viewRect.width - xInsets);
        }

        // the following code tries to keeps 1 line above and 1 line below if available in viewRect
        int lineHeight = myEditor.getLineHeight();
        // to avoid 'hysteresis', minAcceptableY should be always less or equal to maxAcceptableY
        int minAcceptableY = viewRect.y + Math.max(0, Math.min(lineHeight, viewRect.height - 3 * lineHeight));
        int maxAcceptableY = viewRect.y + (viewRect.height <= lineHeight ? 0 :
            (viewRect.height - (viewRect.height <= 2 * lineHeight ? lineHeight : 2 * lineHeight)));
        int scrollUpBy = minAcceptableY - targetLocation.y;
        int scrollDownBy = targetLocation.y - maxAcceptableY;
        int centerPosition = targetLocation.y - viewRect.height / 3;

        int vOffset = viewRect.y;
        if (scrollType == ScrollType.CENTER) {
            vOffset = centerPosition;
        }
        else if (scrollType == ScrollType.CENTER_UP) {
            if (scrollUpBy > 0 || scrollDownBy > 0 || vOffset > centerPosition) {
                vOffset = centerPosition;
            }
        }
        else if (scrollType == ScrollType.CENTER_DOWN) {
            if (scrollUpBy > 0 || scrollDownBy > 0 || vOffset < centerPosition) {
                vOffset = centerPosition;
            }
        }
        else if (scrollType == ScrollType.RELATIVE) {
            if (scrollUpBy > 0) {
                vOffset = viewRect.y - scrollUpBy;
            }
            else if (scrollDownBy > 0) {
                vOffset = viewRect.y + scrollDownBy;
            }
        }
        else if (scrollType == ScrollType.MAKE_VISIBLE) {
            if (scrollUpBy > 0 || scrollDownBy > 0) {
                vOffset = centerPosition;
            }
        }

        JScrollPane scrollPane = myEditor.getScrollPane();
        hOffset = Math.max(0, hOffset);
        vOffset = Math.max(0, vOffset);
        hOffset = Math.min(scrollPane.getHorizontalScrollBar().getMaximum() - getExtent(scrollPane.getHorizontalScrollBar()), hOffset);
        vOffset = Math.min(scrollPane.getVerticalScrollBar().getMaximum() - getExtent(scrollPane.getVerticalScrollBar()), vOffset);

        return new Point(hOffset, vOffset);
    }

    @Nullable
    @RequiredUIAccess
    public JScrollBar getVerticalScrollBar() {
        assertIsDispatchThread();
        JScrollPane scrollPane = myEditor.getScrollPane();
        return scrollPane.getVerticalScrollBar();
    }

    @Nullable
    @RequiredUIAccess
    public JScrollBar getHorizontalScrollBar() {
        assertIsDispatchThread();
        return myEditor.getScrollPane().getHorizontalScrollBar();
    }

    @Override
    @RequiredUIAccess
    public int getVerticalScrollOffset() {
        return getOffset(getVerticalScrollBar());
    }

    @Override
    @RequiredUIAccess
    public int getHorizontalScrollOffset() {
        return getOffset(getHorizontalScrollBar());
    }

    private static int getOffset(JScrollBar scrollBar) {
        return scrollBar == null ? 0 : scrollBar.getValue();
    }

    private static int getExtent(JScrollBar scrollBar) {
        return scrollBar == null ? 0 : scrollBar.getModel().getExtent();
    }

    @Override
    @RequiredUIAccess
    public void scrollVertically(int scrollOffset) {
        scroll(getHorizontalScrollOffset(), scrollOffset);
    }

    @RequiredUIAccess
    private void _scrollVertically(int scrollOffset) {
        assertIsDispatchThread();

        myEditor.validateSize();
        JScrollBar scrollbar = myEditor.getScrollPane().getVerticalScrollBar();

        scrollbar.setValue(scrollOffset);
    }

    @Override
    @RequiredUIAccess
    public void scrollHorizontally(int scrollOffset) {
        scroll(scrollOffset, getVerticalScrollOffset());
    }

    @RequiredUIAccess
    private void _scrollHorizontally(int scrollOffset) {
        assertIsDispatchThread();

        myEditor.validateSize();
        JScrollBar scrollbar = myEditor.getScrollPane().getHorizontalScrollBar();
        scrollbar.setValue(scrollOffset);
    }

    @Override
    @RequiredUIAccess
    public void scroll(int hOffset, int vOffset) {
        if (myAccumulateViewportChanges) {
            myAccumulatedXOffset = hOffset;
            myAccumulatedYOffset = vOffset;
            return;
        }

        cancelAnimatedScrolling(false);

        VisibleEditorsTracker editorsTracker = VisibleEditorsTracker.getInstance();
        boolean useAnimation;
        //System.out.println("myCurrentCommandStart - myLastCommandFinish = " + (myCurrentCommandStart - myLastCommandFinish));
        if (!myEditor.getSettings().isAnimatedScrolling() || myAnimationDisabled || RemoteDesktopService.isRemoteSession()) {
            useAnimation = false;
        }
        else if (CommandProcessor.getInstance().getCurrentCommand() == null) {
            useAnimation = myEditor.getComponent().isShowing();
        }
        else {
            useAnimation =
                editorsTracker.getCurrentCommandStart() - editorsTracker.getLastCommandFinish() >= AnimatedScrollingRunnable.SCROLL_DURATION
                    && editorsTracker.wasEditorVisibleOnCommandStart(myEditor);
        }

        cancelAnimatedScrolling(false);

        if (useAnimation) {
            //System.out.println("scrollToAnimated: " + endVOffset);

            int startHOffset = getHorizontalScrollOffset();
            int startVOffset = getVerticalScrollOffset();

            if (startHOffset == hOffset && startVOffset == vOffset) {
                return;
            }

            //System.out.println("startVOffset = " + startVOffset);

            try {
                myCurrentAnimationRequest = new AnimatedScrollingRunnable(startHOffset, startVOffset, hOffset, vOffset);
            }
            catch (NoAnimationRequiredException e) {
                _scrollHorizontally(hOffset);
                _scrollVertically(vOffset);
            }
        }
        else {
            _scrollHorizontally(hOffset);
            _scrollVertically(vOffset);
        }
    }

    @Override
    @RequiredUIAccess
    public void finishAnimation() {
        cancelAnimatedScrolling(true);
    }

    @Nullable
    @RequiredUIAccess
    private AnimatedScrollingRunnable cancelAnimatedScrolling(boolean scrollToTarget) {
        AnimatedScrollingRunnable request = myCurrentAnimationRequest;
        myCurrentAnimationRequest = null;
        if (request != null) {
            request.cancel(scrollToTarget);
        }
        return request;
    }

    @Override
    public void dispose() {
        myEditor.getDocument().removeDocumentListener(myDocumentListener);
        myEditor.getScrollPane().getViewport().removeChangeListener(myViewportChangeListener);
    }

    @RequiredUIAccess
    public void beforeModalityStateChanged() {
        cancelAnimatedScrolling(true);
    }

    public boolean isScrollingNow() {
        return myCurrentAnimationRequest != null;
    }

    @Override
    public void accumulateViewportChanges() {
        myAccumulateViewportChanges = true;
    }

    @Override
    @RequiredUIAccess
    public void flushViewportChanges() {
        myAccumulateViewportChanges = false;
        if (myAccumulatedXOffset >= 0 && myAccumulatedYOffset >= 0) {
            scroll(myAccumulatedXOffset, myAccumulatedYOffset);
            myAccumulatedXOffset = myAccumulatedYOffset = -1;
            cancelAnimatedScrolling(true);
        }
    }

    @RequiredUIAccess
    void onBulkDocumentUpdateStarted() {
        cancelAnimatedScrolling(true);
    }

    private class AnimatedScrollingRunnable {
        private static final int SCROLL_DURATION = 100;
        private static final int SCROLL_INTERVAL = 10;

        private final int myStartHOffset;
        private final int myStartVOffset;
        private final int myEndHOffset;
        private final int myEndVOffset;
        private final int myAnimationDuration;

        private final ArrayList<Runnable> myPostRunnables = new ArrayList<>();

        private final int myHDist;
        private final int myVDist;
        private final int myMaxDistToScroll;
        private final double myTotalDist;
        private final double myScrollDist;

        private final int myStepCount;
        private final double myPow;
        private final Animator myAnimator;

        public AnimatedScrollingRunnable(
            int startHOffset,
            int startVOffset,
            int endHOffset,
            int endVOffset
        ) throws NoAnimationRequiredException {
            myStartHOffset = startHOffset;
            myStartVOffset = startVOffset;
            myEndHOffset = endHOffset;
            myEndVOffset = endVOffset;

            myHDist = Math.abs(myEndHOffset - myStartHOffset);
            myVDist = Math.abs(myEndVOffset - myStartVOffset);

            myMaxDistToScroll = myEditor.getLineHeight() * 50;
            myTotalDist = Math.sqrt((double)myHDist * myHDist + (double)myVDist * myVDist);
            myScrollDist = Math.min(myTotalDist, myMaxDistToScroll);
            myAnimationDuration = calcAnimationDuration();
            if (myAnimationDuration < SCROLL_INTERVAL * 2) {
                throw new NoAnimationRequiredException();
            }
            myStepCount = myAnimationDuration / SCROLL_INTERVAL - 1;
            double firstStepTime = 1.0 / myStepCount;
            double firstScrollDist = 5.0;
            if (myTotalDist > myScrollDist) {
                firstScrollDist *= myTotalDist / myScrollDist;
                firstScrollDist = Math.min(firstScrollDist, myEditor.getLineHeight() * 5);
            }
            myPow = myScrollDist > 0 ? setupPow(firstStepTime, firstScrollDist / myScrollDist) : 1;

            myAnimator = new Animator("Animated scroller", myStepCount, SCROLL_DURATION, false, true) {
                @Override
                @RequiredUIAccess
                public void paintNow(int frame, int totalFrames, int cycle) {
                    double time = ((double)(frame + 1)) / (double)totalFrames;
                    double fraction = timeToFraction(time);

                    final int hOffset = (int)(myStartHOffset + (myEndHOffset - myStartHOffset) * fraction + 0.5);
                    final int vOffset = (int)(myStartVOffset + (myEndVOffset - myStartVOffset) * fraction + 0.5);

                    _scrollHorizontally(hOffset);
                    _scrollVertically(vOffset);
                }

                @Override
                @RequiredUIAccess
                protected void paintCycleEnd() {
                    if (!isDisposed()) { // Animator will invoke paintCycleEnd() even if it was disposed
                        finish(true);
                    }
                }
            };

            myAnimator.resume();
        }

        @Nonnull
        @RequiredUIAccess
        public Rectangle getTargetVisibleArea() {
            Rectangle viewRect = getVisibleArea();
            return new Rectangle(myEndHOffset, myEndVOffset, viewRect.width, viewRect.height);
        }

        @RequiredUIAccess
        public void cancel(boolean scrollToTarget) {
            assertIsDispatchThread();
            finish(scrollToTarget);
        }

        public void addPostRunnable(Runnable runnable) {
            myPostRunnables.add(runnable);
        }

        @RequiredUIAccess
        private void finish(boolean scrollToTarget) {
            if (scrollToTarget || !myPostRunnables.isEmpty()) {
                _scrollHorizontally(myEndHOffset);
                _scrollVertically(myEndVOffset);
                executePostRunnables();
            }

            Disposer.dispose(myAnimator);
            if (myCurrentAnimationRequest == this) {
                myCurrentAnimationRequest = null;
            }
        }

        private void executePostRunnables() {
            for (Runnable runnable : myPostRunnables) {
                runnable.run();
            }
        }

        private double timeToFraction(double time) {
            if (time > 0.5) {
                return 1 - timeToFraction(1 - time);
            }

            double fraction = Math.pow(time * 2, myPow) / 2;

            if (myTotalDist > myMaxDistToScroll) {
                fraction *= (double)myMaxDistToScroll / myTotalDist;
            }

            return fraction;
        }

        private double setupPow(double inTime, double moveBy) {
            double pow = Math.log(2 * moveBy) / Math.log(2 * inTime);
            if (pow < 1) {
                pow = 1;
            }
            return pow;
        }

        private int calcAnimationDuration() {
            int lineHeight = myEditor.getLineHeight();
            double lineDist = myTotalDist / lineHeight;
            double part = (lineDist - 1) / 10;
            if (part > 1) {
                part = 1;
            }
            //System.out.println("duration = " + duration);
            return (int)(part * SCROLL_DURATION);
        }
    }

    private static class NoAnimationRequiredException extends Exception {
    }
}
