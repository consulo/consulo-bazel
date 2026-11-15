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
 * Go integration (upstream golang/ module), an optional library of the Bazel plugin loaded only with
 * consulo.google.go.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
@NullMarked
module consulo.bazel.go {
    requires consulo.bazel.base;
    requires consulo.language.api;
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
    requires consulo.execution.api;
    requires consulo.execution.debug.api;
    requires consulo.execution.test.api;
    requires consulo.execution.test.sm.api;
    requires consulo.execution.coverage.api;
    requires consulo.compiler.api;
    requires consulo.external.system.api;
    requires consulo.search.everywhere.api;
    requires consulo.module.creation.api;
    requires consulo.file.editor.api;
    requires consulo.project.ui.view.api;
    requires consulo.module.content.api;
    requires consulo.version.control.system.api;
    requires consulo.configurable.api;
    requires consulo.ui.ex.api;
    requires consulo.ui.ex.awt.api;
    requires consulo.http.api;
    requires consulo.usage.api;
    requires consulo.find.api;
    requires consulo.language.editor.refactoring.api;
    requires consulo.language.editor.ui.api;
    requires consulo.base.icon.library;
    requires consulo.ide.api;
    requires consulo.ide.impl;
    requires consulo.proxy;
    requires consulo.util.jdom;
    requires consulo.util.xml.serializer;
    requires consulo.document.api;
    requires consulo.navigation.api;
    requires consulo.web.browser.api;
    requires consulo.project.content.api;
    requires consulo.file.chooser.api;
    requires consulo.undo.redo.api;
    requires consulo.application.ui.api;
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
    requires consulo.container.api;
    requires java.desktop;

    requires consulo.google.go;
    requires consulo.go.toolchain;
    requires consulo.go.language.impl;
    requires consulo.go.execution;

    exports consulo.bazel.go.localize;
    exports org.jetbrains.bazel.golang;
    exports org.jetbrains.bazel.golang.coverage;
    exports org.jetbrains.bazel.golang.debug;
    exports org.jetbrains.bazel.golang.inspections;
    exports org.jetbrains.bazel.golang.resolve;
    exports org.jetbrains.bazel.golang.run;
    exports org.jetbrains.bazel.golang.sync;
    exports org.jetbrains.bazel.golang.targetKinds;
    exports org.jetbrains.bazel.golang.treeview;
    exports org.jetbrains.bazel.golang.workspace;
}
