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
import consulo.execution.debug.frame.XNamedValue;
import consulo.execution.debug.frame.XValueChildrenList;
import consulo.execution.debug.frame.XValueNode;
import consulo.execution.debug.frame.XValuePlace;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.ui.image.Image;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-12
 */
public class SandValue extends XNamedValue {
    private final String myType;
    private final String myValue;
    private final List<SandValue> myChildren;

    public SandValue(String name, String type, String value) {
        this(name, type, value, List.of());
    }

    public SandValue(String name, String type, String value, List<SandValue> children) {
        super(name);
        myType = type;
        myValue = value;
        myChildren = children;
    }

    @Override
    public void computePresentation(XValueNode node, XValuePlace place) {
        Image icon = myChildren.isEmpty() ? ExecutionDebugIconGroup.nodePrimitive() : ExecutionDebugIconGroup.nodeValue();
        node.setPresentation(icon, myType, myValue, !myChildren.isEmpty());
    }

    @Override
    public void computeChildren(XCompositeNode node) {
        XValueChildrenList children = new XValueChildrenList(myChildren.size());
        for (SandValue child : myChildren) {
            children.add(child);
        }
        node.addChildren(children, true);
    }
}
