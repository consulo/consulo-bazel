package org.jetbrains.bazel.languages.starlark.parser;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

class ParsingScope {
    private final boolean myFunction;
    private final boolean mySuite;
    private boolean myAfterSemicolon;

    ParsingScope() {
        this(false, false, false);
    }

    ParsingScope(boolean isFunction, boolean isSuite, boolean isAfterSemicolon) {
        myFunction = isFunction;
        mySuite = isSuite;
        myAfterSemicolon = isAfterSemicolon;
    }

    public boolean isFunction() {
        return myFunction;
    }

    public boolean isSuite() {
        return mySuite;
    }

    public boolean isAfterSemicolon() {
        return myAfterSemicolon;
    }

    public void setAfterSemicolon(boolean isAfterSemicolon) {
        myAfterSemicolon = isAfterSemicolon;
    }

    public ParsingScope withFunction() {
        return new ParsingScope(false, mySuite, myAfterSemicolon);
    }

    public ParsingScope withSuite() {
        return new ParsingScope(myFunction, true, myAfterSemicolon);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ParsingScope that)) {
            return false;
        }
        return myFunction == that.myFunction && mySuite == that.mySuite && myAfterSemicolon == that.myAfterSemicolon;
    }

    @Override
    public int hashCode() {
        return Objects.hash(myFunction, mySuite, myAfterSemicolon);
    }

    @Override
    public String toString() {
        return "ParsingScope(isFunction=" + myFunction + ", isSuite=" + mySuite + ", isAfterSemicolon=" + myAfterSemicolon + ")";
    }
}
