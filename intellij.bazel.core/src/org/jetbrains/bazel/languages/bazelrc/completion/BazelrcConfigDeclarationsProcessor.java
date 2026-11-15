package org.jetbrains.bazel.languages.bazelrc.completion;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.function.Processor;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.virtualFileSystem.VirtualFile;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcElement;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFile;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcImport;
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcLine;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

class BazelrcConfigDeclarationsProcessor implements Processor<BazelrcElement> {
    private final BazelrcLine myInputElement;
    private Set<VirtualFile> mySeenFiles = new LinkedHashSet<>();
    private Map<String, BazelrcFile> myResults = new LinkedHashMap<>();

    BazelrcConfigDeclarationsProcessor(BazelrcLine inputElement) {
        myInputElement = inputElement;
    }

    public Set<VirtualFile> getSeenFiles() {
        return mySeenFiles;
    }

    public void setSeenFiles(Set<VirtualFile> seenFiles) {
        mySeenFiles = seenFiles;
    }

    public Map<String, BazelrcFile> getResults() {
        return myResults;
    }

    public void setResults(Map<String, BazelrcFile> results) {
        myResults = results;
    }

    @Override
    @RequiredReadAction
    public boolean process(BazelrcElement currentElement) {
        if (currentElement.equals(myInputElement)) {
            return true;
        }
        processElement(currentElement);
        return true;
    }

    @RequiredReadAction
    public void processFile(@Nullable BazelrcFile file) {
        if (file == null) {
            return;
        }
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile != null) {
            if (mySeenFiles.add(virtualFile)) {
                for (BazelrcImport it : file.getImports()) {
                    processElement(it);
                }
                for (BazelrcLine it : file.getLines()) {
                    processElement(it);
                }
            }
        }
    }

    @RequiredReadAction
    public void processElement(BazelrcElement current) {
        if (current instanceof BazelrcLine line) {
            processLine(line);
        }
        else if (current instanceof BazelrcImport bazelrcImport) {
            processImport(bazelrcImport);
        }
    }

    @RequiredReadAction
    private void processLine(BazelrcLine line) {
        PsiFile containingFile = line.getContainingFile();
        if (containingFile instanceof BazelrcFile file) {
            String config = line.getConfig();
            if (config != null) {
                myResults.put(config, file);
            }
        }
    }

    @RequiredReadAction
    private void processImport(BazelrcImport element) {
        PsiReference reference = element.getReference();
        processFile(reference != null && reference.resolve() instanceof BazelrcFile file ? file : null);
    }
}
