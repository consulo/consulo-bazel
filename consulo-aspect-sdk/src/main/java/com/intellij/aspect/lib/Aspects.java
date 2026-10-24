/*
 * Copyright 2026 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.aspect.lib;

import java.util.List;
import java.util.Set;

// consulo: declared in upstream sdk/lib/aspectchoice.kt

/**
 * @deprecated Use ASPECT_NAME to obtain the name of the only aspect
 */
@Deprecated
public enum Aspects {
    INTELLIJ("config", "aspect.bzl", "intellij_aspect");

    private final String myPkg;
    private final String myFile;
    private final String myAspect;

    Aspects(String pkg, String file, String aspect) {
        myPkg = pkg;
        myFile = file;
        myAspect = aspect;
    }

    public String getPkg() {
        return myPkg;
    }

    public String getFile() {
        return myFile;
    }

    public String getAspect() {
        return myAspect;
    }

    @Override
    public String toString() {
        return myPkg + ":" + myFile + "%" + myAspect;
    }

    /**
     * For the specified rulesets, returns the list of aspects to be run in the correct order.
     */
    public static List<Aspects> forRules(Set<Rules> languages) {
        return List.of(INTELLIJ);
    }
}
