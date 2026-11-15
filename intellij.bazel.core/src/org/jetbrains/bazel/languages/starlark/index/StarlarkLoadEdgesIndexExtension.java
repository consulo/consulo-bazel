package org.jetbrains.bazel.languages.starlark.index;

import consulo.annotation.component.ExtensionImpl;
import consulo.index.io.DataIndexer;
import consulo.index.io.EnumeratorStringDescriptor;
import consulo.index.io.ID;
import consulo.index.io.data.DataExternalizer;
import consulo.index.io.data.DataInputOutputUtil;
import consulo.language.psi.PsiFile;
import consulo.language.psi.stub.DefaultFileTypeSpecificInputFilter;
import consulo.language.psi.stub.FileBasedIndexExtension;
import consulo.language.psi.stub.FileContent;
import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage.StarlarkFileType;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ExtensionImpl
public class StarlarkLoadEdgesIndexExtension extends FileBasedIndexExtension<String, Collection<String>> {
    @Override
    public ID<String, Collection<String>> getName() {
        return StarlarkLoadEdgesIndex.NAME;
    }

    @Override
    public boolean dependsOnFileContent() {
        return true;
    }

    @Override
    public EnumeratorStringDescriptor getKeyDescriptor() {
        return EnumeratorStringDescriptor.INSTANCE;
    }

    @Override
    public DataExternalizer<Collection<String>> getValueExternalizer() {
        return new CollectionDataExternalizer<>(EnumeratorStringDescriptor.INSTANCE);
    }

    @Override
    public DefaultFileTypeSpecificInputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(StarlarkFileType.INSTANCE);
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    public DataIndexer<String, Collection<String>, FileContent> getIndexer() {
        return fileContent -> {
            PsiFile file = fileContent.getPsiFile();
            if (!file.getName().endsWith(".bzl")) {
                return Map.of();
            }

            // consulo: Kotlin mapNotNull { ... }.filter { ... }.distinct()
            Set<String> loadLabels = new LinkedHashSet<>();
            for (StarlarkLoadStatement it : PsiTreeUtil.getChildrenOfTypeAsList(file, StarlarkLoadStatement.class)) {
                StarlarkStringLiteralExpression loadedFileNamePsi = it.getLoadedFileNamePsi();
                String loadLabel = loadedFileNamePsi == null ? null : loadedFileNamePsi.getStringContents();
                if (loadLabel != null && loadLabel.endsWith(".bzl")) {
                    loadLabels.add(loadLabel);
                }
            }

            return loadLabels.isEmpty() ? Map.of() : Map.of(fileContent.getFile().getUrl(), List.copyOf(loadLabels));
        };
    }

    // consulo: upstream uses com.intellij.util.indexing.impl.CollectionDataExternalizer, which Consulo does not have
    private static class CollectionDataExternalizer<T> implements DataExternalizer<Collection<T>> {
        private final DataExternalizer<T> myElementExternalizer;

        CollectionDataExternalizer(DataExternalizer<T> elementExternalizer) {
            myElementExternalizer = elementExternalizer;
        }

        @Override
        public void save(DataOutput out, Collection<T> value) throws IOException {
            DataInputOutputUtil.writeSeq(out, value, it -> myElementExternalizer.save(out, it));
        }

        @Override
        public Collection<T> read(DataInput in) throws IOException {
            return DataInputOutputUtil.readSeq(in, () -> myElementExternalizer.read(in));
        }
    }
}
