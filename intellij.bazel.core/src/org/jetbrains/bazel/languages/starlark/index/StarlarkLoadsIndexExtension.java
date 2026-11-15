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
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement;
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadStatement;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkLoadValue;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkNamedLoadValue;
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkStringLoadValue;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ExtensionImpl
public class StarlarkLoadsIndexExtension extends FileBasedIndexExtension<String, Collection<String>> {
    @Override
    public ID<String, Collection<String>> getName() {
        return StarlarkLoadsIndex.NAME;
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
            Map<String, List<String>> indexes = new HashMap<>();
            PsiFile file = fileContent.getPsiFile();
            List<StarlarkLoadStatement> loadStatements = PsiTreeUtil.getChildrenOfTypeAsList(file, StarlarkLoadStatement.class);
            for (StarlarkLoadStatement loadStatement : loadStatements) {
                StarlarkStringLiteralExpression loadedFileNamePsi = loadStatement.getLoadedFileNamePsi();
                String loadFileName = loadedFileNamePsi == null ? null : loadedFileNamePsi.getStringContents();
                if (loadFileName == null) {
                    continue;
                }
                for (StarlarkElement loadValue : loadStatement.getLoadedSymbolsPsi()) {
                    String symbolName;
                    if (loadValue instanceof StarlarkStringLoadValue || loadValue instanceof StarlarkNamedLoadValue) {
                        symbolName = ((StarlarkLoadValue) loadValue).getLoadValueExpressionContent();
                    }
                    else {
                        symbolName = null;
                    }
                    if (symbolName == null) {
                        continue;
                    }
                    List<String> loadFileNames = indexes.computeIfAbsent(symbolName, k -> new ArrayList<>());
                    loadFileNames.add(loadFileName);
                }
            }
            // consulo: Kotlin read-only maps are covariant, Java needs a copy typed Map<String, Collection<String>>
            return new HashMap<>(indexes);
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
