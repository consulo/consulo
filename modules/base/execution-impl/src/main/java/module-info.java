/**
 * @author VISTALL
 * @since 08-Aug-22
 */
module consulo.execution.impl {
    requires consulo.execution.api;
    requires consulo.configuration.editor.api;
    requires static consulo.task.api;

    requires com.google.common;

    // TODO remove in future
    requires consulo.ui.ex.awt.api;
    requires java.desktop;
    requires forms.rt;

    exports consulo.execution.impl.internal to consulo.ide.impl;
    exports consulo.execution.impl.internal.action to
        consulo.ide.impl,
        consulo.execution.debug.impl;

    exports consulo.execution.impl.internal.configuration to
        consulo.ide.impl;

    opens consulo.execution.impl.internal.configuration to
        consulo.component.impl;

    exports consulo.execution.impl.internal.dashboard to consulo.ide.impl;
    exports consulo.execution.impl.internal.dashboard.tree to consulo.ide.impl;
    exports consulo.execution.impl.internal.service to consulo.ide.impl;
    exports consulo.execution.impl.internal.ui to consulo.ide.impl;

    exports consulo.execution.impl.internal.ui.layout to
        consulo.ide.impl,
        consulo.execution.debug.impl,
        consulo.desktop.awt.ide.impl;

    exports consulo.execution.impl.internal.ui.layout.action to
        consulo.ide.impl,
        consulo.execution.debug.impl,
        consulo.desktop.awt.ide.impl;

    exports consulo.execution.impl.internal.dashboard.action to consulo.execution.debug.impl;

    opens consulo.execution.impl.internal.ui.layout.action to consulo.component.impl;

    opens consulo.execution.impl.internal.action to consulo.component.impl;

    opens consulo.execution.impl.internal.service to consulo.component.impl, consulo.util.xml.serializer;

    opens consulo.execution.impl.internal.dashboard to consulo.util.xml.serializer;

    opens consulo.execution.impl.internal.ui.layout to consulo.util.xml.serializer;

    opens consulo.execution.impl.internal.console to consulo.util.xml.serializer, consulo.component.impl;
}