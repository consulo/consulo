/**
 * @author VISTALL
 * @since 13/01/2022
 */
module consulo.base.ide {
  requires java.desktop;
  requires java.xml;
  requires java.rmi;
  requires java.management;
  requires java.prefs;
  requires jdk.unsupported;

  requires consulo.container.api;
  requires consulo.container.impl;
  requires consulo.util.nodep;

  requires consulo.bootstrap;

  requires consulo.desktop.awt.hacking;
  requires consulo.desktop.awt.eawt.wrapper;

  requires transitive consulo.injecting.api;
  requires transitive consulo.annotation;
  requires transitive consulo.base.proxy;
  requires transitive consulo.base.runtime.api;
  requires transitive consulo.base.localize.library;
  requires transitive consulo.base.icon.library;
  requires transitive consulo.ui.api;
  requires transitive consulo.disposer.api;
  requires transitive consulo.logging.api;
  requires transitive consulo.localize.api;

  requires consulo.ui.impl;
  requires consulo.localize.impl;

  requires consulo.xcoverage.rt;
  requires consulo.remote.servers.agent.rt;

  requires transitive consulo.util.collection;
  requires transitive consulo.util.collection.primitive;
  requires transitive consulo.util.concurrent;
  requires transitive consulo.util.dataholder;
  //requires consulo.util.interner;
  requires transitive consulo.util.io;
  requires transitive consulo.util.jdom;
  requires transitive consulo.util.lang;
  requires transitive consulo.util.rmi;
  requires transitive consulo.util.serializer;

  requires gnu.trove;
  requires io.netty.buffer;
  requires io.netty.codec;
  requires io.netty.codec.http;
  requires io.netty.common;
  requires io.netty.handler;
  requires io.netty.resolver;
  requires io.netty.transport;

  requires org.apache.httpcomponents.httpcore;
  requires org.apache.httpcomponents.httpclient;
  requires org.apache.httpcomponents.httpmime;

  requires org.apache.commons.lang3;
  requires org.apache.thrift;
  requires org.apache.commons.codec;
  requires org.apache.commons.compress;
  requires org.apache.commons.imaging;
  requires velocity.engine.core;

  requires com.google.common;
  requires com.google.gson;
  requires build.serviceMessages;

  requires com.sun.jna;
  requires com.sun.jna.platform;

  requires org.lz4.java;                                                                     

  requires automaton;
  requires miglayout;
  requires microba;
  requires imgscalr.lib;
  requires proxy.vole;
  requires args4j;
  requires nanoxml;
  requires winp;
  requires xmlrpc.client;
  requires xmlrpc.common;
  requires xmlrpc.server;
  requires pty4j;
  requires forms.rt;
  
  requires transitive jakarta.inject;
  requires transitive kava.beans;
  requires transitive org.slf4j;

  exports com.intellij.idea;
  exports com.intellij.openapi.util.io;
  exports com.intellij.openapi.util;
  exports com.intellij.util;
}