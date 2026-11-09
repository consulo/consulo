/**
 * @author VISTALL
 * @since 2022-08-01
 */
@SuppressWarnings("module")
module consulo.task.api {
    requires transitive consulo.project.api;
    requires transitive consulo.version.control.system.api;
    requires transitive consulo.code.editor.api;
    requires transitive consulo.account.management.api;

    requires com.google.gson;

    exports consulo.task;
    exports consulo.task.context;
    exports consulo.task.event;
    exports consulo.task.issue;
    exports consulo.task.ui;
    exports consulo.task.util;
    exports consulo.task.util.gson;
    exports consulo.task.icon;
    exports consulo.task.localize;
    exports consulo.task.internal to
        consulo.task.impl,
        consulo.ide.impl;

    opens consulo.task to consulo.util.xml.serializer;
}