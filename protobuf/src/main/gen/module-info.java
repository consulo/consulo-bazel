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

/**
 * consulo: module descriptor for upstream's generated Bazel protobuf classes.
 *
 * @author VISTALL
 * @since 2026-10-24
 */
module consulo.bazel.protobuf {
    requires transitive com.google.protobuf;
    requires transitive io.grpc;
    requires transitive io.grpc.stub;
    requires transitive io.grpc.protobuf;
    requires transitive proto.google.common.protos;
    requires static java.annotation;
    requires com.google.common;

    exports com.google.devtools.build.lib.actions.cache;
    exports com.google.devtools.build.lib.buildeventstream;
    exports com.google.devtools.build.lib.packages.metrics;
    exports com.google.devtools.build.lib.query2.proto.proto2api;
    exports com.google.devtools.build.lib.runtime.proto;
    exports com.google.devtools.build.lib.server;
    exports com.google.devtools.build.lib.starlarkdebugging;
    exports com.google.devtools.build.lib.starlarkdocextract;
    exports com.google.devtools.build.lib.view.proto;
    exports com.google.devtools.build.v1;
    exports com.google.devtools.common.options.proto;
    exports com.google.devtools.intellij.plugin;
}
