package org.jetbrains.bazel.sync.environment;

import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@ServiceImpl
@Singleton
public class IJBazelApplicationContextService implements BazelApplicationContextService {
    @Inject
    public IJBazelApplicationContextService() {
    }
}
