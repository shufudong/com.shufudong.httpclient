package com.shufudong.demo.HttpClient.Util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shufudong.lang.util.LoggerUtil;

public class URLConnector {
    
    private final Logger logger = LoggerFactory.getLogger(URLConnector.class);

    private URLConnection connection = null;
    private URL url = null;
    private boolean timedOut = false;

    protected URLConnector() {}
    protected URLConnector(URL url) {
        this.url = url;
    }

    protected synchronized URLConnection openConnection(int timeout) throws IOException {
        Thread t = new Thread(new URLConnectorThread());
        t.start();

        try {
            this.wait(timeout);
        } catch (InterruptedException e) {
            if (connection == null) {
                timedOut = true;
            } else {
                close(connection);
            }
            throw new IOException("Connection never established");
        }

        if (connection != null) {
            return connection;
        } else {
            timedOut = true;
            throw new IOException("Connection timed out");
        }
    }

    public static URLConnection openConnection(URL url) throws IOException {
        return openConnection(url, 30000);
    }

    public static URLConnection openConnection(URL url, int timeout) throws IOException {
        URLConnector uc = new URLConnector(url);
        return uc.openConnection(timeout);
    }

    private class URLConnectorThread implements Runnable {
        @Override
        public void run() {
            URLConnection con = null;
            try {
                con = url.openConnection();
            } catch (IOException e) {
                LoggerUtil.error(logger, e, e.getMessage());
            }

            synchronized (URLConnector.this) {
                if (timedOut && con != null) {
                    close(con);
                } else {
                    connection = con;
                    URLConnector.this.notify();
                }
            }
        }
    }

    private static void close(URLConnection con) {
        if (con instanceof HttpURLConnection) {
            ((HttpURLConnection) con).disconnect();
        }
    }
}
