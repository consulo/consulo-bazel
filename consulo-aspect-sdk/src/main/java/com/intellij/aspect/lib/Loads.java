/*
 * Copyright 2026 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.aspect.lib;

import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * consulo: top-level functions of upstream sdk/lib/loads.kt. The types of that file are
 * {@link Transformer}, {@link LoadStatement} and {@link Repository}.
 */
public final class Loads {
    private static final Pattern LOAD_STATEMENT_RX = Pattern.compile("^load\\(\"([^\"]+)\",([^)]+)\\)$");

    private static final Pattern REPO_NAME_RX = Pattern.compile("^(@[^/]+)//(.+)$");

    private Loads() {
    }

    public static List<LoadStatement> parseLoads(InputStream input) throws IOException {
        List<LoadStatement> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LoadStatement stmt = loadStatementParse(line);
                if (stmt != null) {
                    result.add(stmt);
                }
            }
        }
        return result;
    }

    public static String transformFile(InputStream input, List<Transformer> transformers) throws IOException {
        List<LoadStatement> loads = new ArrayList<>();
        List<String> lines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LoadStatement stmt = loadStatementParse(line);
                if (stmt == null) {
                    lines.add(line);
                }
                else {
                    loads.add(stmt);
                }
            }
        }

        for (Transformer transformer : transformers) {
            transformer.apply(loads, lines);
        }

        StringBuilder builder = new StringBuilder();
        builder.append(loads.stream().map(Loads::loadStatementWrite).collect(Collectors.joining("\n")));
        builder.append("\n");
        builder.append(String.join("\n", lines));
        return builder.toString();
    }

    private static @Nullable LoadStatement loadStatementParse(String line) throws IOException {
        Matcher stmtMatch = LOAD_STATEMENT_RX.matcher(line);
        if (!stmtMatch.matches()) {
            return null;
        }
        String module = stmtMatch.group(1);
        String arguments = stmtMatch.group(2).trim();

        if (module.startsWith(":")) {
            return new LoadStatement(Repository.Relative.INSTANCE, module, arguments);
        }

        if (module.startsWith("//")) {
            return new LoadStatement(Repository.Absolute.INSTANCE, module.substring(2), arguments);
        }

        Matcher moduleMatch = REPO_NAME_RX.matcher(module);
        if (!moduleMatch.matches()) {
            throw new IOException("invalid module identifier: " + module);
        }

        return new LoadStatement(
            new Repository.External(moduleMatch.group(1)),
            moduleMatch.group(2),
            arguments
        );
    }

    private static String loadStatementWrite(LoadStatement stmt) {
        String repository = switch (stmt.repository()) {
            case Repository.Absolute absolute -> "//";
            case Repository.Relative relative -> "";
            case Repository.External external -> external.name() + "//";
        };

        return "load(\"" + repository + stmt.path() + "\", " + stmt.arguments() + ")";
    }
}
