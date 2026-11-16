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
package consulo.bazel.execution.coverage;

import consulo.util.io.FileUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Consulo-only bridge for IntelliJ's {@code com.intellij.coverage.lcov.LcovSerializationUtils} / {@code LcovCoverageReport}
 * (missing in Consulo), used by {@code LcovBazelCoverageReportParser}. Only the reading part is ported: {@code SF:} starts a
 * file record, {@code DA:<line>,<hits>[,<checksum>]} adds line hits, {@code end_of_record} merges the record into the report.
 *
 * @author VISTALL
 * @since 2026-11-16
 */
public final class LcovSerializationUtils {
    private static final String SOURCE_FILE_PREFIX = "SF:";
    private static final String LINE_HIT_PREFIX = "DA:";
    private static final String END_OF_RECORD = "end_of_record";

    private LcovSerializationUtils() {
    }

    public static LcovCoverageReport readLCOVFromPaths(List<Path> paths) throws IOException {
        LcovCoverageReport report = new LcovCoverageReport();
        for (Path path : paths) {
            readLCOV(report, path);
        }
        return report;
    }

    private static void readLCOV(LcovCoverageReport report, Path path) throws IOException {
        String currentFileName = null;
        List<LineHits> lineDataList = null;
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(SOURCE_FILE_PREFIX)) {
                    currentFileName = line.substring(SOURCE_FILE_PREFIX.length());
                    lineDataList = new ArrayList<>();
                }
                else if (line.startsWith(LINE_HIT_PREFIX)) {
                    if (lineDataList == null) {
                        throw new IOException("Unexpected " + LINE_HIT_PREFIX + " outside of a " + SOURCE_FILE_PREFIX + " record");
                    }
                    String[] values = line.substring(LINE_HIT_PREFIX.length()).split(",");
                    if (values.length < 2) {
                        throw new IOException("Expected at least 2 values, got " + values.length + ": " + line);
                    }
                    int lineNumber;
                    try {
                        lineNumber = Integer.parseInt(values[0].trim());
                    }
                    catch (NumberFormatException e) {
                        continue;
                    }
                    long hits;
                    try {
                        hits = Long.parseLong(values[1].trim());
                    }
                    catch (NumberFormatException e) {
                        hits = 0;
                    }
                    lineDataList.add(new LineHits(lineNumber, hits));
                }
                else if (END_OF_RECORD.equals(line)) {
                    if (currentFileName != null && lineDataList != null) {
                        report.mergeFileReport(currentFileName, lineDataList);
                    }
                    currentFileName = null;
                    lineDataList = null;
                }
            }
        }
        if (lineDataList != null) {
            throw new IOException("Unexpected end of LCOV data in " + path);
        }
    }

    public static final class LcovCoverageReport {
        private final Map<String, List<LineHits>> myInfo = new LinkedHashMap<>();

        /**
         * File path (system independent, as written in the report) to its line hits, sorted by line number.
         */
        public Map<String, List<LineHits>> getInfo() {
            return myInfo;
        }

        public void mergeFileReport(String filePath, List<LineHits> report) {
            String normalizedFilePath = FileUtil.toSystemIndependentName(filePath);
            Map<Integer, LineHits> merged = new TreeMap<>();
            List<LineHits> oldReport = myInfo.get(normalizedFilePath);
            if (oldReport != null) {
                for (LineHits lineHits : oldReport) {
                    merged.put(lineHits.getLineNumber(), lineHits);
                }
            }
            for (LineHits lineHits : report) {
                LineHits existing = merged.get(lineHits.getLineNumber());
                if (existing == null) {
                    merged.put(lineHits.getLineNumber(), new LineHits(lineHits.getLineNumber(), lineHits.getHits()));
                }
                else {
                    existing.addHits(lineHits.getHits());
                }
            }
            List<LineHits> result = new ArrayList<>(merged.values());
            result.sort(Comparator.comparingInt(LineHits::getLineNumber));
            myInfo.put(normalizedFilePath, result);
        }
    }

    public static final class LineHits {
        private final int myLineNumber;
        private long myHits;

        public LineHits(int lineNumber, long hits) {
            myLineNumber = lineNumber;
            myHits = hits;
        }

        public int getLineNumber() {
            return myLineNumber;
        }

        public long getHits() {
            return myHits;
        }

        public void addHits(long hits) {
            myHits += hits;
        }
    }
}
