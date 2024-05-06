package com.example.mywebview.web;

public class WebFlag {

    private volatile static boolean qbdWebviewInit = false;

    public static synchronized boolean isQbdWebviewInit() {
        return qbdWebviewInit;
    }

    public static synchronized void setQbdWebviewInit(boolean qbdWebviewInit) {
        WebFlag.qbdWebviewInit = qbdWebviewInit;
    }

}
