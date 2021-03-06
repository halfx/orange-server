package cn.orangeiot.http.spi;

import cn.orangeiot.common.annotation.KdsHttpMessage;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-08
 */
public interface ApiConf {

    /**
     * api路径
     */
    @KdsHttpMessage(Method = "post")
    String USER_LOGIN_TEL = "/user/login/getuserbytel";//用户手机登录

    @KdsHttpMessage(Method = "post")
    String USER_LOGIN_MAIL = "/user/login/getuserbymail";//用户邮箱登录

    @KdsHttpMessage(Method = "post")
    String USER_REGISTER_TEL = "/user/reg/putuserbytel";//用户手机号注册

    @KdsHttpMessage(Method = "post")
    String USER_REGISTER_MAIL = "/user/reg/putuserbyemail";//用户邮箱注册

    @KdsHttpMessage(Method = "post")
    String USER_LOGOUT = "/user/logout";//登出

    @KdsHttpMessage(Method = "post")
    String SEND_SMS_CODE = "/sms/sendSmsTokenByTX";//发送手机验证码

    @KdsHttpMessage(Method = "post")
    String SEND_EMAIL_CODE = "/mail/sendemailtoken";//发送邮箱验证码

    @KdsHttpMessage(Method = "get")
    String GET_FILE_HEADER_IMG = "/user/edit/showfileonline/:uid";//获取头像

    @KdsHttpMessage(Method = "post")
    String UPLOAD_HEADER_IMG = "/user/edit/uploaduserhead";//上传头像

    @KdsHttpMessage(Method = "post")
    String USER_NICKNAME = "/user/edit/getUsernickname";//获取用户昵称

    @KdsHttpMessage(Method = "post")
    String UPDATE_NICKNAME = "/user/edit/postUsernickname";//修改用户昵称

    @KdsHttpMessage(Method = "post")
    String UPDATE_PASSWORD = "/user/edit/postUserPwd";//修改用户密码

    @KdsHttpMessage(Method = "post")
    String FORGET_PASSWORD = "/user/edit/forgetPwd";//忘记密码

    @KdsHttpMessage(Method = "post")
    String SUGGEST_MSG = "/suggest/putmsg";//用户留言

    @KdsHttpMessage(Method = "post")
    String CREATE_ADMIN_DEV = "/adminlock/reg/createadmindev";//添加设备

    @KdsHttpMessage(Method = "post")
    String DELETE_EVEND_DEV = "/adminlock/reg/deletevendordev";//第三方重置设备

    @KdsHttpMessage(Method = "post")
    String DELETE_ADMIN_DEV = "/adminlock/reg/deleteadmindev";//用户主动删除设备

    @KdsHttpMessage(Method = "post")
    String DELETE_NORMAL_DEV = "/normallock/reg/deletenormaldev";//管理员删除用户

    @KdsHttpMessage(Method = "post")
    String CREATE_NORMAL_DEV = "/normallock/reg/createNormalDev";//管理员为设备添加普通用户

    @KdsHttpMessage(Method = "post")
    String GET_OPEN_LOCK_RECORD = "/openlock/downloadopenlocklist";//获取开锁记录

    @KdsHttpMessage(Method = "post")
    String UPDATE_USER_PREMISSON = "/normallock/ctl/updateNormalDevlock";//管理员修改普通用户权限

    @KdsHttpMessage(Method = "post")
    String REQUEST_USER_OPEN_LOCK = "/adminlock/open/adminOpenLock";//App户开锁成功上报

    @KdsHttpMessage(Method = "post")
    String REQUEST_USER_AUTH = "/adminlock/open/openLockAuth";//鎖鑑權權

    @KdsHttpMessage(Method = "post")
    String GET_DEV_LIST = "/adminlock/edit/getAdminDevlist";//获取设备列表

    @KdsHttpMessage(Method = "post")
    String GET_DEV_USER_LIST = "/normallock/ctl/getNormalDevlist";//设备下的普通用户列表

    @KdsHttpMessage(Method = "post")
    String EDIT_ADMIN_DEV = "/adminlock/edit/editadmindev";//管理员修改锁的位置信息

    @KdsHttpMessage(Method = "post")
    String GET_DEV_LONGTITUDE = "/adminlock/edit/getAdminDevlocklongtitude";//获取设备经纬度等信息

    @KdsHttpMessage(Method = "post")
    String UPDATE_ADMIN_DEV_AUTO_LOCK = "/adminlock/edit/updateAdminDevAutolock";//修改设备是否开启自动解锁功能

    @KdsHttpMessage(Method = "post")
    String UPDATE_DEV_NICKNAME = "/adminlock/edit/updateAdminlockNickName";//修改设备昵称

    @KdsHttpMessage(Method = "post")
    String CHECK_DEV = "/adminlock/edit/checkadmindev";//检测是否被绑定

    @KdsHttpMessage(Method = "post")
    String UPLOAD_OPEN_LOCK_RECORD = "/openlock/uploadopenlocklist";//上传开门记录

    @KdsHttpMessage(Method = "post")
    String MODEL_PWD_BY_MAC = "/model/getpwdBySN";//根据sn获取password1

    @KdsHttpMessage(Method = "post")
    String UPLOAD_PUSHID = "/user/upload/pushId";//用户上传pushId

    @KdsHttpMessage(Method = "post")
    String SEND_PUSH_APPLICATION = "/notify/app";//測試推送

    @KdsHttpMessage(Method = "post")
    @Deprecated
    String UPDATE_LOCK_INFO = "/normallock/info/update";//修改锁的信息

    @KdsHttpMessage(Method = "post")
    String OPEN_LOCK_NO_AUTH_SUCCESS = "/adminlock/noAuth/upload";//上傳無服務器鉴权開門記錄

    @KdsHttpMessage(Method = "post")
    String SELECT_OPENLOCK_RECORD = "/openlock/record/select";//查询开门记录

    @KdsHttpMessage(Method = "post")
    String UPDATE_BULK_LOCK_NUMBER = "/adminlock/info/number/bulkupdate";//批量修改锁编号信息

    @KdsHttpMessage(Method = "post")
    String UPDATE_LOCK_NUMBER = "/adminlock/info/number/update";//修改锁编号信息

    @KdsHttpMessage(Method = "post")
    String GET_LOCK_NUMBER = "/adminlock/info/number/get";//獲取锁编号信息
}
