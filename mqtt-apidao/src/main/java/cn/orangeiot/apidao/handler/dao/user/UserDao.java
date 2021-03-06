package cn.orangeiot.apidao.handler.dao.user;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.conf.RedisKeyConf;
import cn.orangeiot.common.constant.mongodb.KdsGatewayDeviceList;
import cn.orangeiot.common.constant.mongodb.KdsSuggest;
import cn.orangeiot.common.constant.mongodb.KdsUser;
import cn.orangeiot.common.constant.mongodb.KdsUserLog;
import cn.orangeiot.common.genera.ErrorType;
import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.common.utils.KdsCreateMD5;
import cn.orangeiot.common.utils.SHA1;
import cn.orangeiot.common.utils.UUIDUtils;
import cn.orangeiot.reg.memenet.MemenetAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTOptions;
import io.vertx.ext.mongo.BulkOperation;
import io.vertx.ext.mongo.FindOptions;
import io.vertx.ext.mongo.UpdateOptions;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-07
 */
public class UserDao extends SynchUserDao implements MemenetAddr {

    private static Logger logger = LogManager.getLogger(UserDao.class);

    private JWTAuth jwtAuth;

    private Vertx vertx;

    private JsonObject config;

    private final String clientHeader = "app";

    public UserDao(JWTAuth jwtAuth, Vertx vertx, JsonObject config) {
        this.jwtAuth = jwtAuth;
        this.vertx = vertx;
        this.config = config;
    }

    /**
     * @Description 获取用户
     * @author zhang bo
     * @date 17-12-7
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getUser(Message<JsonObject> message) {
        String[] ars = message.body().getString("clientId").split(":");
        if (ars[0].equals(clientHeader)) {//
            authConn(message);
            return;
        }
        //查找缓存
        RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT + message.body().getString("username"), RedisKeyConf.USER_VAL_TOKEN, rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs.cause());
                message.reply(false);
            } else {
                int flag = Objects.nonNull(message.body().getString("clientId")) ? message.body().getString("clientId").indexOf(":") : 0;//验证标识
                String pwd = message.body().getString("password");

                if (Objects.nonNull(rs.result()) && pwd.equals(rs.result())) {
                    message.reply(true);
                } else {
                    if (flag == 3) {//app验证
                        message.reply(false);
                    } else {//网关
                        //查找DB
                        MongoClient.client.findOne(KdsUser.COLLECT_NAME, new JsonObject().put(KdsUser.USER_GW_ACCOUNT, message.body().getString("username")), new JsonObject()
                                .put(KdsUser.USER_PWD, 1), res -> {
                            if (res.failed()) {
                                logger.error(res.cause().getMessage(), res.cause());
                                message.reply(false);
                            } else {
                                if (Objects.nonNull(res.result()) && Objects.nonNull(res.result().getValue("userPwd"))
                                        && pwd.equals(res.result().getString("userPwd"))) {
                                    message.reply(true);
                                    onGatewayInfo(res.result().put("username", message.body().getString("username")));
                                } else {
                                    message.reply(false);
                                }
                            }
                        });
                    }
                }
            }
        });

    }

    /**
     * @Description 验证连接
     * @author zhang bo
     * @date 18-9-7
     * @version 1.0
     */
    public void authConn(Message<JsonObject> message) {
        RedisClient.client.hmget(RedisKeyConf.USER_ACCOUNT + message.body().getString("username"),
                new ArrayList<String>() {{
                    add(RedisKeyConf.USER_VAL_TOKEN);
                    add(RedisKeyConf.USER_VAL_OLDTOKEN);
                }}, rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs.cause());
                        message.reply(false);
                    } else {
                        if (rs.result().size() > 0) {
                            if (Objects.nonNull(rs.result().getValue(0)) && rs.result().getString(0).equals(message.body().getString("password"))) {//正確
                                message.reply(true);
                            } else if (Objects.nonNull(rs.result().getValue(1)) && rs.result().getString(1).equals(message.body().getString("password"))) {//老值
                                message.reply(false, new DeliveryOptions().addHeader("status", "false"));
                            } else
                                message.reply(false);
                        } else {
                            message.reply(false);
                        }
                    }
                });

    }


    /**
     * @Description 上版本mqtt connect auth
     * @author zhang bo
     * @date 17-12-7
     * @version 1.0
     */
//    @SuppressWarnings("Duplicates")
//    public void getUser(Message<JsonObject> message) {
//        RedisClient.client.hget("userAccount:", ((JsonObject)message.body()).getString("username"), (rs) -> {
//            if (rs.failed()) {
//                rs.cause().printStackTrace();
//            } else if (Objects.nonNull(rs.result()) && GUID.MD5(((JsonObject)message.body()).getString("password") + ((String)rs.result()).split("::")[0]).equals(((String)rs.result()).split("::")[1])) {
//                message.reply(true);
//            } else {
//                MongoClient.client.findOne("sys_user", (new JsonObject()).put("account", ((JsonObject)message.body()).getString("username")), (new JsonObject()).put("type", "").put("status", "").put("salt", "").put("password", ""), (res) -> {
//                    if (res.failed()) {
//                        res.cause().printStackTrace();
//                    } else if (Objects.nonNull(res.result()) && GUID.MD5(((JsonObject)message.body()).getString("password") + ((JsonObject)res.result()).getString("salt").toString()).equals(((JsonObject)res.result()).getString("password").toString())) {
//                        message.reply((new JsonObject()).put("password", ((JsonObject)res.result()).getString("password")).put("salt", ((JsonObject)res.result()).getString("salt")).put("username", ((JsonObject)message.body()).getString("username")).put("code", true));
//                    } else {
//                        message.reply((new JsonObject()).put("code", false));
//                    }
//
//                });
//            }
//        });
//    }Rge


    /**
     * @Description 检验是否登录
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    public void verifyLogin(Message<String> message) {
        try {
            JsonObject jsonObject = new JsonObject(new String(Base64.decodeBase64(message.body())));
            String uid = jsonObject.getString("_id");
            if (null != uid) {
                RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT + uid, RedisKeyConf.USER_VAL_TOKEN, res -> {
                    if (res.failed()) {
                        message.reply(false);
                    } else if (Objects.nonNull(res.result()) && res.result().equals(message.body())) {
                        Future.<String>future(f -> RedisClient.client.get(RedisKeyConf.RATE_LIMIT + uid, f))
                                .setHandler(rs -> {
                                    if (rs.failed()) {
                                        logger.error(rs.cause().getMessage(), rs);
                                        message.reply(false);
                                    } else {
                                        message.reply(true, new DeliveryOptions().addHeader("uid", uid)
                                                .addHeader("times", Objects.nonNull(rs.result()) ? rs.result() : "0"));
                                        RedisClient.client.expire(RedisKeyConf.USER_ACCOUNT + uid
                                                , config.getLong("liveTime"), times -> {
                                                    if (times.failed())
                                                        logger.error(times.cause().getMessage(), times.cause());
                                                });
                                    }
                                });
                    } else {
                        message.reply(false);
                    }
                });
            }
        } catch (Exception e) {
            message.reply(false);
        }
    }


    /**
     * 用户手机登录
     *
     * @param message
     */
    @SuppressWarnings("Duplicates")
    public void telLogin(Message<JsonObject> message) {
        //查找DB
        MongoClient.client.findOne(KdsUser.COLLECT_NAME, new JsonObject().put(KdsUser.USER_TEL, message.body().getString("tel"))
                .put(KdsUser.VERSION_TYPE, message.body().getString("versionType")), new JsonObject()
                .put(KdsUser.USER_PWD, 1).put(KdsUser.PWD_SALT, 1).put(KdsUser._ID, 1).put(KdsUser.NICK_NAME, 1)
                .put(KdsUser.ME_USERNAME, 1).put(KdsUser.ME_PWD, 1).put(KdsUser.USER_ID, 1), res -> {
            if (res.failed()) {
                logger.error(res.cause().getMessage(), res.cause());
            } else {
                if (Objects.nonNull(res.result())) {
                    if (Objects.nonNull(res.result().getValue(KdsUser.PWD_SALT))) {//md5验证
                        encyPwd(res.result().put("username", message.body().getString("tel"))
                                .put("loginIP", message.body().getString("loginIP")), message, KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(res.result().getString("pwdSalt") + message.body().getString("password"))));
                    } else {//sha1验证
                        encyPwd(res.result().put("username", message.body().getString("tel"))
                                .put("loginIP", message.body().getString("loginIP")), message, SHA1.encode(message.body().getString("password")));
                    }
                } else {//登陆失败
                    message.reply(null);
                }

            }
        });
    }


    /**
     * 用户email登录
     *
     * @param message
     */
    @SuppressWarnings("Duplicates")
    public void mailLogin(Message<JsonObject> message) {
        //查找DB
        MongoClient.client.findOne(KdsUser.COLLECT_NAME, new JsonObject().put(KdsUser.USER_MAIL, message.body().getString("mail"))
                .put(KdsUser.VERSION_TYPE, message.body().getString("versionType")), new JsonObject()
                .put(KdsUser.USER_PWD, 1).put(KdsUser.PWD_SALT, 1).put(KdsUser._ID, 1).put(KdsUser.NICK_NAME, 1)
                .put(KdsUser.ME_USERNAME, 1).put(KdsUser.ME_PWD, 1).put(KdsUser.USER_ID, 1), res -> {
            if (res.failed()) {
                logger.error(res.cause().getMessage(), res.cause());
            } else {
                if (Objects.nonNull(res.result())) {
                    if (Objects.nonNull(res.result().getValue("pwdSalt"))) {//md5验证
                        encyPwd(res.result().put("username", message.body().getString("mail"))
                                .put("loginIP", message.body().getString("loginIP")), message, KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(res.result().getString("pwdSalt") + message.body().getString("password"))));
                    } else {//sha1验证
                        encyPwd(res.result().put("username", message.body().getString("mail"))
                                .put("loginIP", message.body().getString("loginIP")), message, SHA1.encode(message.body().getString("password")));
                    }
                } else {//登陆失败
                    message.reply(null);
                }

            }
        });
    }


    /**
     * @Description 手机号注册
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */

    public void registerTel(Message<JsonObject> message) {
        register(message, "userTel");
    }


    /**
     * @Description email注册
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    public void registerMail(Message<JsonObject> message) {
        register(message, "userMail");
    }


    /**
     * @Description 通用注册
     * @author zhang bo
     * @date 17-12-12
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void register(Message<JsonObject> message, String field) {
        RedisClient.client.get(message.body().getString("versionType") + ":" + message.body().getString("name"), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs.cause());
            } else {
                if (Objects.nonNull(rs.result()) && rs.result().equals(message.body().getString("tokens"))) {//验证码验证通过
                    MongoClient.client.findOne(KdsUser.COLLECT_NAME, new JsonObject().put(field, message.body().getString("name"))
                            .put(KdsUser.VERSION_TYPE, message.body().getString("versionType")), new JsonObject().put(KdsUser._ID, 1), as -> {//是否已经注册
                        if (as.failed()) {
                            logger.error(as.cause().getMessage(), as.cause());
                        } else {
                            if (!Objects.nonNull(as.result())) {//没有注册
                                String password = SHA1.encode(message.body().getString("password"));
                                MongoClient.client.insert(KdsUser.COLLECT_NAME, new JsonObject().put(field, message.body().getString("name"))
                                        .put(KdsUser.USER_PWD, password).put(KdsUser.NICK_NAME, message.body().getString("name"))
                                        .put(KdsUser.VERSION_TYPE, message.body().getString("versionType"))
                                        .put("insertTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))), res -> {
                                    if (res.failed()) {
                                        logger.error(res.cause().getMessage(), res.cause());
                                    } else {
                                        String uid = res.result();
                                        String jwtStr = jwtAuth.generateToken(new JsonObject().put("_id", uid).put("username", message.body().getString("name")),
                                                new JWTOptions());//jwt加密
                                        String[] jwts = StringUtils.split(jwtStr, ".");
                                        RedisClient.client.hset(RedisKeyConf.USER_ACCOUNT + uid, RedisKeyConf.USER_VAL_TOKEN
                                                , jwts[1], jwtrs -> {
                                                    if (jwtrs.failed()) logger.error(jwtrs.cause().getMessage(), jwtrs);
                                                    else {
                                                        RedisClient.client.expire(RedisKeyConf.USER_ACCOUNT + uid
                                                                , config.getLong("liveTime"), times -> {
                                                                    if (times.failed())
                                                                        logger.error(times.cause().getMessage(), times.cause());
                                                                });
                                                    }
                                                });
                                        message.reply(new JsonObject().put("token", jwts[1]).put("uid", uid));
                                        onSynchRegisterUserInfo(new JsonObject().put(KdsUser.USER_PWD, password).put(KdsUser.NICK_NAME, message.body().getString("name"))
                                                .put(KdsUser._ID, uid).put("username", message.body().getString("name")));
                                    }
                                });
                            } else {
                                message.reply(null, new DeliveryOptions().addHeader("code",
                                        String.valueOf(ErrorType.REGISTER_USER_DICT_FAIL.getKey())).addHeader("msg", ErrorType.REGISTER_USER_DICT_FAIL.getValue()));

                            }
                        }
                    });
                } else {
                    message.reply(null, new DeliveryOptions().addHeader("code",
                            String.valueOf(ErrorType.VERIFY_CODE_FAIL.getKey())).addHeader("msg", ErrorType.VERIFY_CODE_FAIL.getValue()));

                }
            }
        });
    }

    /**
     * @Description 檢查用戶的ttl有效時間
     * @author zhang bo
     * @date 18-11-21
     * @version 1.0
     */
    public void verifyUserTTl(String uid) {
        RedisClient.client.ttl(RedisKeyConf.RATE_LIMIT + uid, time -> {
            if (time.failed())
                logger.error(time.cause().getMessage(), time.cause());
            else {
                if (time.result() == -1) {// key 存在但没有设置剩余生存时间时
                    RedisClient.client.del(RedisKeyConf.RATE_LIMIT + uid, rs -> {
                        if (rs.failed()) logger.error(rs.cause().getMessage(), rs);
                    });
                }
            }
        });
    }


    /**
     * @Description 记录用户登录信息
     * @author zhang bo
     * @date 19-1-15
     * @version 1.0
     */
    public void recordUserLogin(JsonObject jsonObject, Message<JsonObject> message) {
        //更新登錄記錄
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        MongoClient.client.findOneAndUpdateWithOptions(KdsUserLog.COLLECT_NAME, new JsonObject().put(KdsUserLog.USER_NAME
                , jsonObject.getString("username")).put(KdsUserLog.VERSION_TYPE, message.body().getString("versionType"))
                , new JsonObject().put("$set", new JsonObject().put(KdsUserLog.USER_NAME, jsonObject.getString("username"))
                        .put(KdsUserLog.LOGIN_TIME, time).put(KdsUserLog.LOGIN_IP, jsonObject.getString("loginIP"))
                        .put(KdsUserLog.VERSION_TYPE, message.body().getString("versionType")))
                , new FindOptions(), new UpdateOptions().setUpsert(true), logtime -> {
                    if (logtime.failed()) logger.error(logtime.cause().getMessage(),logtime);
                });
    }



    /**
     * @Description 检验密码
     * @author zhang bo
     * @date 17-12-11
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void encyPwd(JsonObject jsonObject, Message<JsonObject> message, String pwd) {
        if (pwd.equals(jsonObject.getString("userPwd"))) {
            String jwtStr = jwtAuth.generateToken(new JsonObject().put("_id", jsonObject.getString("_id"))
                    .put("username", jsonObject.getString("username")), new JWTOptions());//jwt加密
            String[] jwts = StringUtils.split(jwtStr, ".");
            verifyUserTTl(jsonObject.getString("_id"));

            if (Objects.nonNull(jsonObject.getValue("username"))) {//同步数据
                onSynchUserInfo(jsonObject, jwts[1], config.getLong("liveTime"), res -> {
                    if (res.failed()) {
                        message.reply(null);
                    } else {
                        if (res.result()) {
                            message.reply(new JsonObject().put("uid", jsonObject.getString("_id")).put("token", jwts[1])
                                    .put("meUsername", jsonObject.getString("meUsername")).put("mePwd", jsonObject.getString("mePwd")));
                            recordUserLogin(jsonObject, message);
                        } else {
                            message.reply(null);
                        }
                    }
                });
            } else {
                message.reply(null);
            }

        } else {
            message.reply(null);
        }

    }


    /**
     * @Description 获取昵称
     * @author zhang bo
     * @date 17-12-13
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getNickname(Message<JsonObject> message) {
        RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT + message.body().getString("uid"), RedisKeyConf.USER_VAL_INFO, ars -> {
            if (ars.failed()) {
                logger.error(ars.cause().getMessage(), ars.cause());
            } else {
                if (Objects.nonNull(ars.result())) {
                    message.reply(new JsonObject().put(KdsUser.NICK_NAME, new JsonObject(ars.result()).getString("nickName")));
                } else {
                    MongoClient.client.findOne(KdsUser.COLLECT_NAME, new JsonObject().put(KdsUser._ID, new JsonObject().put("$oid", message.body().getString("uid")))
                                    .put(KdsUser.VERSION_TYPE, message.body().getString("versionType")),
                            new JsonObject().put(KdsUser.NICK_NAME, "").put(KdsUser._ID, 0), rs -> {
                                if (rs.failed()) {
                                    logger.error(rs.cause().getMessage(), rs.cause());
                                } else {
                                    if (Objects.nonNull(rs.result())) {
                                        message.reply(rs.result());
                                    } else {
                                        message.reply(null);
                                    }
                                }
                            });
                }
            }
        });
    }


    /**
     * @Description 修改用户昵称
     * @author zhang bo
     * @date 17-12-14
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void updateNickname(Message<JsonObject> message) {
        RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT + message.body().getString("uid"), RedisKeyConf.USER_VAL_INFO, ars -> {
            if (ars.failed()) {
                logger.error(ars.cause().getMessage(), ars.cause());
            } else {
                if (Objects.nonNull(ars.result())) {
                    RedisClient.client.hset(RedisKeyConf.USER_ACCOUNT + message.body().getString("uid"), RedisKeyConf.USER_VAL_INFO
                            , new JsonObject(ars.result()).put("nickName", message.body().getString("nickname")).toString(), rs -> {
                                if (rs.failed()) {
                                    logger.error(rs.cause().getMessage(), rs.cause());
                                } else {
                                    message.reply(new JsonObject());
                                    //异步同步信息
                                    MongoClient.client.updateCollection(KdsUser.COLLECT_NAME, new JsonObject().put(KdsUser._ID, new JsonObject().put("$oid", message.body().getString("uid")))
                                            , new JsonObject().put("$set", new JsonObject().put(KdsUser.NICK_NAME, message.body().getString("nickname"))), mrs -> {
                                                if (mrs.failed()) {
                                                    logger.error(mrs.cause().getMessage(), mrs.cause());
                                                } else {
                                                    if (Objects.nonNull(mrs.result()) && mrs.result().getDocModified() == 1) {
                                                        message.reply(new JsonObject());
                                                    } else {
                                                        message.reply(null);
                                                    }
                                                }
                                            });
                                }
                            });
                } else {
                    message.reply(null);
                }
            }
        });

    }


    /**
     * @Description 修改用户密码
     * @author zhang bo
     * @date 17-12-14
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void updateUserpwd(Message<JsonObject> message) {
        RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT + message.body().getString("uid"), RedisKeyConf.USER_VAL_INFO, as -> {
            if (as.failed()) {
                logger.error(as.cause().getMessage(), as.cause());
            } else {
                if (Objects.nonNull(as.result())) {
                    JsonObject jsonObject = new JsonObject(as.result());
                    if (Objects.nonNull(jsonObject.getValue("pwdSalt"))) {//MD5
                        if (jsonObject.getString("userPwd").equals(KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(
                                jsonObject.getString("pwdSalt") + message.body().getString("oldpwd")))))
                            MongoClient.client.updateCollection(KdsUser.COLLECT_NAME, new JsonObject().put(KdsUser._ID, new JsonObject().put("$oid", message.body().getString("uid")))
                                    , new JsonObject().put("$set", new JsonObject().put(KdsUser.USER_PWD, KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(
                                            jsonObject.getString("pwdSalt") + message.body().getString("newpwd"))))), rs -> {
                                        if (rs.failed()) {
                                            logger.error(rs.cause().getMessage(), rs.cause());
                                        } else {
                                            if (Objects.nonNull(rs.result()) && rs.result().getDocModified() == 1) {
                                                message.reply(new JsonObject());
                                                onSynchUpdateUserInfo(new JsonObject().put(KdsUser.USER_PWD, KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(
                                                        jsonObject.getString("pwdSalt") + message.body().getString("newpwd"))))
                                                        .put("uid", message.body().getString("uid")));
                                            } else {
                                                message.reply(null);
                                            }
                                        }
                                    });
                        else
                            message.reply(null);
                    } else {//SHA-1
                        if (jsonObject.getString("userPwd").equals(SHA1.encode(message.body().getString("oldpwd"))))
                            MongoClient.client.updateCollection(KdsUser.COLLECT_NAME, new JsonObject().put(KdsUser._ID, new JsonObject().put("$oid", message.body().getString("uid")))
                                    , new JsonObject().put("$set", new JsonObject().put(KdsUser.USER_PWD, SHA1.encode(message.body().getString("newpwd")))), rs -> {
                                        if (rs.failed()) {
                                            logger.error(rs.cause().getMessage(), rs.cause());
                                        } else {
                                            if (Objects.nonNull(rs.result()) && rs.result().getDocModified() == 1) {
                                                message.reply(new JsonObject());
                                                onSynchUpdateUserInfo(new JsonObject().put(KdsUser.USER_PWD, SHA1.encode(message.body().getString("newpwd")))
                                                        .put("uid", message.body().getString("uid")));
                                            } else {
                                                message.reply(null);
                                            }
                                        }
                                    });
                        else
                            message.reply(null);
                    }
                } else {
                    message.reply(null);
                }
            }
        });


    }

    /**
     * @Description 忘记密码
     * @author zhang bo
     * @date 17-12-14
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void forgetUserpwd(Message<JsonObject> message) {
        RedisClient.client.get(message.body().getString("versionType") + ":" + message.body().getString("name"), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs.cause());
            } else {
                if (Objects.nonNull(rs.result()) && rs.result().equals(message.body().getString("tokens"))) {//验证码验证通过
                    String field = "";
                    if (message.body().getInteger("type") == 1) {//手机
                        field = "userTel";
                    } else {//邮箱
                        field = "userMail";
                    }
                    String finalField = field;
                    MongoClient.client.findOne(KdsUser.COLLECT_NAME, new JsonObject().put(field, message.body().getString("name"))
                                    .put(KdsUser.VERSION_TYPE, message.body().getString("versionType")),
                            new JsonObject().put(KdsUser.PWD_SALT, "").put(KdsUser._ID, 1), mrs -> {
                                if (mrs.failed()) {
                                    logger.error(mrs.cause().getMessage(), mrs.cause());
                                } else {
                                    if (Objects.nonNull(mrs.result())) {//账户是否存在
                                        String pwd = "";
                                        if (Objects.nonNull(mrs.result().getValue(KdsUser.PWD_SALT))) {//MD5
                                            pwd = KdsCreateMD5.getMd5(KdsCreateMD5.getMd5(
                                                    mrs.result().getString(KdsUser.PWD_SALT) + message.body().getString("pwd")));
                                        } else {
                                            pwd = SHA1.encode(message.body().getString("pwd"));
                                        }
                                        //重置密码
                                        MongoClient.client.updateCollection(KdsUser.COLLECT_NAME, new JsonObject().put(finalField, message.body().getString("name"))
                                                        .put(KdsUser.VERSION_TYPE, message.body().getString(KdsUser.VERSION_TYPE))
                                                , new JsonObject().put("$set", new JsonObject().put(KdsUser.USER_PWD, pwd)), res -> {
                                                    if (res.failed()) {
                                                        logger.error(res.cause().getMessage(), res.cause());
                                                    } else if (res.succeeded()) {
                                                        message.reply(new JsonObject());
                                                    } else {
                                                        message.reply(null);
                                                    }
                                                });
                                        onSynchUpdateUserInfo(new JsonObject().put(KdsUser.USER_PWD, pwd).put("uid", mrs.result().getString("_id")));
                                    } else {
                                        message.reply(null, new DeliveryOptions().addHeader("code",
                                                String.valueOf(ErrorType.RESULT_CODE_FAIL.getKey())).addHeader("msg", ErrorType.RESULT_CODE_FAIL.getValue()));
                                    }

                                }
                            });
                } else {
                    message.reply(null, new DeliveryOptions().addHeader("code",
                            String.valueOf(ErrorType.VERIFY_CODE_FAIL.getKey())).addHeader("msg", ErrorType.VERIFY_CODE_FAIL.getValue()));
                }
            }
        });
    }


    /**
     * @Description 用户留言
     * @author zhang bo
     * @date 17-12-14
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void suggestMsg(Message<JsonObject> message) {
        MongoClient.client.insert(KdsSuggest.COLLECT_NAME, message.body().put(KdsSuggest.TIME, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))), rs -> {
            if (rs.failed()) {
                logger.error(rs.cause().getMessage(), rs.cause());
            } else {
                message.reply(new JsonObject());
            }
        });
    }


    /**
     * @Description 用户登出
     * @author zhang bo
     * @date 17-12-20
     * @version 1.0
     */
    public void logOut(Message<JsonObject> message) {
        String uid = new JsonObject(new String(Base64.decodeBase64(message.body().getString("token")))).getString("_id");
        RedisClient.client.del(RedisKeyConf.USER_ACCOUNT + uid, rs -> {
            if (rs.failed()) logger.error(rs.cause().getMessage(), rs.cause());
        });//清除token
        message.reply(new JsonObject());
    }


    /**
     * @Description 米米网同步用户
     * @author zhang bo
     * @date 18-1-12
     * @version 1.0
     */
    public void meMeUser(Message<JsonObject> message) {
        //同步db
//        MongoClient.client.updateCollectionWithOptions(KdsUser.COLLECT_NAME, new JsonObject().put(KdsUser._ID, new JsonObject().put("$oid", message.body().getString("uid")))
//                , new JsonObject().put("$set", new JsonObject().put(KdsUser.ME_USERNAME, message.body().getString("username"))
//                        .put(KdsUser.ME_PWD, message.body().getString("password")).put(KdsUser.USER_ID, message.body().getLong("userid")))
//                , new UpdateOptions().setUpsert(true).setMulti(false), rs -> {
//                    if (rs.failed()) {
//                        logger.error(rs.cause().getMessage(), rs.cause());
//                    } else
//                        //同步緩存
//                        RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT + message.body().getString("uid"), RedisKeyConf.USER_VAL_INFO, res -> {
//                            if (res.failed()) {
//                                logger.error(res.cause().getMessage(), res.cause());
//                            } else {
//                                if (Objects.nonNull(res.result()))
//                                    RedisClient.client.hset(RedisKeyConf.USER_ACCOUNT + message.body().getString("uid"), RedisKeyConf.USER_VAL_INFO
//                                            , new JsonObject(res.result()).put("meUsername", message.body().getString("username"))
//                                                    .put("mePwd", message.body().getString("password"))
//                                                    .put("userid", message.body().getLong("userid")).toString(), as -> {
//                                                if (as.failed())
//                                                    logger.error(as.cause().getMessage(), as.cause());
//                                            });
//                            }
//                        });
//                });

        JsonObject params = new JsonObject().put(KdsGatewayDeviceList.UID, message.body().getString("uid"));
        if (message.body().getValue("devuuid") != null)
            params.put(KdsGatewayDeviceList.DEVICE_SN, message.body().getString("devuuid"));
        MongoClient.client.updateCollectionWithOptions(KdsGatewayDeviceList.COLLECT_NAME, params
                , new JsonObject().put("$set", new JsonObject().put(KdsGatewayDeviceList.ME_USER_NAME, message.body().getString("username")).put("mePwd", message.body().getString("password"))
                        .put(KdsGatewayDeviceList.USER_ID, message.body().getLong("userid")))
                , new UpdateOptions().setUpsert(false).setMulti(false), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs);
                        message.reply(null);
                    } else {
                        if (rs.result().getDocMatched() > 0)
                            message.reply(new JsonObject());
                        else
                            message.reply(null);
                    }
                });
    }


    /**
     * @Description 米米网用户批量注册
     * @author zhang bo
     * @date 18-1-26
     * @version 1.0
     */
    public void meMeUserBulk(Message<JsonObject> message) {
        MongoClient.client.count(KdsUser.COLLECT_NAME, new JsonObject().put(KdsUser.USER_ID, new JsonObject().put("$exists", false))
                , rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs.cause());
                        message.reply(null);
                    } else {
                        if (Objects.nonNull(rs.result()) && rs.result() > 0) {//存在未注册的用户
                            boolean flag = rs.result() % 100 == 0 ? true : false;//是否是倍数
                            Long num = rs.result() / 100;//次数
                            if (flag) {
                                bulkRequestRegister(num, 100L);
                                message.reply(new JsonObject());
                            } else {
                                bulkRequestRegister(num, 100L);
                                Long endTotal = rs.result() - 100 * num;//100倍数的余数
                                bulkRequestRegister(1L, endTotal);//餘下一次
                                message.reply(new JsonObject());
                            }
                        } else {
                            message.reply(new JsonObject());
                        }
                    }
                });
    }


    /**
     * @Description 批量请求注册 涕归
     * @author zhang bo
     * @date 18-1-27
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public boolean bulkRequestRegister(Long num, Long count) {
        if (num == 0) {
            return true;
        } else {
            Future.<List<JsonObject>>future(f -> MongoClient.client.findWithOptions(KdsUser.COLLECT_NAME, new JsonObject().put(KdsUser.USER_ID
                    , new JsonObject().put("$exists", false)), new FindOptions().setFields(new JsonObject().put(KdsUser._ID, 1))
                    .setLimit(count.intValue()), f))
                    .compose(users -> {//异步处理数据
                        if (Objects.nonNull(users) && users.size() > 0) {//用户id
                            List<BulkOperation> bulkOperations = new ArrayList<>();
                            JsonArray jsonArray = new JsonArray();//用户信息集合
                            for (int j = 0; j < count; j++) {//最大100次
                                String username = UUIDUtils.getUUID();
                                String password = UUIDUtils.getUUID();
                                jsonArray.add(new JsonObject().put("username", username).put("password", password));
                                //批量處理
                                JsonObject params = new JsonObject().put("type", BulkOperation.BulkOperationType.UPDATE)
                                        .put("filter", new JsonObject().put("_id", new JsonObject().put("$oid", users.get(j).getString("_id"))))
                                        .put("document", new JsonObject().put("$set",
                                                new JsonObject().put("meUsername", username).put("mePwd", password)))
                                        .put("upsert", true).put("multi", false);
                                bulkOperations.add(new BulkOperation(params));
                            }
                            return Future.<Map<String, Object>>future(f -> f.complete(new HashMap<String, Object>() {{
                                put("jsonList", jsonArray);
                                put("bulks", bulkOperations);
                            }}));
                        } else {
                            return Future.future(f -> f.fail("null data"));
                        }
                    }).compose(f ->//异步请求第三方接口
                    Future.<List<BulkOperation>>future(fu -> vertx.eventBus().send(MemenetAddr.class.getName() + REGISTER_USER_BULK, new JsonArray(f.get("jsonList").toString())
                            , (AsyncResult<Message<JsonObject>> as) -> {
                                if (as.failed()) {
                                    logger.error(as.cause().getMessage(), as.cause());
                                    Future.future(e -> e.fail("request ===result ->  error"));
                                } else {
                                    JsonObject jsonObject = as.result().body().getJsonArray("results").getJsonObject(0);
                                    JsonArray results = as.result().body().getJsonArray("results");
                                    if (jsonObject.getInteger("result") == 0) {//成功
                                        logger.info("=========success============" + jsonObject.toString());
                                        List<BulkOperation> bulkOperations = ((List<BulkOperation>) f.get("bulks"));
                                        bulkOperations.forEach(e -> {
                                            Long userid = results.stream().filter(r -> new JsonObject(r.toString()).getString("username")
                                                    .equals(e.getDocument().getJsonObject("$set").getString("meUsername")))
                                                    .map(uid -> new JsonObject(uid.toString()).getLong("userid")).findFirst().orElse(null);
                                            if (Objects.nonNull(userid))
                                                e.getDocument().getJsonObject("$set").put("userid", userid);
                                        });
                                        fu.complete(bulkOperations);
                                    } else {
                                        logger.error("=========error============" + jsonObject.toString());
                                        fu.fail("request ===result ->  error");
                                    }
                                }
                            }))
            ).setHandler(f -> {//接口返回处理
                if (f.failed()) {
                    logger.error(f.cause().getMessage(), f);
                } else {
                    //修改用户数据
                    MongoClient.client.bulkWrite(KdsUser.COLLECT_NAME, f.result(), ars -> {
                        if (ars.failed()) {
                            logger.error(ars.cause().getMessage(), ars.cause());
                        } else {
                            logger.info("=========mongoBulk============" + JsonObject.mapFrom(ars.result()).toString());
                            bulkRequestRegister(num - 1, count);
                        }
                    });
                }
            });
        }
        return false;
    }


    /**
     * @Description 獲取網關管理員
     * @author zhang bo
     * @date 18-5-4
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void selectGWAdmin(Message<JsonObject> message) {
        MongoClient.client.findOne(KdsGatewayDeviceList.COLLECT_NAME,
                new JsonObject().put(KdsGatewayDeviceList.DEVICE_SN, message.body().getString("gwId")
                ).put(KdsGatewayDeviceList.ADMIN_UID, new JsonObject().put("$exists", true)), new JsonObject()
                        .put(KdsGatewayDeviceList._ID, 0).put(KdsGatewayDeviceList.ADMIN_UID, 1), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs.cause());
                        message.reply(null);
                    } else {
                        if (Objects.nonNull(rs.result())) {
                            message.reply(rs.result());
                        } else {
                            message.reply(null);
                        }
                    }
                });
    }


    /**
     * @Description 上報pushId
     * @author zhang bo
     * @date 18-5-4
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void uploadPushId(Message<JsonObject> message) {
        logger.info("==params -> " + message.body());
        JsonObject params = new JsonObject().put("JPushId", message.body().getString("JPushId")).put("type", message.body().getInteger("type"));
        if (message.body().getValue("VoIPId") != null)
            params.put("VoIPId", message.body().getString("VoIPId"));
        RedisClient.client.hset(RedisKeyConf.USER_ACCOUNT + message.body().getString("uid"), RedisKeyConf.USER_PUSH_ID,
                params.toString(), rs -> {
                    if (rs.failed()) {
                        logger.error(rs.cause().getMessage(), rs.cause());
                        message.reply(null);
                    } else {
                        message.reply(new JsonObject());
                    }
                });
    }

    /**
     * @Description 获取PushId
     * @author zhang bo
     * @date 18-5-4
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void getPushId(Message<JsonObject> message) {
        logger.info("==params -> " + message.body());
        if (message.body().getValue("uid") != null)
            RedisClient.client.hget(RedisKeyConf.USER_ACCOUNT + message.body().getString("uid"), RedisKeyConf.USER_PUSH_ID, rs -> {
                if (rs.failed()) {
                    logger.error(rs.cause().getMessage(), rs.cause());
                    message.reply(null);
                } else {
                    if (Objects.nonNull(rs.result()))
                        message.reply(new JsonObject(rs.result()));
                    else
                        message.reply(null);
                }
            });
        else
            message.reply(null);
    }

    /**
     * @Description 发送branch记录
     * @author zhang bo
     * @date 18-5-4
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void branchSendRecord(Message<JsonObject> message) {
        if (message.body().getValue("callId") != null && message.body().getValue("deviceId") != null) {
            RedisClient.client.hsetnx(RedisKeyConf.SESSION_BRANCH + message.body().getString("callId"), RedisKeyConf.SIP_VAL_COUNT, "", rs -> {
                if (rs.failed()) {
                    logger.error(rs.cause().getMessage(), rs.cause());
                    message.reply(null);
                } else {
                    if (Objects.nonNull(rs.result()) && rs.result() == 1) {
                        MongoClient.client.findOne(KdsGatewayDeviceList.COLLECT_NAME, new JsonObject().put("deviceList.deviceId", message.body().getString("deviceId"))
                                .put("deviceList.event_str", "online"), new JsonObject().put(KdsGatewayDeviceList._ID, 0).put(KdsGatewayDeviceList.DEVICE_SN, 1), ars -> {
                            if (ars.failed()) {
                                logger.error(rs.cause().getMessage(), rs.cause());
                                message.reply(null);
                            } else {
                                message.reply(ars.result());
                            }
                        });
                    } else {
                        message.reply(null);
                    }
                }
            });
        } else {
            message.reply(null);
        }
    }

}
