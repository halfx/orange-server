package cn.orangeiot.auth.handler.user;

import cn.orangeiot.reg.user.UserAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2017-12-11
 */
public class UserHandler implements UserAddr{
    private static Logger logger = LoggerFactory.getLogger(UserHandler.class);


    private Vertx vertx;

    private JsonObject config;

    public UserHandler(Vertx vertx, JsonObject config) {
        this.vertx=vertx;
        this.config=config;
    }

    /**
     * @Description 手机登录
     * @author zhang bo
     * @date 17-12-11
     * @version 1.0
     */
    public void onByTelMessage(Message<JsonObject> message){
        vertx.eventBus().send(UserAddr.class.getName()+VERIFY_TEL,message.body(),(AsyncResult<Message<JsonObject>> rs)->{
            if(rs.failed()){
                rs.cause().printStackTrace();
            }else{
                message.reply(rs.result().body());
            }
        });
    }


    /**
     * @Description mail登录
     * @author zhang bo
     * @date 17-12-11
     * @version 1.0
     */
    public void onByMailMessage(Message<JsonObject> message){
        vertx.eventBus().send(UserAddr.class.getName()+VERIFY_MAIL,message.body(),(AsyncResult<Message<JsonObject>> rs)->{
            if(rs.failed()){
                rs.cause().printStackTrace();
            }else{
                message.reply(rs.result().body());
            }
        });
    }
}
