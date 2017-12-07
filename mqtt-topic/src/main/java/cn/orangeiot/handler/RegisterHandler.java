package cn.orangeiot.handler;

import cn.orangeiot.handler.topic.TopicHandler;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @author zhang bo
 * @version 1.0 集群的handler事件注册
 * @Description
 * @date 2017-11-23
 */
public class RegisterHandler {

    private static Logger logger = LoggerFactory.getLogger(RegisterHandler.class);

    private JsonObject config;

    public RegisterHandler(JsonObject config) {
        this.config=config;
    }

    /**
     * @Description 注册事件
     * @author zhang bo
     * @date 17-11-23
     * @version 1.0
     */
    public void consumer(AsyncResult<Vertx> res){
        if (res.succeeded()) {
            Vertx vertx = res.result();

            //主题处理
            TopicHandler topicHandler=new TopicHandler(vertx,config);
            vertx.eventBus().consumer(config.getString("consumer_saveTopic"),topicHandler::onSaveMessage);
            vertx.eventBus().consumer(config.getString("consumer_delTopic"),topicHandler::onDelMessage);
        } else {
            // failed!
            logger.fatal(res.cause().getMessage(), res.cause());
        }
    }

}
