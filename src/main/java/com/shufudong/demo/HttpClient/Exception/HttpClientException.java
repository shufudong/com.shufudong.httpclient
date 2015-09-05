package com.shufudong.demo.HttpClient.Exception;

import com.shufudong.lang.exception.BaseException;

/** 
* @ClassName:   [中]HttpClientException 
* @Description: [中]HttpClient工具类异常
* @author       [中]ShuFuDong
* @date         [中]2015年9月4日 下午9:06:13 
*/ 
public class HttpClientException extends BaseException {
    
    private static final long serialVersionUID = 1L;

    /**
     * [中]构造一个异常, 指明异常的详细信息.
     * 
     * @param message
     *            [中]详细信息
     */
    public HttpClientException(String message) {
        super(message);
    }

    /**
     * [中]构造一个异常, 指明引起这个异常的起因.
     * 
     * @param message
     *            [中]详细信息
     * @param cause
     *            [中]异常的起因
     */
    public HttpClientException(String message, String code,
            Throwable cause) {
        super(message, code, cause);
    }

    /** 
    * [中]构造一个异常
    * @param rise           [中]对象类
    * @param code           [中]错误编码
    * @param name           [中]错误名
    * @param arguments      [中]错误信息参数数组
    */
    public HttpClientException(Class<?> rise, String code, String name,
            Object[] arguments) {
        super(rise, code, name, arguments);
    }
    
    /** 
    * [中]构造一个异常
    * @param rise           [中]对象类
    * @param code           [中]错误编码
    * @param arguments      [中]错误信息参数数组
    */
    public HttpClientException(Class<?> rise,String code,Object[] arguments){
        super(rise,code,null,arguments);
    }

    /** 
    * [中]构造一个异常
    * @param rise       [中]对象类
    * @param e          [中]异常起因
    */
    public HttpClientException(Class<?> rise,Throwable e){
        super(rise,e);
    }
}

