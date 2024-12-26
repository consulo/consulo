/**
 * @author VISTALL
 * @since 2022-01-22
 */
module consulo.language.api {
    requires transitive consulo.document.api;
    requires transitive consulo.index.io;
    requires transitive consulo.module.content.api;
    requires transitive consulo.navigation.api;
    requires transitive consulo.project.content.api;
    requires consulo.ui.ex.api;

    exports consulo.language;
    exports consulo.language.ast;
    exports consulo.language.cacheBuilder;
    exports consulo.language.content;
    exports consulo.language.custom;
    exports consulo.language.controlFlow;
    exports consulo.language.controlFlow.base;
    exports consulo.language.dataFlow;
    exports consulo.language.dataFlow.map;
    exports consulo.language.extension;
    exports consulo.language.file;
    exports consulo.language.findUsage;
    exports consulo.language.file.event;
    exports consulo.language.file.inject;
    exports consulo.language.file.light;
    exports consulo.language.icon;
    exports consulo.language.inject;
    exports consulo.language.lexer;
    exports consulo.language.localize;
    exports consulo.language.navigation;
    exports consulo.language.parser;
    exports consulo.language.pattern;
    exports consulo.language.pattern.compiler;
    exports consulo.language.plain;
    exports consulo.language.plain.ast;
    exports consulo.language.plain.psi;
    exports consulo.language.plain.psi.stub.todo;
    exports consulo.language.pom;
    exports consulo.language.pom.event;
    exports consulo.language.pratt;
    exports consulo.language.psi;
    exports consulo.language.psi.event;
    exports consulo.language.psi.include;
    exports consulo.language.psi.filter;
    exports consulo.language.psi.filter.position;
    exports consulo.language.psi.meta;
    exports consulo.language.psi.path;
    exports consulo.language.psi.resolve;
    exports consulo.language.psi.scope;
    exports consulo.language.psi.search;
    exports consulo.language.psi.stub;
    exports consulo.language.psi.stub.gist;
    exports consulo.language.psi.stub.todo;
    exports consulo.language.psi.util;
    exports consulo.language.scratch;
    exports consulo.language.sem;
    exports consulo.language.template;
    exports consulo.language.util;
    exports consulo.language.version;

    exports consulo.language.psi.internal to
        consulo.ide.impl,
        consulo.file.editor.impl,
        consulo.language.impl;
    exports consulo.language.internal.psi.stub to
        consulo.ide.impl,
        consulo.language.impl;
    exports consulo.language.internal to
        consulo.find.api,
        consulo.ide.impl,
        consulo.language.editor.api,
        consulo.language.editor.refactoring.api,
        consulo.language.editor.ui.api,
        consulo.language.impl,
        consulo.usage.api;
    exports consulo.language.psi.stub.internal to
        consulo.ide.impl,
        consulo.language.impl;
    exports consulo.language.internal.custom to
        consulo.ide.impl,
        consulo.language.editor.impl;

    // cache value impl visitor
    opens consulo.language.psi.stub to
        consulo.application.impl;
    opens consulo.language.psi.util to
        consulo.application.impl,
        consulo.ide.impl,
        consulo.language.impl;
}