/* Copyright 2019 Norconex Inc.
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
package org.apache.pdfbox.jbig2.util.log;

/**
 * Minimal logging interface used by the local SLF4J bridge.
 *
 * This mirrors the historical PDFBox JBig2 logging API to keep
 * the bridge source-compatible with newer dependencies.
 */
public interface Logger {

    void debug(String msg);

    void debug(String msg, Throwable t);

    void info(String msg);

    void info(String msg, Throwable t);

    void warn(String msg);

    void warn(String msg, Throwable t);

    void fatal(String msg);

    void fatal(String msg, Throwable t);

    void error(String msg);

    void error(String msg, Throwable t);

    boolean isDebugEnabled();

    boolean isInfoEnabled();

    boolean isWarnEnabled();

    boolean isFatalEnabled();

    boolean isErrorEnabled();
}
