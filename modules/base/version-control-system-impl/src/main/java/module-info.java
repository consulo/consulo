/**
 * @author VISTALL
 * @since 10-Jul-22
 */
module consulo.version.control.system.impl {
    requires consulo.version.control.system.api;
    requires consulo.ui.ex.api;
    requires consulo.code.editor.api;
    requires consulo.project.ui.view.api;
    requires consulo.project.ui.impl;
    requires consulo.external.service.api;
    requires consulo.find.api;

    requires consulo.execution.api;

    // TODO we need it?
    requires consulo.language.editor.api;

    exports consulo.versionControlSystem.impl.internal to consulo.ide.impl, consulo.desktop.awt.ide.impl;
    exports consulo.versionControlSystem.impl.internal.action to consulo.ide.impl;
    exports consulo.versionControlSystem.impl.internal.change to consulo.ide.impl, consulo.desktop.awt.ide.impl;
    exports consulo.versionControlSystem.impl.internal.ui.awt to consulo.ide.impl, consulo.local.history.impl;
    exports consulo.versionControlSystem.impl.internal.change.ui.awt to consulo.ide.impl, consulo.desktop.awt.ide.impl, consulo.local.history.impl;
    exports consulo.versionControlSystem.impl.internal.change.ui to consulo.ide.impl;
    exports consulo.versionControlSystem.impl.internal.change.commited to consulo.ide.impl;
    exports consulo.versionControlSystem.impl.internal.update to consulo.ide.impl;
    exports consulo.versionControlSystem.impl.internal.commit to consulo.ide.impl;
    exports consulo.versionControlSystem.impl.internal.change.action to consulo.ide.impl;
    exports consulo.versionControlSystem.impl.internal.history to consulo.ide.impl, consulo.desktop.awt.ide.impl;
    exports consulo.versionControlSystem.impl.internal.change.patch to consulo.ide.impl, consulo.desktop.awt.ide.impl;
    exports consulo.versionControlSystem.impl.internal.change.shelf to consulo.ide.impl, consulo.desktop.awt.ide.impl;
    exports consulo.versionControlSystem.impl.internal.patch to consulo.ide.impl, consulo.desktop.awt.ide.impl;
    exports consulo.versionControlSystem.impl.internal.change.conflict to consulo.ide.impl;
    exports consulo.versionControlSystem.impl.internal.checkin to consulo.ide.impl;
    exports consulo.versionControlSystem.impl.internal.diff to consulo.ide.impl, consulo.desktop.awt.ide.impl;
    exports consulo.versionControlSystem.impl.internal.util to consulo.ide.impl, consulo.desktop.awt.ide.impl;
    exports consulo.versionControlSystem.impl.internal.annotate to consulo.ide.impl, consulo.desktop.awt.ide.impl;
    exports consulo.versionControlSystem.impl.internal.contentAnnotation to consulo.ide.impl;
    exports consulo.versionControlSystem.impl.internal.patch.apply to consulo.desktop.awt.ide.impl;
    exports consulo.versionControlSystem.impl.internal.patch.tool to consulo.desktop.awt.ide.impl;

    opens consulo.versionControlSystem.impl.internal.change.commited to consulo.util.xml.serializer;
    opens consulo.versionControlSystem.impl.internal.contentAnnotation to consulo.util.xml.serializer;
    opens consulo.versionControlSystem.impl.internal.change.conflict to consulo.util.xml.serializer, consulo.ui.ex.awt.api;
    opens consulo.versionControlSystem.impl.internal.change to consulo.util.xml.serializer;
    opens consulo.versionControlSystem.impl.internal.action to consulo.component.impl;
    opens consulo.versionControlSystem.impl.internal.change.patch to consulo.component.impl;
    opens consulo.versionControlSystem.impl.internal.change.action to consulo.component.impl;
    opens consulo.versionControlSystem.impl.internal.change.ui.awt to consulo.proxy;

    requires com.google.common;

    // TODO remove in future
    requires java.desktop;
    requires forms.rt;
    requires miglayout;
}