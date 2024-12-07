/**
 * @author VISTALL
 * @since 08-Aug-22
 */
module consulo.execution.debug.impl {
    requires consulo.diff.api;
    requires consulo.execution.debug.api;
    requires consulo.bookmark.ui.view.api;
    requires consulo.task.api;

    requires consulo.execution.impl;
    
    requires consulo.code.editor.impl;

    requires it.unimi.dsi.fastutil;

    // TODO remove in future
    requires java.desktop;
    requires forms.rt;

    exports consulo.execution.debug.impl.internal.action to consulo.ide.impl;
    exports consulo.execution.debug.impl.internal to consulo.ide.impl, consulo.util.xml.serializer;
    exports consulo.execution.debug.impl.internal.breakpoint to consulo.ide.impl;
    exports consulo.execution.debug.impl.internal.breakpoint.ui to consulo.ide.impl;

    opens consulo.execution.debug.impl.internal.action to consulo.component.impl;
    opens consulo.execution.debug.impl.internal.frame.action to consulo.component.impl;
    opens consulo.execution.debug.impl.internal.ui.tree.action to consulo.component.impl;
    opens consulo.execution.debug.impl.internal.setting to consulo.util.xml.serializer;
    opens consulo.execution.debug.impl.internal.breakpoint to consulo.util.xml.serializer;
    opens consulo.execution.debug.impl.internal.action.handler to consulo.util.xml.serializer;
}
