package com.shufudong.demo.HttpClient.Exception;

import com.shufudong.lang.exception.BaseRuntimeException;

/** 
 * @ClassName:   [中]HttpUtilException 
 * @Description: [中]TODO(这里用一句话描述这个类的作用) 
 * @author       [中]ShuFuDong
 * @date         [中]2015年9月4日 下午11:37:52 
 */
public class HttpUtilException extends BaseRuntimeException {
    
    private static final long serialVersionUID = -1162839714262626980L;

    /**
     * [中]构造一个异常, 指明异常的详细信息.
     * 
     * @param message
     *            [中]详细信息
     */
    public HttpUtilException(String message) {
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
    public HttpUtilException(String message, String code,
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
    public HttpUtilException(Class<?> rise, String code, String name,
            Object[] arguments) {
        super(rise, code, name, arguments);
    }
    
    /** 
    * [中]构造一个异常
    * @param rise           [中]对象类
    * @param code           [中]错误编码
    * @param arguments      [中]错误信息参数数组
    */
    public HttpUtilException(Class<?> rise,String code,Object[] arguments){
        super(rise,code,null,arguments);
    }

    /** 
    * [中]构造一个异常
    * @param rise       [中]对象类
    * @param e          [中]异常起因
    */
    public HttpUtilException(Class<?> rise,Throwable e){
        super(rise,e);
    }
}
