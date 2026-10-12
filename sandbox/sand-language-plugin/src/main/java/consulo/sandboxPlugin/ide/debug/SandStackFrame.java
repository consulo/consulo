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
package consulo.sandboxPlugin.ide.debug;

import consulo.execution.debug.frame.XCompositeNode;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XValueChildrenList;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public class SandStackFrame extends XStackFrame {
    private final String myMethod;
    private final int myLine;
    private final List<SandValue> myVariables;

    public SandStackFrame(String method, int line, List<SandValue> variables) {
        myMethod = method;
        myLine = line;
        myVariables = variables;
    }

    @Override
    public Object getEqualityObject() {
        return myMethod;
    }

    @Override
    public void customizePresentation(ColoredTextContainer component) {
        component.append(myMethod + "()", SimpleTextAttributes.REGULAR_ATTRIBUTES);
        component.append(":" + myLine, SimpleTextAttributes.GRAYED_ATTRIBUTES);
        component.append(", Main.sand", SimpleTextAttributes.GRAYED_ITALIC_ATTRIBUTES);
    }

    @Override
    public void computeChildren(XCompositeNode node) {
        XValueChildrenList children = new XValueChildrenList(myVariables.size());
        for (SandValue variable : myVariables) {
            children.add(variable);
        }
        node.addChildren(children, true);
    }
}
