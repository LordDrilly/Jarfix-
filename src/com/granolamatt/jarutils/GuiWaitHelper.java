/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.granolamatt.jarutils;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author root
 */
public class GuiWaitHelper {

    public static void waitForGui() throws InterruptedException {
        final Thread me = Thread.currentThread();
        final Thread shutmedown = new Thread() {
            @Override
            public void run() {
                synchronized (me) {
                    me.notify();
                }
                try {
                    me.join();
                } catch (InterruptedException ex) {
                    Logger.getLogger(GuiWaitHelper.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        Runtime.getRuntime().addShutdownHook(shutmedown);
        synchronized (me) {
            me.wait();
        }
        System.out.println("Done with hook");
    }
}
