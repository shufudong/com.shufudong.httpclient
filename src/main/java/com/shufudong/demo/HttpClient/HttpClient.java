package com.shufudong.demo.HttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shufudong.demo.HttpClient.Exception.HttpClientException;
import com.shufudong.demo.HttpClient.Util.HttpUtil;
import com.shufudong.demo.HttpClient.Util.URLConnector;
import com.shufudong.lang.hash.Base64;
import com.shufudong.lang.util.LoggerUtil;
import com.shufudong.lang.util.ObjectUtil;

/** 
* @ClassName:   [中]HttpClient 
* @Description: [中]发送HTTP GET/POST 请求
* @author       [中]ShuFuDong
* @date         [中]2015年9月4日 上午12:09:18 
*/ 
public class HttpClient {
    
    private final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private int timeout = 30000;
    private boolean debug = false;
    private boolean lineFeed = true;
    private boolean trustAny = false;
    private boolean followRedirects = true;
    private boolean keepAlive = false;

    private String contentType = null;
    private String streamCharset = null;
    private String url = null;
    private String rawStream = null;
    private String requestCharset = "UTF-8";

    private String basicAuthUsername = null;
    private String basicAuthPassword = null;

    private Map<String, Object> parameters = null;
    private Map<String, String> headers = null;

    private URL requestUrl = null;
    private URLConnection con = null;

    /** Creates an empty HttpClient object. */
    
    /** 
    * [中]创建一个空的HttpClient对象
    */
    public HttpClient() {}

    /** Creates a new HttpClient object. */
    /** 
    * [中]创建一个新的HttpClient对象
    * @param url    [中]访问地址    
    */
    public HttpClient(URL url) {
        this.url = url.toExternalForm();
    }

    /** 
    * [中]创建一个新的HttpClient对象
    * @param url    [中]访问地址
    */
    public HttpClient(String url) {
        this.url = url;
    }

    /** 
    * [中]创建一个新的HttpClient对象
    * @param url            [中]访问地址
    * @param parameters     [中]请求参数
    */
    public HttpClient(String url, Map<String, Object> parameters) {
        this.url = url;
        this.parameters = parameters;
    }

    /** 
    * [中]创建一个新的HttpClient对象
    * @param url            [中]访问地址
    * @param parameters     [中]请求参数
    */
    public HttpClient(URL url, Map<String, Object> parameters) {
        this.url = url.toExternalForm();
        this.parameters = parameters;
    }

    /** 
    * [中]创建一个新的HttpClient对象
    * @param url            [中]访问地址
    * @param parameters     [中]请求参数
    * @param headers        [中]请求报文头信息
    */
    public HttpClient(String url, Map<String, Object> parameters, Map<String, String> headers) {
        this.url = url;
        this.parameters = parameters;
        this.headers = headers;
    }

    /** 
    * [中]创建一个新的HttpClient对象
    * @param url            [中]访问地址
    * @param parameters     [中]请求参数
    * @param headers        [中]请求报文头信息
    */
    public HttpClient(URL url, Map<String, Object> parameters, Map<String, String> headers) {
        this.url = url.toExternalForm();
        this.parameters = parameters;
        this.headers = headers;
    }

    /** When true overrides Debug.verboseOn() and forces debugging for this instance */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /** Sets the timeout for waiting for the connection (default 30sec) */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /** Enables this request to follow redirect 3xx codes (default true) */
     public void followRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /** Turns on or off line feeds in the request. (default is on) */
    public void setLineFeed(boolean lineFeed) {
        this.lineFeed = lineFeed;
    }

    /** Set the raw stream for posts. */
    public void setRawStream(String stream) {
        this.rawStream = stream;
    }

    /** Set the URL for this request. */
    public void setUrl(URL url) {
        this.url = url.toExternalForm();
    }

    /** Set the URL for this request. */
    public void setUrl(String url) {
        this.url = url;
    }

    /** Set the parameters for this request. */
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    /** Set an individual parameter for this request. */
    public void setParameter(String name, String value) {
        if (parameters == null)
            parameters = new HashMap<String, Object>();
        parameters.put(name, value);
    }

    /** Set the headers for this request. */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /** Set an individual header for this request. */
    public void setHeader(String name, String value) {
        if (headers == null)
            headers = new HashMap<String, String>();
        headers.put(name, value);
    }

    /** Return a Map of headers. */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /** Return a Map of parameters. */
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /** Return a string representing the requested URL. */
    public String getUrl() {
        return url;
    }

    /** Sets the content-type */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /** Returns the content type */
    public String getContentType() {
        return this.contentType;
    }
    
    /** Sets the scream charset */
    public void setStreamCharset(String streamCharset) {
        this.streamCharset = streamCharset;
    }
    
    /** Returns the stream charset */
    public String getStreamCharset() {
        return this.streamCharset;
    }

    /** Toggle keep-alive setting */
    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    /** Return keep-alive setting */
    public boolean getKeepAlive() {
        return this.keepAlive;
    }

    /** Allow untrusted server certificates */
    public void setAllowUntrusted(boolean trustAny) {
        this.trustAny = trustAny;
    }

    /** Do we trust any certificate */
    public boolean getAllowUntrusted() {
        return this.trustAny;
    }

    public void setBasicAuthInfo(String basicAuthUsername, String basicAuthPassword) {
        this.basicAuthUsername = basicAuthUsername;
        this.basicAuthPassword = basicAuthPassword;
    }

    /** Invoke HTTP request GET. */
    public String get() throws HttpClientException {
        return sendHttpRequest("get");
    }

    /** Invoke HTTP request GET. */
    public InputStream getStream() throws HttpClientException {
        return sendHttpRequestStream("get");
    }

    /** Invoke HTTP request POST. */
    public String post() throws HttpClientException {
        return sendHttpRequest("post");
    }

    /** Invoke HTTP request POST and pass raw stream. */
    public String post(String stream) throws HttpClientException {
        this.rawStream = stream;
        return sendHttpRequest("post");
    }

    /** Invoke HTTP request POST. */
    public InputStream postStream() throws HttpClientException {
        return sendHttpRequestStream("post");
    }

    /** Returns the value of the specified named response header field. */
    public String getResponseHeader(String header) throws HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        return con.getHeaderField(header);
    }

    /** Returns the key for the nth response header field. */
    public String getResponseHeaderFieldKey(int n) throws HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        return con.getHeaderFieldKey(n);
    }

    /** Returns the value for the nth response header field. It returns null of there are fewer then n fields. */
    public String getResponseHeaderField(int n) throws HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        return con.getHeaderField(n);
    }

    /** Returns the content of the response. */
    public Object getResponseContent() throws java.io.IOException, HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        return con.getContent();
    }

    /** Returns the content-type of the response. */
    public String getResponseContentType() throws HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        return con.getContentType();
    }

    /** Returns the content length of the response */
    public int getResponseContentLength() throws HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        return con.getContentLength();
    }

    /** Returns the content encoding of the response. */
    public String getResponseContentEncoding() throws HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        return con.getContentEncoding();
    }

    public int getResponseCode() throws HttpClientException {
        if (con == null) {
            throw new HttpClientException("Connection not yet established");
        }
        if (!(con instanceof HttpURLConnection)) {
            throw new HttpClientException("Connection is not HTTP; no response code");
        }

        try {
            return ((HttpURLConnection) con).getResponseCode();
        } catch (IOException e) {
            throw new HttpClientException(this.getClass(),e);
        }
    }

    public String sendHttpRequest(String method) throws HttpClientException {
        InputStream in = sendHttpRequestStream(method);
        if (in == null) return null;

        StringBuilder buf = new StringBuilder();
        try {
            if (debug) {
                try {
                    LoggerUtil.debug(logger, "ContentEncoding: " + con.getContentEncoding() + "; ContentType: " +
                            con.getContentType() + " or: " + URLConnection.guessContentTypeFromStream(in));
                } catch (IOException ioe) {
                    LoggerUtil.error(logger, ioe, "Caught exception printing content debugging information");
                }
            }

            String charset = null;
            String contentType = con.getContentType();
            if (contentType == null) {
                try {
                    contentType = URLConnection.guessContentTypeFromStream(in);
                } catch (IOException ioe) {
                    LoggerUtil.error(logger, ioe, "Problems guessing content type from steam");
                }
            }

            if (debug) {
                LoggerUtil.debug(logger, "Content-Type: " + contentType);
            }
            
            if (contentType != null) {
                contentType = contentType.toUpperCase();
                int charsetEqualsLoc = contentType.indexOf("=", contentType.indexOf("CHARSET"));
                int afterSemiColon = contentType.indexOf(";", charsetEqualsLoc);
                if (charsetEqualsLoc >= 0 && afterSemiColon >= 0) {
                    charset = contentType.substring(charsetEqualsLoc + 1, afterSemiColon);
                } else if (charsetEqualsLoc >= 0) {
                    charset = contentType.substring(charsetEqualsLoc + 1);
                }

                if (charset != null) charset = charset.trim().replaceAll("\"", "");
                if (debug) {
                    LoggerUtil.debug(logger, "Getting text from HttpClient with charset: " + charset);
                }
            }

            BufferedReader post = new BufferedReader(charset == null ? new InputStreamReader(in) : new InputStreamReader(in, charset));
            String line = "";

            if (debug) {
                LoggerUtil.debug(logger, "---- HttpClient Response Content ----");
            }
            
            while ((line = post.readLine()) != null) {
                if (debug) {
                    LoggerUtil.debug(logger, "[HttpClient] : " + line);
                }
                buf.append(line);
                if (lineFeed) {
                    buf.append("\n");
                }
            }
        } catch (Exception e) {
            LoggerUtil.error(logger, e, "Error processing input stream");
            throw new HttpClientException(this.getClass(),e);
        }
        return buf.toString();
    }

    private InputStream sendHttpRequestStream(String method) throws HttpClientException {

        String arguments = null;
        InputStream in = null;

        if (url == null) {
            throw new HttpClientException("Cannot process a null URL.");
        }

        if (rawStream != null) {
            arguments = rawStream;
        } else if (ObjectUtil.isNotEmpty(parameters)) {
            arguments = HttpUtil.urlEncodeArgs(parameters,requestCharset,false);
        }

        // Append the arguments to the query string if GET.
        if (method.equalsIgnoreCase("get") && arguments != null) {
            if (url.contains("?")) {
                url = url + "&" + arguments;
            } else {
                url = url + "?" + arguments;
            }
        }

        // Create the URL and open the connection.
        try {
            requestUrl = new URL(url);
            con = URLConnector.openConnection(requestUrl, timeout);
            
            if (debug) {
                LoggerUtil.debug(logger, "Connection opened to : " + requestUrl.toExternalForm());
            }

            if ((con instanceof HttpURLConnection)) {
                ((HttpURLConnection) con).setInstanceFollowRedirects(followRedirects);
                if (debug) {
                    LoggerUtil.debug(logger, "Connection is of type HttpURLConnection, more specifically: " + con.getClass().getName());
                }
            }

            // set the content type
            if (contentType != null) {
                con.setRequestProperty("Content-type", contentType);
            }

            // connection settings
            con.setDoOutput(true);
            con.setUseCaches(false);
            if (keepAlive) {
                con.setRequestProperty("Connection", "Keep-Alive");
            }

            if (method.equalsIgnoreCase("post")) {
                if (contentType == null) {
                    con.setRequestProperty("Content-type", "application/x-www-form-urlencoded");
                }
                con.setDoInput(true);
            }

            // if there is basicAuth info set the request property for it
            if (basicAuthUsername != null) {
                String basicAuthString = "Basic " + Base64.base64Encode(basicAuthUsername + ":" + (basicAuthPassword == null ? "" : basicAuthPassword));
                con.setRequestProperty("Authorization", basicAuthString);
                if (debug) {
                    LoggerUtil.debug(logger, "Header - Authorization: " + basicAuthString);
                }
            }

            if (ObjectUtil.isNotEmpty(headers)) {
                for (Map.Entry<String, String> entry: headers.entrySet()) {
                    String headerName = entry.getKey();
                    String headerValue = entry.getValue();
                    con.setRequestProperty(headerName, headerValue);
                    if (debug) {
                        LoggerUtil.debug(logger, "Header - " + headerName + ": " + headerValue);
                    }
                }
            }

            if (method.equalsIgnoreCase("post")) {
                OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream(), this.streamCharset != null ? this.streamCharset : "UTF-8");
                if (debug) {
                    LoggerUtil.debug(logger, "Opened output stream");
                }
                if (arguments != null) {
                    out.write(arguments);
                    if (debug) {
                        LoggerUtil.debug(logger, "Wrote arguements (parameters) : " + arguments);
                    }
                }
                out.flush();
                out.close();
                if (debug) {
                    LoggerUtil.debug(logger, "Flushed and closed buffer");
                }
            }
            if (debug) {
                Map<String, List<String>> headerFields = con.getHeaderFields();
                LoggerUtil.debug(logger, "Header Fields : " + headerFields);
            }
            in = con.getInputStream();
        } catch (IOException ioe) {
            LoggerUtil.error(logger, ioe, "");
            throw new HttpClientException(this.getClass(),ioe);
        } catch (Exception e) {
            LoggerUtil.error(logger, e, "");
            throw new HttpClientException(this.getClass(), e);
        }
        return in;
    }

    public static String getUrlContent(String url) throws HttpClientException {
        HttpClient client = new HttpClient(url);
        return client.get();
    }

    public static int checkHttpRequest(String url) throws HttpClientException {
        HttpClient client = new HttpClient(url);
        client.get();
        return client.getResponseCode();
    }
}
