// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.api.graphql;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public final class CachingGraphQLQueryLoaderTest {

  @Rule
  public TemporaryFolder testDataRule = new TemporaryFolder();

  private CachingGraphQLQueryLoader loader;
  private File queryFile;
  private File fragmentFolder;

  @Before
  public void setUp() throws IOException {
    loader = new CachingGraphQLQueryLoader(path -> {
      Path resolved = testDataRule.getRoot().toPath().resolve(path);
      if (Files.exists(resolved)) {
        try {
          return Files.newInputStream(resolved);
        }
        catch (IOException e) {
          return null;
        }
      }
      return null;
    });
    testDataRule.newFolder("graphql", "query");
    fragmentFolder = testDataRule.newFolder("graphql", "fragment");
    queryFile = testDataRule.newFile("graphql/query/q.graphql");
  }

  @Test
  public void testSimpleQuery() throws IOException {
    String source =
      "query(id: ID!) {\n" +
      "    node(id: id) {\n" +
      "        name\n" +
      "    }\n" +
      "}";
    String loaded = writeAndLoadQuery(source);
    check(loaded, source);
  }

  @Test
  public void testQueryWithInnerFragment() throws IOException {
    String source =
      "fragment inner on Something {\n" +
      "    name\n" +
      "}\n" +
      "\n" +
      "query(id: ID!) {\n" +
      "    node(id: id) {\n" +
      "        name\n" +
      "        ...inner\n" +
      "    }\n" +
      "}";
    String loaded = writeAndLoadQuery(source);
    check(loaded, source);
  }

  @Test
  public void testQueryWithOutsideFragment() throws IOException {
    String fragmentSource =
      "fragment outer on Something {\n" +
      "    name\n" +
      "}";
    writeFragment("outer", fragmentSource);

    String querySource =
      "query(id: ID!) {\n" +
      "    node(id: id) {\n" +
      "        name\n" +
      "        ...outer\n" +
      "    }\n" +
      "}";

    String loaded = writeAndLoadQuery(querySource);
    check(loaded, fragmentSource, querySource);
  }

  @Test
  public void testQueryWithInnerAndOutsideFragment() throws IOException {
    String fragmentSource =
      "fragment outer on Something {\n" +
      "    name\n" +
      "}";
    writeFragment("outer", fragmentSource);

    String querySource =
      "fragment inner on Something {\n" +
      "    ...outer\n" +
      "    name\n" +
      "}\n" +
      "\n" +
      "query(id: ID!) {\n" +
      "    node(id: id) {\n" +
      "        name\n" +
      "        ...inner\n" +
      "        ...outer\n" +
      "    }\n" +
      "}";

    String loaded = writeAndLoadQuery(querySource);
    check(loaded, fragmentSource, querySource);
  }

  @Test
  public void testCircularFragments() throws IOException {
    String fragment1Source =
      "fragment outer1 on Something {\n" +
      "    ...outer2\n" +
      "}";
    writeFragment("outer1", fragment1Source);

    String fragment2Source =
      "fragment outer2 on Something {\n" +
      "    ...outer1\n" +
      "}";
    writeFragment("outer2", fragment2Source);

    String querySource =
      "fragment inner on Something {\n" +
      "    name\n" +
      "}\n" +
      "\n" +
      "query(id: ID!) {\n" +
      "    node(id: id) {\n" +
      "        name\n" +
      "        ...outer1\n" +
      "        ...outer2\n" +
      "    }\n" +
      "}";

    String loaded = writeAndLoadQuery(querySource);
    check(loaded, fragment2Source, fragment1Source, querySource);
  }

  @Test
  public void testDependencyOrder() throws IOException {
    String fragment1Source =
      "fragment outer1 on Something {\n" +
      "    ...outer2\n" +
      "}";
    writeFragment("outer1", fragment1Source);

    String fragment2Source =
      "fragment outer2 on Something {\n" +
      "    name\n" +
      "}";
    writeFragment("outer2", fragment2Source);

    String querySource =
      "fragment inner on Something {\n" +
      "    name\n" +
      "}\n" +
      "\n" +
      "query(id: ID!) {\n" +
      "    node(id: id) {\n" +
      "        name\n" +
      "        ...outer1\n" +
      "    }\n" +
      "}";

    String loaded = writeAndLoadQuery(querySource);
    check(loaded, fragment2Source, fragment1Source, querySource);
  }

  private void writeFragment(String name, String source) throws IOException {
    Path file = fragmentFolder.toPath().resolve(name + ".graphql");
    Files.createDirectories(file.getParent());
    Files.createFile(file);
    Files.writeString(file, source);
  }

  private String writeAndLoadQuery(String querySource) throws IOException {
    Files.writeString(queryFile.toPath(), querySource);
    return loader.loadQuery(queryFile.getPath());
  }

  private void check(String loaded, String... sources) {
    String trimmedSource = Arrays.stream(sources)
      .collect(Collectors.joining("\n"))
      .lines()
      .map(String::trim)
      .collect(Collectors.joining("\n"));
    assertEquals(trimmedSource, loaded);
  }
}
