package org.example.library;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** a library's own annotation, read at run time the way log4j reads its plugins */
@Retention(RetentionPolicy.RUNTIME)
public @interface Named {
    String value();
}
