package cn.orangeiot.util;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */

import java.io.Serializable;

/**
 * 接口层统一返回的数据封装接口
 *
 * @author app
 */
public class Result<E> implements Serializable {
    public final static int CODE_SUCCESS = 200;
    public int code = CODE_SUCCESS;
    public String msg;
    //public String sign;//签名,使用于数据安全验证码

    private E data;

    public Result() {
        this.code = CODE_SUCCESS;
        this.msg = "success";
    }

    public Result(E data) {
        this.code = CODE_SUCCESS;
        this.msg = "success";
        this.data = data;
    }

    public Result<E> setData(E data) {
        this.data = data;
        return this;
    }

    public E getData() {
        return this.data;
    }

    public int getCode() {
        return this.code;
    }

    public Result<E> setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMsg() {
        return this.msg;
    }

    public Result<E> setMsg(String msg) {
        this.msg = msg;
        return this;
    }


    public Result<E> setErrorMessage(int code, String errorMsg) {
        this.code = code;
        this.msg = errorMsg;
        return this;
    }



/*	@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    private Page page;
	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}*/
}
