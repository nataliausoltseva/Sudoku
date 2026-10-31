# Project-specific ProGuard / R8 rules for Sudoku

# Preserve line numbers and source file attributes for readable stack traces in crash logs
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserve type annotations and generic signatures used by Kotlin coroutines and reflection
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Optimization directives
-repackageclasses ''
-allowaccessmodification
