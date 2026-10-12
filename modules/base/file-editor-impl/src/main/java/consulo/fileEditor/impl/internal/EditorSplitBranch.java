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
package consulo.fileEditor.impl.internal;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public final class EditorSplitBranch extends EditorSplitNode {
    private boolean myVertical;
    private float myProportion;
    private EditorSplitNode myFirst;
    private EditorSplitNode mySecond;

    public EditorSplitBranch(boolean vertical, float proportion, EditorSplitNode first, EditorSplitNode second) {
        myVertical = vertical;
        myProportion = proportion;
        myFirst = first;
        mySecond = second;

        first.setParentBranch(this);
        second.setParentBranch(this);
    }

    public boolean isVertical() {
        return myVertical;
    }

    public void setVertical(boolean vertical) {
        myVertical = vertical;
    }

    public float getProportion() {
        return myProportion;
    }

    public void setProportion(float proportion) {
        myProportion = proportion;
    }

    public EditorSplitNode getFirst() {
        return myFirst;
    }

    public EditorSplitNode getSecond() {
        return mySecond;
    }

    public EditorSplitNode getOther(EditorSplitNode child) {
        return child == myFirst ? mySecond : myFirst;
    }

    void replace(EditorSplitNode oldChild, EditorSplitNode newChild) {
        if (myFirst == oldChild) {
            myFirst = newChild;
        }
        else if (mySecond == oldChild) {
            mySecond = newChild;
        }
        else {
            throw new IllegalArgumentException(oldChild + " is not a child of this split");
        }

        newChild.setParentBranch(this);
    }
}
