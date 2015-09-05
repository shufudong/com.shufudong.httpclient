package com.shufudong.demo.HttpClient.Util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.shufudong.demo.HttpClient.Exception.HttpUtilException;
import com.shufudong.lang.util.LoggerUtil;
import com.shufudong.lang.util.ObjectUtil;
import com.shufudong.lang.util.StringUtil;

public class HttpUtil {

    private final static Logger logger = LoggerFactory
            .getLogger(HttpUtil.class);

    public static final String MULTI_ROW_DELIMITER = "_o_";
    public static final String ROW_SUBMIT_PREFIX = "_rowSubmit_o_";
    public static final String COMPOSITE_DELIMITER = "_c_";
    public static final int MULTI_ROW_DELIMITER_LENGTH = MULTI_ROW_DELIMITER
            .length();
    public static final int ROW_SUBMIT_PREFIX_LENGTH = ROW_SUBMIT_PREFIX
            .length();
    public static final int COMPOSITE_DELIMITER_LENGTH = COMPOSITE_DELIMITER
            .length();

    public static Map<String, Object> getCombinedMap(HttpServletRequest request) {
        return getCombinedMap(request, null);
    }

    public static Map<String, Object> getCombinedMap(
            HttpServletRequest request, Set<? extends String> namesToSkip) {
        Map<String, Object> combinedMap = new ConcurrentHashMap<String,Object>();
        combinedMap.putAll(getParameterMap(request)); // parameters override
                                                      // nothing
        combinedMap.putAll(getServletContextMap(request, namesToSkip)); // bottom
                                                                        // level
                                                                        // application
                                                                        // attributes
        combinedMap.putAll(getSessionMap(request, namesToSkip)); // session
                                                                 // overrides
                                                                 // application
        combinedMap.putAll(getAttributeMap(request)); // attributes trump them
                                                      // all

        return combinedMap;
    }

    public static Map<String, Object> getParameterMap(HttpServletRequest request) {
        return getParameterMap(request, null, null);
    }
    
    public static Map<String, Object> getParameterMap(
            HttpServletRequest request, Set<? extends String> nameSet) {
        return getParameterMap(request, nameSet, null);
    }

    public static Map<String, Object> getParameterMap(
            HttpServletRequest request, Set<? extends String> nameSet,
            Boolean onlyIncludeOrSkip) {
        boolean onlyIncludeOrSkipPrim = onlyIncludeOrSkip == null ? true
                : onlyIncludeOrSkip.booleanValue();
        Map<String, Object> paramMap = new HashMap<String, Object>();

        // add all the actual HTTP request parameters
        Enumeration<String> e = GenericsUtil.cast(request.getParameterNames());
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            if (nameSet != null
                    && (onlyIncludeOrSkipPrim ^ nameSet.contains(name))) {
                continue;
            }

            Object value = null;
            String[] paramArr = request.getParameterValues(name);
            if (paramArr != null) {
                if (paramArr.length > 1) {
                    value = Arrays.asList(paramArr);
                } else {
                    value = paramArr[0];
                    // does the same thing basically, nothing better about it as
                    // far as I can see: value = request.getParameter(name);
                }
            }
            paramMap.put(name, value);
        }

        paramMap.putAll(getPathInfoOnlyParameterMap(request, nameSet,
                onlyIncludeOrSkip));

        if (paramMap.size() == 0) {
            // nothing found in the parameters; maybe we read the stream instead
            Map<String, Object> multiPartMap = GenericsUtil.checkMap(request
                    .getAttribute("multiPartMap"));
            if (ObjectUtil.isNotEmpty(multiPartMap)) {
                paramMap.putAll(multiPartMap);
            }
        }
        return canonicalizeParameterMap(paramMap);
    }

    public static Map<String, Object> getQueryStringOnlyParameterMap(
            HttpServletRequest request) {
        return getQueryStringOnlyParameterMap(request.getQueryString());
    }

    public static Map<String, Object> getQueryStringOnlyParameterMap(
            String queryString) {
        Map<String, Object> paramMap = new ConcurrentHashMap<String,Object>();
        if (StringUtil.isNotEmpty(queryString)) {
            StringTokenizer queryTokens = new StringTokenizer(queryString, "&");
            while (queryTokens.hasMoreTokens()) {
                String token = queryTokens.nextToken();
                if (token.startsWith("amp;")) {
                    // this is most likely a split value that had an &amp; in
                    // it, so don't consider this a name; note that some old
                    // code just stripped the "amp;" and went with it
                    // token = token.substring(4);
                    continue;
                }
                int equalsIndex = token.indexOf("=");
                String name = token;
                if (equalsIndex > 0) {
                    name = token.substring(0, equalsIndex);
                    paramMap.put(name, token.substring(equalsIndex + 1));
                }
            }
        }
        return canonicalizeParameterMap(paramMap);
    }

    public static Map<String, Object> getPathInfoOnlyParameterMap(
            HttpServletRequest request, Set<? extends String> nameSet,
            Boolean onlyIncludeOrSkip) {
        return getPathInfoOnlyParameterMap(request.getPathInfo(), nameSet,
                onlyIncludeOrSkip);
    }

    public static Map<String, Object> getPathInfoOnlyParameterMap(
            String pathInfoStr, Set<? extends String> nameSet,
            Boolean onlyIncludeOrSkip) {
        boolean onlyIncludeOrSkipPrim = onlyIncludeOrSkip == null ? true
                : onlyIncludeOrSkip.booleanValue();
        Map<String, Object> paramMap = new ConcurrentHashMap<String,Object>();

        // now add in all path info parameters /~name1=value1/~name2=value2/
        // note that if a parameter with a given name already exists it will be
        // put into a list with all values

        if (StringUtil.isNotEmpty(pathInfoStr)) {
            // make sure string ends with a trailing '/' so we get all values
            if (!pathInfoStr.endsWith("/"))
                pathInfoStr += "/";

            int current = pathInfoStr.indexOf('/');
            int last = current;
            while ((current = pathInfoStr.indexOf('/', last + 1)) != -1) {
                String element = pathInfoStr.substring(last + 1, current);
                last = current;
                if (element.charAt(0) == '~' && element.indexOf('=') > -1) {
                    String name = element.substring(1, element.indexOf('='));
                    if (nameSet != null
                            && (onlyIncludeOrSkipPrim ^ nameSet.contains(name))) {
                        continue;
                    }

                    String value = element.substring(element.indexOf('=') + 1);
                    Object curValue = paramMap.get(name);
                    if (curValue != null) {
                        List<String> paramList = null;
                        if (curValue instanceof List<?>) {
                            paramList = GenericsUtil.checkList(curValue);
                            paramList.add(value);
                        } else {
                            String paramString = (String) curValue;
                            paramList = Collections.synchronizedList(new ArrayList<String>());
                            paramList.add(paramString);
                            paramList.add(value);
                        }
                        paramMap.put(name, paramList);
                    } else {
                        paramMap.put(name, value);
                    }
                }
            }
        }

        return canonicalizeParameterMap(paramMap);
    }

    public static Map<String, Object> getUrlOnlyParameterMap(
            HttpServletRequest request) {
        return getUrlOnlyParameterMap(request.getQueryString(),
                request.getPathInfo());
    }

    public static Map<String, Object> getUrlOnlyParameterMap(
            String queryString, String pathInfo) {
        // NOTE: these have already been through canonicalizeParameterMap, so
        // not doing it again here
        Map<String, Object> paramMap = getQueryStringOnlyParameterMap(queryString);
        paramMap.putAll(getPathInfoOnlyParameterMap(pathInfo, null, null));
        return paramMap;
    }

    public static Map<String, Object> canonicalizeParameterMap(
            Map<String, Object> paramMap) {
        for (Map.Entry<String, Object> paramEntry : paramMap.entrySet()) {
            if (paramEntry.getValue() instanceof String) {
                paramEntry.setValue(paramEntry.getValue());
            } else if (paramEntry.getValue() instanceof Collection<?>) {
                List<String> newList = Collections.synchronizedList(new ArrayList<String>()); 
                for (String listEntry : GenericsUtil
                        .<String> checkCollection(paramEntry.getValue())) {
                    newList.add(listEntry);
                }
                paramEntry.setValue(newList);
            }
        }
        return paramMap;
    }

    public static Map<String, Object> getJSONAttributeMap(
            HttpServletRequest request) {
        Map<String, Object> returnMap = new ConcurrentHashMap<String,Object>();
        Map<String, Object> attrMap = getAttributeMap(request);
        for (Map.Entry<String, Object> entry : attrMap.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();
            if (val instanceof java.sql.Timestamp) {
                val = val.toString();
            }
            if (val instanceof String || val instanceof Number
                    || val instanceof Map<?, ?> || val instanceof List<?>
                    || val instanceof Boolean) {
                returnMap.put(key, val);
            }
        }

        return returnMap;
    }

    public static Map<String, Object> getAttributeMap(HttpServletRequest request) {
        return getAttributeMap(request, null);
    }

    public static Map<String, Object> getAttributeMap(
            HttpServletRequest request, Set<? extends String> namesToSkip) {
        Map<String, Object> attributeMap = new ConcurrentHashMap<String,Object>();

        // look at all request attributes
        Enumeration<String> requestAttrNames = GenericsUtil.cast(request
                .getAttributeNames());
        while (requestAttrNames.hasMoreElements()) {
            String attrName = requestAttrNames.nextElement();
            if (namesToSkip != null && namesToSkip.contains(attrName))
                continue;

            Object attrValue = request.getAttribute(attrName);
            attributeMap.put(attrName, attrValue);
        }
        return attributeMap;
    }

    public static Map<String, Object> getSessionMap(HttpServletRequest request) {
        return getSessionMap(request, null);
    }

    public static Map<String, Object> getSessionMap(HttpServletRequest request,
            Set<? extends String> namesToSkip) {
        Map<String, Object> sessionMap = new ConcurrentHashMap<String,Object>();
        HttpSession session = request.getSession();

        // look at all the session attributes
        Enumeration<String> sessionAttrNames = GenericsUtil.cast(session
                .getAttributeNames());
        while (sessionAttrNames.hasMoreElements()) {
            String attrName = sessionAttrNames.nextElement();
            if (namesToSkip != null && namesToSkip.contains(attrName))
                continue;

            Object attrValue = session.getAttribute(attrName);
            sessionMap.put(attrName, attrValue);
        }
        return sessionMap;
    }

    public static Map<String, Object> getServletContextMap(
            HttpServletRequest request) {
        return getServletContextMap(request, null);
    }

    public static Map<String, Object> getServletContextMap(
            HttpServletRequest request, Set<? extends String> namesToSkip) {
        Map<String, Object> servletCtxMap = new ConcurrentHashMap<String,Object>();

        // look at all servlet context attributes
        ServletContext servletContext = (ServletContext) request
                .getAttribute("servletContext");
        Enumeration<String> applicationAttrNames = GenericsUtil
                .cast(servletContext.getAttributeNames());
        while (applicationAttrNames.hasMoreElements()) {
            String attrName = applicationAttrNames.nextElement();
            if (namesToSkip != null && namesToSkip.contains(attrName))
                continue;

            Object attrValue = servletContext.getAttribute(attrName);
            servletCtxMap.put(attrName, attrValue);
        }
        return servletCtxMap;
    }

    public static Map<String, Object> makeParamMapWithPrefix(
            HttpServletRequest request, String prefix, String suffix) {
        return makeParamMapWithPrefix(request, null, prefix, suffix);
    }

    public static Map<String, Object> makeParamMapWithPrefix(
            HttpServletRequest request,
            Map<String, ? extends Object> additionalFields, String prefix,
            String suffix) {
        return makeParamMapWithPrefix(getParameterMap(request),
                additionalFields, prefix, suffix);
    }

    public static Map<String, Object> makeParamMapWithPrefix(
            Map<String, ? extends Object> context,
            Map<String, ? extends Object> additionalFields, String prefix,
            String suffix) {
        Map<String, Object> paramMap = new HashMap<String, Object>();
        for (Map.Entry<String, ? extends Object> entry : context.entrySet()) {
            String parameterName = entry.getKey();
            if (parameterName.startsWith(prefix)) {
                if (StringUtil.isNotEmpty(suffix)) {
                    if (parameterName.endsWith(suffix)) {
                        String key = parameterName.substring(prefix.length(),
                                parameterName.length() - (suffix.length()));
                        if (entry.getValue() instanceof ByteBuffer) {
                            ByteBuffer value = (ByteBuffer) entry.getValue();
                            paramMap.put(key, value);
                        } else {
                            String value = (String) entry.getValue();
                            paramMap.put(key, value);
                        }
                    }
                } else {
                    String key = parameterName.substring(prefix.length());
                    if (context.get(parameterName) instanceof ByteBuffer) {
                        ByteBuffer value = (ByteBuffer) entry.getValue();
                        paramMap.put(key, value);
                    } else {
                        String value = (String) entry.getValue();
                        paramMap.put(key, value);
                    }
                }
            }
        }
        if (additionalFields != null) {
            for (Map.Entry<String, ? extends Object> entry : additionalFields
                    .entrySet()) {
                String fieldName = entry.getKey();
                if (fieldName.startsWith(prefix)) {
                    if (StringUtil.isNotEmpty(suffix)) {
                        if (fieldName.endsWith(suffix)) {
                            String key = fieldName.substring(prefix.length(),
                                    fieldName.length() - (suffix.length() - 1));
                            Object value = entry.getValue();
                            paramMap.put(key, value);

                            // check for image upload data
                            if (!(value instanceof String)) {
                                String nameKey = "_" + key + "_fileName";
                                Object nameVal = additionalFields.get("_"
                                        + fieldName + "_fileName");
                                if (nameVal != null) {
                                    paramMap.put(nameKey, nameVal);
                                }

                                String typeKey = "_" + key + "_contentType";
                                Object typeVal = additionalFields.get("_"
                                        + fieldName + "_contentType");
                                if (typeVal != null) {
                                    paramMap.put(typeKey, typeVal);
                                }

                                String sizeKey = "_" + key + "_size";
                                Object sizeVal = additionalFields.get("_"
                                        + fieldName + "_size");
                                if (sizeVal != null) {
                                    paramMap.put(sizeKey, sizeVal);
                                }
                            }
                        }
                    } else {
                        String key = fieldName.substring(prefix.length());
                        Object value = entry.getValue();
                        paramMap.put(key, value);

                        // check for image upload data
                        if (!(value instanceof String)) {
                            String nameKey = "_" + key + "_fileName";
                            Object nameVal = additionalFields.get("_"
                                    + fieldName + "_fileName");
                            if (nameVal != null) {
                                paramMap.put(nameKey, nameVal);
                            }

                            String typeKey = "_" + key + "_contentType";
                            Object typeVal = additionalFields.get("_"
                                    + fieldName + "_contentType");
                            if (typeVal != null) {
                                paramMap.put(typeKey, typeVal);
                            }

                            String sizeKey = "_" + key + "_size";
                            Object sizeVal = additionalFields.get("_"
                                    + fieldName + "_size");
                            if (sizeVal != null) {
                                paramMap.put(sizeKey, sizeVal);
                            }
                        }
                    }
                }
            }
        }
        return paramMap;
    }

    public static List<Object> makeParamListWithSuffix(
            HttpServletRequest request, String suffix, String prefix) {
        return makeParamListWithSuffix(request, null, suffix, prefix);
    }

    public static List<Object> makeParamListWithSuffix(
            HttpServletRequest request,
            Map<String, ? extends Object> additionalFields, String suffix,
            String prefix) {
        List<Object> paramList = new ArrayList<Object>();
        Enumeration<String> parameterNames = GenericsUtil.cast(request
                .getParameterNames());
        while (parameterNames.hasMoreElements()) {
            String parameterName = parameterNames.nextElement();
            if (parameterName.endsWith(suffix)) {
                if (StringUtil.isNotEmpty(prefix)) {
                    if (parameterName.startsWith(prefix)) {
                        String value = request.getParameter(parameterName);
                        paramList.add(value);
                    }
                } else {
                    String value = request.getParameter(parameterName);
                    paramList.add(value);
                }
            }
        }
        if (additionalFields != null) {
            for (Map.Entry<String, ? extends Object> entry : additionalFields
                    .entrySet()) {
                String fieldName = entry.getKey();
                if (fieldName.endsWith(suffix)) {
                    if (StringUtil.isNotEmpty(prefix)) {
                        if (fieldName.startsWith(prefix)) {
                            paramList.add(entry.getValue());
                        }
                    } else {
                        paramList.add(entry.getValue());
                    }
                }
            }
        }
        return paramList;
    }

    public static String getApplicationName(HttpServletRequest request) {
        String appName = "root";
        if (request.getContextPath().length() > 1) {
            appName = request.getContextPath().substring(1);
        }
        return appName;
    }

    public static void setInitialRequestInfo(HttpServletRequest request) {
        HttpSession session = request.getSession();
        if (ObjectUtil.isNotEmpty(session.getAttribute("_WEBAPP_NAME_"))) {
            // oops, info already in place...
            return;
        }

        StringBuffer fullRequestUrl = HttpUtil.getFullRequestUrl(request);

        session.setAttribute("_WEBAPP_NAME_",
                HttpUtil.getApplicationName(request));
        session.setAttribute("_CLIENT_LOCALE_", request.getLocale());
        session.setAttribute("_CLIENT_REQUEST_", fullRequestUrl.toString());
        session.setAttribute(
                "_CLIENT_USER_AGENT_",
                request.getHeader("User-Agent") != null ? request
                        .getHeader("User-Agent") : "");
        session.setAttribute(
                "_CLIENT_REFERER_",
                request.getHeader("Referer") != null ? request
                        .getHeader("Referer") : "");

        session.setAttribute("_CLIENT_FORWARDED_FOR_",
                request.getHeader("X-Forwarded-For"));
        session.setAttribute("_CLIENT_REMOTE_ADDR_", request.getRemoteAddr());
        session.setAttribute("_CLIENT_REMOTE_HOST_", request.getRemoteHost());
        session.setAttribute("_CLIENT_REMOTE_USER_", request.getRemoteUser());
    }

    public static void parametersToAttributes(HttpServletRequest request) {
        java.util.Enumeration<String> e = GenericsUtil.cast(request
                .getParameterNames());
        while (e.hasMoreElements()) {
            String name = e.nextElement();
            request.setAttribute(name, request.getParameter(name));
        }
    }

    public static StringBuffer getServerRootUrl(HttpServletRequest request) {
        StringBuffer requestUrl = new StringBuffer();
        requestUrl.append(request.getScheme());
        requestUrl.append("://" + request.getServerName());
        if (request.getServerPort() != 80 && request.getServerPort() != 443)
            requestUrl.append(":" + request.getServerPort());
        return requestUrl;
    }

    public static StringBuffer getFullRequestUrl(HttpServletRequest request) {
        StringBuffer requestUrl = HttpUtil.getServerRootUrl(request);
        requestUrl.append(request.getRequestURI());
        if (request.getQueryString() != null) {
            requestUrl.append("?" + request.getQueryString());
        }
        return requestUrl;
    }

    public static String urlEncodeArgs(Map<String, ? extends Object> args,
            String charset) {
        return urlEncodeArgs(args, charset, true);
    }

    public static String urlEncodeArgs(Map<String, ? extends Object> args,
            String charset, boolean useExpandedEntites) {
        StringBuilder buf = new StringBuilder();
        if (args != null) {
            for (Map.Entry<String, ? extends Object> entry : args.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                String valueStr = null;
                if (name == null || value == null) {
                    continue;
                }

                Collection<?> col;
                if (value instanceof String) {
                    col = Arrays.asList(value);
                } else if (value instanceof Collection) {
                    col = GenericsUtil.cast(value);
                } else if (value.getClass().isArray()) {
                    col = Arrays.asList((Object[]) value);
                } else {
                    col = Arrays.asList(value);
                }
                for (Object colValue : col) {
                    if (colValue instanceof String) {
                        valueStr = (String) colValue;
                    } else if (colValue == null) {
                        continue;
                    } else {
                        valueStr = colValue.toString();
                    }

                    if (StringUtil.isNotEmpty(valueStr)) {
                        if (buf.length() > 0) {
                            if (useExpandedEntites) {
                                buf.append("&amp;");
                            } else {
                                buf.append("&");
                            }
                        }
                        try {
                            buf.append(URLEncoder.encode(name,charset));
                        } catch (UnsupportedEncodingException e) {
                            LoggerUtil.error(logger, e, e.getMessage());
                            throw new HttpUtilException(HttpUtil.class,e);
                        }
                        buf.append('=');
                        try {
                            buf.append(URLEncoder.encode(valueStr,charset));
                        } catch (UnsupportedEncodingException e) {
                            LoggerUtil.error(logger, e, e.getMessage());
                            throw new HttpUtilException(HttpUtil.class,e);
                        }
                    }
                }
            }
        }
        return buf.toString();
    }

    public static String getRequestUriFromTarget(String target) {
        if (StringUtil.isEmpty(target))
            return null;
        int endOfRequestUri = target.length();
        if (target.indexOf('?') > 0) {
            endOfRequestUri = target.indexOf('?');
        }
        int slashBeforeRequestUri = target.lastIndexOf('/', endOfRequestUri);
        String requestUri = null;
        if (slashBeforeRequestUri < 0) {
            requestUri = target.substring(0, endOfRequestUri);
        } else {
            requestUri = target.substring(slashBeforeRequestUri,
                    endOfRequestUri);
        }
        return requestUri;
    }

    public static String getQueryStringFromTarget(String target) {
        if (StringUtil.isEmpty(target))
            return "";
        int queryStart = target.indexOf('?');
        if (queryStart != -1) {
            return target.substring(queryStart);
        }
        return "";
    }

    public static String removeQueryStringFromTarget(String target) {
        if (StringUtil.isEmpty(target))
            return null;
        int queryStart = target.indexOf('?');
        if (queryStart < 0) {
            return target;
        }
        return target.substring(0, queryStart);
    }

    public static String getWebappMountPointFromTarget(String target) {
        int firstChar = 0;
        if (StringUtil.isEmpty(target))
            return null;
        if (target.charAt(0) == '/')
            firstChar = 1;
        int pathSep = target.indexOf('/', 1);
        String webappMountPoint = null;
        if (pathSep > 0) {
            // if not then no good, supposed to be a inter-app, but there is no
            // path sep! will do general search with null and treat like an
            // intra-app
            webappMountPoint = target.substring(firstChar, pathSep);
        }
        return webappMountPoint;
    }

    public static String encodeAmpersands(String htmlString) {
        StringBuilder htmlBuffer = new StringBuilder(htmlString);
        int ampLoc = -1;
        while ((ampLoc = htmlBuffer.indexOf("&", ampLoc + 1)) != -1) {
            // NOTE: this should work fine, but if it doesn't could try making
            // sure all characters between & and ; are letters, that would
            // qualify as an entity

            // found ampersand, is it already and entity? if not change it to
            // &amp;
            int semiLoc = htmlBuffer.indexOf(";", ampLoc);
            if (semiLoc != -1) {
                // found a semi colon, if it has another & or an = before it,
                // don't count it as an entity, otherwise it may be an entity,
                // so skip it
                int eqLoc = htmlBuffer.indexOf("=", ampLoc);
                int amp2Loc = htmlBuffer.indexOf("&", ampLoc + 1);
                if ((eqLoc == -1 || eqLoc > semiLoc)
                        && (amp2Loc == -1 || amp2Loc > semiLoc)) {
                    continue;
                }
            }

            // at this point not an entity, no substitute with a &amp;
            htmlBuffer.insert(ampLoc + 1, "amp;");
        }
        return htmlBuffer.toString();
    }

    public static String encodeBlanks(String htmlString) {
        return htmlString.replaceAll(" ", "%20");
    }

    public static String setResponseBrowserProxyNoCache(
            HttpServletRequest request, HttpServletResponse response) {
        setResponseBrowserProxyNoCache(response);
        return "success";
    }

    public static void setResponseBrowserProxyNoCache(
            HttpServletResponse response) {
        long nowMillis = System.currentTimeMillis();
        response.setDateHeader("Expires", nowMillis);
        response.setDateHeader("Last-Modified", nowMillis); // always modified
        response.setHeader("Cache-Control",
                "no-store, no-cache, must-revalidate"); // HTTP/1.1
        response.addHeader("Cache-Control", "post-check=0, pre-check=0, false");
        response.setHeader("Pragma", "no-cache"); // HTTP/1.0
    }

    public static String getContentTypeByFileName(String fileName) {
        FileNameMap mime = URLConnection.getFileNameMap();
        return mime.getContentTypeFor(fileName);
    }

    public static void streamContentToBrowser(HttpServletResponse response,
            byte[] bytes, String contentType, String fileName)
            throws IOException {
        // tell the browser not the cache
        setResponseBrowserProxyNoCache(response);

        // set the response info
        response.setContentLength(bytes.length);
        if (contentType != null) {
            response.setContentType(contentType);
        }
        if (fileName != null) {
            response.setHeader("Content-Disposition", "attachment;filename="
                    + fileName);
        }

        // create the streams
        OutputStream out = response.getOutputStream();
        InputStream in = new ByteArrayInputStream(bytes);

        // stream the content
        try {
            streamContent(out, in, bytes.length);
        } catch (IOException e) {
            in.close();
            out.close(); // should we close the ServletOutputStream on error??
            throw e;
        }

        // close the input stream
        in.close();

        // close the servlet output stream
        out.flush();
        out.close();
    }

    public static void streamContentToBrowser(HttpServletResponse response,
            byte[] bytes, String contentType) throws IOException {
        streamContentToBrowser(response, bytes, contentType, null);
    }

    public static void streamContentToBrowser(HttpServletResponse response,
            InputStream in, int length, String contentType, String fileName)
            throws IOException {
        // tell the browser not the cache
        setResponseBrowserProxyNoCache(response);

        // set the response info
        response.setContentLength(length);
        if (contentType != null) {
            response.setContentType(contentType);
        }
        if (fileName != null) {
            response.setHeader("Content-Disposition", "attachment;filename="
                    + fileName);
        }

        // stream the content
        OutputStream out = response.getOutputStream();
        try {
            streamContent(out, in, length);
        } catch (IOException e) {
            out.close();
            throw e;
        }

        // close the servlet output stream
        out.flush();
        out.close();
    }

    public static void streamContentToBrowser(HttpServletResponse response,
            InputStream in, int length, String contentType) throws IOException {
        streamContentToBrowser(response, in, length, contentType, null);
    }

    public static void streamContent(OutputStream out, InputStream in,
            int length) throws IOException {
        int bufferSize = 512; // same as the default buffer size; change as
                              // needed

        // make sure we have something to write to
        if (out == null) {
            throw new IOException("Attempt to write to null output stream");
        }

        // make sure we have something to read from
        if (in == null) {
            throw new IOException("Attempt to read from null input stream");
        }

        // make sure we have some content
        if (length == 0) {
            throw new IOException(
                    "Attempt to write 0 bytes of content to output stream");
        }

        // initialize the buffered streams
        BufferedOutputStream bos = new BufferedOutputStream(out, bufferSize);
        BufferedInputStream bis = new BufferedInputStream(in, bufferSize);

        byte[] buffer = new byte[length];
        int read = 0;
        try {
            while ((read = bis.read(buffer, 0, buffer.length)) != -1) {
                bos.write(buffer, 0, read);
            }
        } catch (IOException e) {
            bis.close();
            bos.close();
            throw e;
        } finally {
            if (bis != null) {
                bis.close();
            }
            if (bos != null) {
                bos.flush();
                bos.close();
            }
        }
    }

    public static String stripViewParamsFromQueryString(String queryString) {
        return stripViewParamsFromQueryString(queryString, null);
    }

    public static String stripViewParamsFromQueryString(String queryString,
            String paginatorNumber) {
        Set<String> paramNames = new HashSet<String>();
        if (StringUtil.isNotEmpty(paginatorNumber)) {
            paginatorNumber = "_" + paginatorNumber;
        }
        paramNames.add("VIEW_INDEX" + paginatorNumber);
        paramNames.add("VIEW_SIZE" + paginatorNumber);
        paramNames.add("viewIndex" + paginatorNumber);
        paramNames.add("viewSize" + paginatorNumber);
        return stripNamedParamsFromQueryString(queryString, paramNames);
    }

    public static String stripNamedParamsFromQueryString(String queryString,
            Collection<String> paramNames) {
        String retStr = null;
        if (StringUtil.isNotEmpty(queryString)) {
            StringTokenizer queryTokens = new StringTokenizer(queryString, "&");
            StringBuilder cleanQuery = new StringBuilder();
            while (queryTokens.hasMoreTokens()) {
                String token = queryTokens.nextToken();
                if (token.startsWith("amp;")) {
                    token = token.substring(4);
                }
                int equalsIndex = token.indexOf("=");
                String name = token;
                if (equalsIndex > 0) {
                    name = token.substring(0, equalsIndex);
                }
                if (!paramNames.contains(name)) {
                    if (cleanQuery.length() > 0) {
                        cleanQuery.append("&");
                    }
                    cleanQuery.append(token);
                }
            }
            retStr = cleanQuery.toString();
        }
        return retStr;
    }

    public static Collection<Map<String, Object>> parseMultiFormData(
            Map<String, Object> parameters) {
        Map<Integer, Map<String, Object>> rows = new ConcurrentHashMap<Integer,Map<String,Object>>();
                                                                            // the
                                                                            // rows
                                                                            // keyed
                                                                            // by
                                                                            // row
                                                                            // number

        // first loop through all the keys and create a hashmap for each
        // ${ROW_SUBMIT_PREFIX}${N} = Y
        for (String key : parameters.keySet()) {
            // skip everything that is not ${ROW_SUBMIT_PREFIX}N
            if (key == null || key.length() <= ROW_SUBMIT_PREFIX_LENGTH)
                continue;
            if (key.indexOf(MULTI_ROW_DELIMITER) <= 0)
                continue;
            if (!key.substring(0, ROW_SUBMIT_PREFIX_LENGTH).equals(
                    ROW_SUBMIT_PREFIX))
                continue;
            if (!parameters.get(key).equals("Y"))
                continue;

            // decode the value of N and create a new map for it
            Integer n = Integer.decode(key.substring(ROW_SUBMIT_PREFIX_LENGTH,
                    key.length()));
            Map<String, Object> m = new ConcurrentHashMap<String,Object>();
            m.put("row", n); // special "row" = N tuple
            rows.put(n, m); // key it to N
        }

        // next put all parameters with matching N in the right map
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            // skip keys without DELIMITER and skip ROW_SUBMIT_PREFIX
            if (key == null)
                continue;
            int index = key.indexOf(MULTI_ROW_DELIMITER);
            if (index <= 0)
                continue;
            if (key.length() > ROW_SUBMIT_PREFIX_LENGTH
                    && key.substring(0, ROW_SUBMIT_PREFIX_LENGTH).equals(
                            ROW_SUBMIT_PREFIX))
                continue;

            // get the map with index N
            Integer n = Integer.decode(key.substring(index
                    + MULTI_ROW_DELIMITER_LENGTH, key.length())); // N from
                                                                  // ${param}${DELIMITER}${N}
            Map<String, Object> map = rows.get(n);
            if (map == null)
                continue;

            // get the key without the <DELIMITER>N suffix and store it and its
            // value
            String newKey = key.substring(0, index);
            map.put(newKey, entry.getValue());
        }
        // return only the values, which is the list of maps
        return rows.values();
    }

    public static <V> Map<String, V> removeMultiFormParameters(
            Map<String, V> parameters) {
        Map<String, V> filteredParameters = new ConcurrentHashMap<String,V>();
        for (Map.Entry<String, V> entry : parameters.entrySet()) {
            String key = entry.getKey();
            if (key != null
                    && (key.indexOf(MULTI_ROW_DELIMITER) != -1
                            || key.indexOf("_useRowSubmit") != -1 || key
                            .indexOf("_rowCount") != -1)) {
                continue;
            }

            filteredParameters.put(key, entry.getValue());
        }
        return filteredParameters;
    }

    public static String makeCompositeParam(String prefix, String suffix) {
        return prefix + COMPOSITE_DELIMITER + suffix;
    }

    public static Object makeParamValueFromComposite(
            HttpServletRequest request, String prefix, Locale locale) {
        String compositeType = request.getParameter(makeCompositeParam(prefix,
                "compositeType"));
        if (StringUtil.isEmpty(compositeType))
            return null;

        // collect the composite fields into a map
        Map<String, String> data = new ConcurrentHashMap<String,String>();
        for (Enumeration<String> names = GenericsUtil.cast(request
                .getParameterNames()); names.hasMoreElements();) {
            String name = names.nextElement();
            if (!name.startsWith(prefix + COMPOSITE_DELIMITER))
                continue;

            // extract the suffix of the composite name
            String suffix = name.substring(name.indexOf(COMPOSITE_DELIMITER)
                    + COMPOSITE_DELIMITER_LENGTH);

            // and the value of this parameter
            String value = request.getParameter(name);

            // key = suffix, value = parameter data
            data.put(suffix, value);
        }

        // handle recomposition of data into the compositeType
        if ("Timestamp".equals(compositeType)) {
            String date = data.get("date");
            String hour = data.get("hour");
            String minutes = data.get("minutes");
            String ampm = data.get("ampm");
            if (date == null || date.length() < 10)
                return null;
            if (StringUtil.isEmpty(hour))
                return null;
            if (StringUtil.isEmpty(minutes))
                return null;
            boolean isTwelveHour = StringUtil.isNotEmpty(ampm);

            // create the timestamp from the data
            try {
                int h = Integer.parseInt(hour);
                Timestamp timestamp = Timestamp.valueOf(date.substring(0, 10)
                        + " 00:00:00.000");
                Calendar cal = Calendar.getInstance(locale);
                cal.setTime(timestamp);
                if (isTwelveHour) {
                    boolean isAM = ("AM".equals(ampm) ? true : false);
                    if (isAM && h == 12)
                        h = 0;
                    if (!isAM && h < 12)
                        h += 12;
                }
                cal.set(Calendar.HOUR_OF_DAY, h);
                cal.set(Calendar.MINUTE, Integer.parseInt(minutes));
                return new Timestamp(cal.getTimeInMillis());
            } catch (IllegalArgumentException e) {
                LoggerUtil.error(logger, e, e.getMessage());
                return null;
            }
        }

        // we don't support any other compositeTypes (yet)
        return null;
    }

    public static String getSessionId(HttpServletRequest request) {
        HttpSession session = request.getSession();
        return (session == null ? "unknown" : session.getId());
    }

    public static boolean isJavaScriptEnabled(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Boolean javaScriptEnabled = (Boolean) session
                .getAttribute("javaScriptEnabled");
        if (javaScriptEnabled != null) {
            return javaScriptEnabled.booleanValue();
        }
        return false;
    }

    public static int getMultiFormRowCount(HttpServletRequest request) {
        return HttpUtil.getMultiFormRowCount(HttpUtil.getParameterMap(request));
    }

    public static int getMultiFormRowCount(Map<String, ?> requestMap) {
        // The number of multi form rows is computed selecting the maximum index
        int rowCount = 0;
        String maxRowIndex = "";
        int rowDelimiterLength = HttpUtil.MULTI_ROW_DELIMITER.length();
        for (String parameterName : requestMap.keySet()) {
            int rowDelimiterIndex = (parameterName != null ? parameterName
                    .indexOf(HttpUtil.MULTI_ROW_DELIMITER) : -1);
            if (rowDelimiterIndex > 0) {
                String thisRowIndex = parameterName.substring(rowDelimiterIndex
                        + rowDelimiterLength);
                if (thisRowIndex.indexOf("_") > -1) {
                    thisRowIndex = thisRowIndex.substring(0,
                            thisRowIndex.indexOf("_"));
                }
                if (maxRowIndex.length() < thisRowIndex.length()) {
                    maxRowIndex = thisRowIndex;
                } else if (maxRowIndex.length() == thisRowIndex.length()
                        && maxRowIndex.compareTo(thisRowIndex) < 0) {
                    maxRowIndex = thisRowIndex;
                }
            }
        }
        if (StringUtil.isNotEmpty(maxRowIndex)) {
            try {
                rowCount = Integer.parseInt(maxRowIndex);
                rowCount++; // row indexes are zero based
            } catch (NumberFormatException e) {
                LoggerUtil.error(logger, e, e.getMessage());
            }
        }
        return rowCount;
    }

    public static String stashParameterMap(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Map<String, Object>> paramMapStore = GenericsUtil
                .checkMap(session.getAttribute("_PARAM_MAP_STORE_"));
        if (paramMapStore == null) {
            paramMapStore = new ConcurrentHashMap<String,Map<String,Object>>();
            session.setAttribute("_PARAM_MAP_STORE_", paramMapStore);
        }
        Map<String, Object> parameters = HttpUtil.getParameterMap(request);
        String paramMapId = RandomStringUtil.randomAlphanumeric(10);
        paramMapStore.put(paramMapId, parameters);
        return paramMapId;
    }

    public static String getNextUniqueId(HttpServletRequest request) {
        Integer uniqueIdNumber = (Integer) request.getAttribute("UNIQUE_ID");
        if (uniqueIdNumber == null) {
            uniqueIdNumber = Integer.valueOf(1);
        }

        request.setAttribute("UNIQUE_ID",
                Integer.valueOf(uniqueIdNumber.intValue() + 1));
        return "autoId_" + uniqueIdNumber;
    }

}
