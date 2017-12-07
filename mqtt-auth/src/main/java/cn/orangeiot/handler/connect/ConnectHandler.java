package cn.orangeiot.handler.connect;


import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * @author zhang bo  mqtt连接验证
 * @version 1.0
 * @Description
 * @date 2017-11-23
 */
public class ConnectHandler {

    private static Logger logger = LoggerFactory.getLogger(ConnectHandler.class);


    private Vertx vertx;

    private JsonObject config;

    public ConnectHandler(Vertx vertx, JsonObject config) {
        this.vertx=vertx;
        this.config=config;
    }

    public void onMessage(Message message){
        if(Objects.nonNull(message.body())){
            logger.info("==ConnectHandler=onMessage=params:"+message.body());
            JsonObject jsonObject=new JsonObject(message.body().toString());

            /*查找缓存*/
            vertx.eventBus().send(config.getString("send_connect_cache"),message.body(),(AsyncResult<Message<Boolean>> rs)->{
                   if(rs.result().body()){//验证成功
                       message.reply(new JsonObject().put("token", UUID.randomUUID().toString()).put("authorized_user",
                               jsonObject.getString("username")).put("auth_valid", rs.result().body()));
                   }else{
                       vertx.eventBus().send(config.getString("send_connect_dao"),message.body(),(AsyncResult<Message<JsonObject>> as)->{//查询数据层
                         replyResult(message,as.result().body());
                       });
                   }
            });


        }
    }


    /**
     * @Description 返回结果
     * @author zhang bo
     * @date 17-11-27
     * @version 1.0
     */
    public void replyResult(Message message,JsonObject jsonObject){
        if(jsonObject.getBoolean("code")) {
            message.reply(new JsonObject().put("token", UUID.randomUUID().toString()).put("authorized_user",
                    jsonObject.getString("username")).put("auth_valid", jsonObject.getBoolean("code")));

            vertx.eventBus().send(config.getString("send_synch_user"), jsonObject);//同步数据
        }
    }

}
