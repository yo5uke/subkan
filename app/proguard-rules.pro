# Room generates implementations reflectively named after the @Database class; R8 keeps them via
# the AGP-supplied rules. Nothing app-specific is needed here today.
#
# Coil 3's OkHttp backend ships its own consumer rules.

# Keep line numbers so a stack trace from a release build is still readable after deobfuscation.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
