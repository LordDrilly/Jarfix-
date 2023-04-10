/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.granolamatt.jarfixer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author root
 */
public class JarFixer {

    private static final String myJarPath = "/opt/java/jars/";

    public void copyFile(File destination) {
        try {
            InputStream in = ClassLoader.getSystemResourceAsStream("com/granolamatt/jarfixer/javaloader.c");
            OutputStream out = new FileOutputStream(destination);

            byte[] buf = new byte[1024];
            int len;

            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void buildLoader(File dir, File jarName, String jdkhome) throws IOException {
        JarFile jf = new JarFile(jarName);
        Manifest manifest = jf.getManifest();
        Attributes m = manifest.getMainAttributes();
        String jpath = m.getValue(Attributes.Name.CLASS_PATH);
        String myPath = myJarPath + jarName.getName();
        String mpath = jpath.replace("lib/", myJarPath);
        String classPath = ("\"-Djava.class.path=" + myPath + " " + mpath + "\"").replaceAll(" ", ":");
        System.out.println("Classpath is: " + classPath);
        classPath = "#define JAVA_CLASSPATH " + classPath;
        String main = "#define JAVA_MAIN_CLASS \"" + m.getValue(Attributes.Name.MAIN_CLASS).replaceAll("\\.", "/") + "\"";

        String cname = dir.getAbsolutePath() + "/javaloader.c";
        String hname = dir.getAbsolutePath() + "/javaloader.h";
        String binName = jf.getName().substring(0, jf.getName().length() - 4);
        System.out.println("C file is " + cname + "\nH file is " + hname);
        File cfile = new File(cname);
        cfile.delete();
        copyFile(cfile);

        File hfile = new File(hname);
        hfile.delete();
        OutputStream out = new FileOutputStream(hfile);
        BufferedWriter lineOut = new BufferedWriter(new OutputStreamWriter(out));
        lineOut.write(classPath);
        lineOut.newLine();
        lineOut.write(main);
        lineOut.newLine();
        lineOut.newLine();
        lineOut.close();

        ProcessBuilder pb;
        pb = new ProcessBuilder("/usr/bin/gcc", "-o", binName, "-O2", "-I" + jdkhome +"/include/", "-I" + jdkhome +"/include/linux", cname, "-ljvm");
        pb.directory(dir);
        Process p = pb.start();
        try {
            int ans = p.waitFor();
        } catch (InterruptedException ex) {
            Logger.getLogger(JarFixer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public List<File> cleanPath(String path, String jdkhome) throws IOException {
        File file;
        if (path.startsWith("/")) {
            file = new File(path);
        } else {
            File currentDir = new File("");
            System.out.println("My path is " + currentDir.getAbsolutePath());
            file = new File(currentDir.getAbsolutePath());
        }
        boolean found = false;
        List<File> cleanList = new LinkedList<>();
        if (file.isDirectory() && file.getName().equals("dist")) {
            found = true;
        } else {
            File[] directories = file.listFiles();
            if (directories != null) {
                for (File dname : directories) {
                    if (dname.isDirectory() && dname.getName().equals("dist")) {
                        found = true;
                        file = dname;
                        break;
                    }
                }
            }
        }
        if (found) {
            File[] directories = file.listFiles();
            if (directories != null) {
                for (File dname : directories) {
                    if (dname.isFile() && dname.getName().contains(".jar")) {
                        cleanList.add(dname);
                        buildLoader(file, dname, jdkhome);
                    }
                }
            }
        }
        return cleanList;
    }

    private void configureArgs(String[] args) throws IOException {
        final OptionListing.RetClass[] argv = OptionListing.parseAllOptions(args);
        String jdkhome = System.getenv("JDK_HOME");
        String jname = "";

        for (OptionListing.RetClass arg : argv) {
            try {
                switch (arg.index) {
                    case 0:
                        jdkhome = arg.args[0];
                        break;
                    case 1:
                        jname = arg.args[0];
                        break;
                }
            } catch (NumberFormatException ex) {
                System.out.println("Number was off");
            }
        }
        System.out.println("Using JDK_HOME " + jdkhome);
        if (jdkhome == null) {
            System.out.println("JDK_HOME Not set, Either set JDK_HOME env or use -j option ");
            System.out.println("Using /usr/local/jdk but this may fail");
            jdkhome = "/usr/local/jdk";
        }
        
        List<File> cleanList = cleanPath(jname, jdkhome);
        for (File files : cleanList) {
            System.out.println("Clean and made loader for " + files.getName());
        }

    }

    public static void main(String[] args) throws IOException {
        JarFixer jf = new JarFixer();
        jf.configureArgs(args);
        System.exit(0);
    }
}
