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
package consulo.project.ui.impl.internal.wm;

import consulo.logging.Logger;
import consulo.project.ui.internal.WindowInfoImpl;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;

import java.util.Iterator;
import java.util.Stack;

/**
 * This is stack of tool window that were replaced by another tool windows.
 *
 * @author Vladimir Kondratyev
 */
public final class ToolWindowSideStack {
    private static final Logger LOG = Logger.getInstance(ToolWindowSideStack.class);
    private final Stack myStack;

    public ToolWindowSideStack() {
        myStack = new Stack();
    }

    /**
     * Pushes <code>info</code> into the stack. The method stores cloned copy of original <code>info</code>.
     */
    public void push(WindowInfoImpl info) {
        LOG.assertTrue(info.isDocked());
        LOG.assertTrue(!info.isAutoHide());
        myStack.push(info.copy());
    }

    public WindowInfoImpl pop(ToolWindowAnchor anchor) {
        for (int i = myStack.size() - 1; true; i--) {
            WindowInfoImpl info = (WindowInfoImpl) myStack.get(i);
            if (anchor == info.getAnchor()) {
                myStack.remove(i);
                return info;
            }
        }
    }

    /**
     * @return <code>true</code> if and only if there is window in the state with the same
     * <code>anchor</code> as the specified <code>info</code>.
     */
    public boolean isEmpty(ToolWindowAnchor anchor) {
        for (int i = myStack.size() - 1; i > -1; i--) {
            WindowInfoImpl info = (WindowInfoImpl) myStack.get(i);
            if (anchor == info.getAnchor()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Removes all <code>WindowInfo</code>s with the specified <code>id</code>.
     */
    public void remove(String id) {
        for (Iterator i = myStack.iterator(); i.hasNext(); ) {
            WindowInfoImpl info = (WindowInfoImpl) i.next();
            if (id.equals(info.getId())) {
                i.remove();
            }
        }
    }

    public void clear() {
        myStack.clear();
    }
}
