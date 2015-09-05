package com.shufudong.demo.HttpClient;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.shufudong.demo.HttpClient.Exception.HttpClientException;

/** 
 * @ClassName:   [中]HttpClientTest 
 * @Description: [中]TODO(这里用一句话描述这个类的作用) 
 * @author       [中]ShuFuDong
 * @date         [中]2015年9月2日 上午6:06:47 
 */
public class HttpClientTest {
    
    private HttpClient httpClient;

    /** 
     * [中]TODO(这里用一句话描述这个方法的作用) 
     * @throws java.lang.Exception           [中]设定文件 
     */
    @Before
    public void setUp() throws Exception {
        httpClient = new HttpClient();
    }

    /** 
     * [中]TODO(这里用一句话描述这个方法的作用) 
     * @throws java.lang.Exception           [中]设定文件 
     */
    @After
    public void tearDown() throws Exception {
        httpClient = null;
    }

    /**
     * Test method for {@link com.shufudong.demo.HttpClient.HttpClient#sendHttpRequest(java.lang.String)}.
     */
    @Test
    public void testSendHttpRequest() {
        httpClient.setDebug(true);
        httpClient.setUrl("http://localhost:8080/ApiDummy/CB/api/PKServiceSearch");
        Map<String,Object> requestMap = new HashMap<String,Object>();
        requestMap.put("nikkei_member_no", "987654");
        httpClient.setParameters(requestMap);
        String respStr = null;
        try {
            respStr = httpClient.sendHttpRequest("post");
        } catch (HttpClientException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Assert.assertNotNull(respStr);
    }

}
