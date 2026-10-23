package org.jetbrains.bsp.protocol;

import java.nio.file.Path;

public record TextDocumentIdentifier(Path path) {
}
