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

import org.jspecify.annotations.NullMarked;

/**
 * Upstream Bazel plugin core modules compiled as one module (they share packages), see CONSULO.md:
 * {@code intellij.bazel.languages}, {@code intellij.bazel.commons}, {@code intellij.bazel.projectview},
 * {@code intellij.bazel.backend}.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
@NullMarked
module consulo.bazel.base {
    requires transitive consulo.language.api;
    requires consulo.language.impl;
    requires consulo.language.editor.api;
    requires consulo.language.code.style.api;
    requires consulo.language.code.style.ui.api;
    requires consulo.datacontext.api;
    requires consulo.index.io;
    requires consulo.disposer.api;
    requires consulo.color.scheme.api;
    requires consulo.code.editor.api;
    requires consulo.ui.api;
    requires consulo.application.api;
    requires consulo.application.content.api;
    requires consulo.component.api;
    requires consulo.project.api;
    requires consulo.project.ui.api;
    requires consulo.module.api;
    requires consulo.virtual.file.system.api;
    requires consulo.platform.api;
    requires consulo.process.api;
    requires consulo.build.ui.api;
    requires consulo.logging.api;
    requires consulo.util.concurrent.coroutine;
    requires consulo.util.io;
    requires consulo.util.lang;
    requires consulo.util.collection;
    requires consulo.util.interner;
    requires consulo.util.dataholder;

    requires com.google.gson;
    requires com.google.common;
    requires com.dynatrace.hash4j;
    requires org.commonmark;
    requires it.unimi.dsi.fastutil;
    requires tools.jackson.databind;
    requires tools.jackson.dataformat.xml;
    requires com.h2database.mvstore;
    requires com.esotericsoftware.kryo.kryo5;
    requires org.commonmark.ext.gfm.tables;

    requires transitive consulo.bazel.protobuf;
    requires transitive consulo.bazel.aspect.sdk;

    // generated
    exports consulo.bazel.languages.icon;
    exports consulo.bazel.languages.localize;
    exports consulo.bazel.commons.localize;
    exports consulo.bazel.projectview.localize;
    exports consulo.bazel.backend.localize;

    // consulo-only
    exports consulo.bazel.languages;
    exports consulo.bazel.commons;
    exports consulo.bazel.documentation;
    exports consulo.bazel.projectview;
    exports consulo.bazel.workspace;
    exports consulo.bazel.aspect;

    exports org.jetbrains.bazel.languages;
    exports org.jetbrains.bazel.languages.bazelquery;
    exports org.jetbrains.bazel.languages.bazelquery.elements;
    exports org.jetbrains.bazel.languages.bazelquery.highlighting;
    exports org.jetbrains.bazel.languages.bazelquery.lexer;
    exports org.jetbrains.bazel.languages.bazelquery.parser;
    exports org.jetbrains.bazel.languages.bazelquery.psi;
    exports org.jetbrains.bazel.languages.bazelrc;
    exports org.jetbrains.bazel.languages.bazelrc.commenter;
    exports org.jetbrains.bazel.languages.bazelrc.elements;
    exports org.jetbrains.bazel.languages.bazelrc.highlighting;
    exports org.jetbrains.bazel.languages.bazelrc.lexer;
    exports org.jetbrains.bazel.languages.bazelrc.matching;
    exports org.jetbrains.bazel.languages.bazelrc.parser;
    exports org.jetbrains.bazel.languages.bazelrc.psi;
    exports org.jetbrains.bazel.languages.bazelversion;
    exports org.jetbrains.bazel.languages.bazelversion.parser;
    exports org.jetbrains.bazel.languages.bazelversion.psi;
    exports org.jetbrains.bazel.languages.starlark;
    exports org.jetbrains.bazel.languages.starlark.bazel;
    exports org.jetbrains.bazel.languages.starlark.commenter;
    exports org.jetbrains.bazel.languages.starlark.elements;
    exports org.jetbrains.bazel.languages.starlark.folding;
    exports org.jetbrains.bazel.languages.starlark.highlighting;
    exports org.jetbrains.bazel.languages.starlark.indentation;
    exports org.jetbrains.bazel.languages.starlark.lexer;
    exports org.jetbrains.bazel.languages.starlark.matching;
    exports org.jetbrains.bazel.languages.starlark.parser;
    exports org.jetbrains.bazel.languages.starlark.psi;
    exports org.jetbrains.bazel.languages.starlark.psi.expressions;
    exports org.jetbrains.bazel.languages.starlark.psi.expressions.arguments;
    exports org.jetbrains.bazel.languages.starlark.psi.functions;
    exports org.jetbrains.bazel.languages.starlark.psi.statements;
    exports org.jetbrains.bazel.languages.starlark.references;
    exports org.jetbrains.bazel.languages.starlark.rename;
    exports org.jetbrains.bazel.languages.starlark.utils;

    // intellij.bazel.commons
    exports org.jetbrains.bazel.buildifier;
    exports org.jetbrains.bazel.commons;
    exports org.jetbrains.bazel.commons.constants;
    exports org.jetbrains.bazel.commons.gson;
    exports org.jetbrains.bazel.commons.phased;
    exports org.jetbrains.bazel.commons.symlinks;
    exports org.jetbrains.bazel.config;
    exports org.jetbrains.bazel.coroutines;
    exports org.jetbrains.bazel.ignore;
    exports org.jetbrains.bazel.label;
    exports org.jetbrains.bazel.languages.projectview;
    exports org.jetbrains.bazel.languages.projectview.imports;
    exports org.jetbrains.bazel.performance;
    exports org.jetbrains.bazel.progress;
    exports org.jetbrains.bazel.server.diagnostics;
    exports org.jetbrains.bazel.settings.bazel;
    exports org.jetbrains.bazel.sync;
    exports org.jetbrains.bazel.sync.environment;
    exports org.jetbrains.bazel.sync.workspace.persistence;
    exports org.jetbrains.bazel.sync.workspace.snapshot;
    exports org.jetbrains.bazel.testing;
    exports org.jetbrains.bazel.utils;
    exports org.jetbrains.bazel.workspace;
    exports org.jetbrains.bsp.protocol;
    exports org.jetbrains.bsp.protocol.utils;

    // intellij.bazel.projectview
    exports org.jetbrains.bazel.flow.open;
    exports org.jetbrains.bazel.languages.bazelrc.documentation;
    exports org.jetbrains.bazel.languages.bazelrc.flags;
    exports org.jetbrains.bazel.languages.projectview.annotation;
    exports org.jetbrains.bazel.languages.projectview.annotation.quickfix;
    exports org.jetbrains.bazel.languages.projectview.base;
    exports org.jetbrains.bazel.languages.projectview.checker;
    exports org.jetbrains.bazel.languages.projectview.commenter;
    exports org.jetbrains.bazel.languages.projectview.completion;
    exports org.jetbrains.bazel.languages.projectview.documentation;
    exports org.jetbrains.bazel.languages.projectview.elements;
    exports org.jetbrains.bazel.languages.projectview.formatting;
    exports org.jetbrains.bazel.languages.projectview.highlighting;
    exports org.jetbrains.bazel.languages.projectview.lexer;
    exports org.jetbrains.bazel.languages.projectview.parser;
    exports org.jetbrains.bazel.languages.projectview.project;
    exports org.jetbrains.bazel.languages.projectview.psi;
    exports org.jetbrains.bazel.languages.projectview.psi.sections;
    exports org.jetbrains.bazel.languages.projectview.references;


    // intellij.bazel.backend
    exports org.jetbrains.bazel.action;
    exports org.jetbrains.bazel.bazelrunner;
    exports org.jetbrains.bazel.bazelrunner.outputs;
    exports org.jetbrains.bazel.bazelrunner.params;
    exports org.jetbrains.bazel.install;
    exports org.jetbrains.bazel.languages.starlark.repomapping;
    exports org.jetbrains.bazel.magicmetamodel;
    exports org.jetbrains.bazel.run.task;
    exports org.jetbrains.bazel.server;
    exports org.jetbrains.bazel.server.bep;
    exports org.jetbrains.bazel.server.bsp;
    exports org.jetbrains.bazel.server.bsp.managers;
    exports org.jetbrains.bazel.server.bsp.utils;
    exports org.jetbrains.bazel.server.bzlmod;
    exports org.jetbrains.bazel.server.model;
    exports org.jetbrains.bazel.server.sync;
    exports org.jetbrains.bazel.server.sync.firstPhase;
    exports org.jetbrains.bazel.server.sync.sharding;
    exports org.jetbrains.bazel.sync.projectStructure;
    exports org.jetbrains.bazel.sync.status;
    exports org.jetbrains.bazel.sync.task;
    exports org.jetbrains.bazel.sync.workspace;
    exports org.jetbrains.bazel.sync.workspace.importer;
    exports org.jetbrains.bazel.sync.workspace.languages;
    exports org.jetbrains.bazel.sync.workspace.mapper;
    exports org.jetbrains.bazel.sync.workspace.persistence.mvstore;
    exports org.jetbrains.bazel.sync.workspace.targetKind;
    exports org.jetbrains.bazel.target;
    exports org.jetbrains.bazel.taskEvents;
    exports org.jetbrains.bazel.util;
    exports org.jetbrains.bazel.workspace.indexing;
    exports org.jetbrains.bazel.workspace.packageMarker;
    exports org.jetbrains.bazel.workspacemodel.entities;

    // reflection: persistent settings state and Gson-parsed model classes
    opens org.jetbrains.bazel.settings.bazel to consulo.util.xml.serializer;
    opens org.jetbrains.bazel.bazelrunner to com.google.gson;
    opens org.jetbrains.bazel.server.bsp.managers to com.google.gson;
    opens org.jetbrains.bazel.server.sync to com.google.gson;
    opens org.jetbrains.bazel.server.bep to tools.jackson.databind;
    opens consulo.bazel.workspace to consulo.util.xml.serializer;
    // Kryo serializes the persisted workspace snapshot by reflection
    opens org.jetbrains.bazel.sync.workspace.persistence.mvstore to com.esotericsoftware.kryo.kryo5;
    opens org.jetbrains.bazel.sync.workspace.snapshot to com.esotericsoftware.kryo.kryo5;
    opens org.jetbrains.bazel.label to com.esotericsoftware.kryo.kryo5;
    opens org.jetbrains.bazel.commons to com.esotericsoftware.kryo.kryo5;
    opens org.jetbrains.bsp.protocol to com.google.gson, com.esotericsoftware.kryo.kryo5;
}
