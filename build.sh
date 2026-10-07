#!/bin/sh
#
# Compile and run the Village SACCO application without Eclipse.
#
# COS 3103 Topic 2 - Compiling and Interpreting Applications.
#   javac  = the compiler.   Turns .java source into .class bytecode.
#   java   = the launcher.   Starts the JVM, which interprets/JIT-compiles bytecode.
#   -d bin = put the .class files under bin/, mirroring the package folders.
#   -cp    = the CLASSPATH: where the JVM looks for classes (Topic 9).
#
# Usage:
#   ./build.sh            compile, then run
#   ./build.sh compile    compile only
#   ./build.sh run        run only (assumes bin/ is up to date)
#   ./build.sh clean      delete bin/
#   ./build.sh demo       compile, then run with sample data
#
set -e

SRC_DIR=src
OUT_DIR=bin
MAIN_CLASS=sacco.app.SaccoApp

compile() {
    echo "Compiling..."
    mkdir -p "$OUT_DIR"
    # The file list is built with find so new source files are picked up
    # automatically. @file tells javac to read arguments from that file - handy
    # because a long command line can exceed the shell's limit.
    find "$SRC_DIR" -name "*.java" > .sources.txt
    javac -Xlint:all -d "$OUT_DIR" @.sources.txt
    rm -f .sources.txt
    echo "Compiled $(find "$OUT_DIR" -name '*.class' | wc -l | tr -d ' ') class files into $OUT_DIR/"
}

run() {
    # The fully-qualified class name, NOT a file path: the JVM resolves
    # sacco.app.SaccoApp to bin/sacco/app/SaccoApp.class via the classpath.
    java -cp "$OUT_DIR" "$MAIN_CLASS" "$@"
}

case "${1:-all}" in
    compile) compile ;;
    run)     shift; run "$@" ;;
    clean)   rm -rf "$OUT_DIR" .sources.txt; echo "Removed $OUT_DIR/" ;;
    demo)    compile; run --demo ;;
    all)     compile; run ;;
    *)       echo "Usage: $0 [compile|run|clean|demo]"; exit 1 ;;
esac
