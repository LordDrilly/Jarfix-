JarFixer
========

The most annoying thing is to have two programs running and they are bothed named "java".  To fix this, I created a jarfixer program.  This is a c wrapper around jvm.  It requires the jdk to be at /usr/local/jdk by default and libjvm.so in the ldconfig path.  Then, all jdk binaries go in /opt/java/bin and all jars for those binaries in /opt/java/jars
