package cn.orangeiot.apidao.handler;

import cn.orangeiot.apidao.client.MongoClient;
import cn.orangeiot.apidao.client.RedisClient;
import cn.orangeiot.apidao.handler.dao.admindev.AdminDevDao;
import cn.orangeiot.apidao.handler.dao.file.FileDao;
import cn.orangeiot.apidao.handler.dao.job.JobDao;
import cn.orangeiot.apidao.handler.dao.message.MessageDao;
import cn.orangeiot.apidao.handler.dao.topic.TopicDao;
import cn.orangeiot.apidao.handler.dao.user.UserDao;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.adminlock.AdminlockAddr;
import cn.orangeiot.reg.file.FileAddr;
import cn.orangeiot.reg.message.MessageAddr;
import cn.orangeiot.reg.user.UserAddr;
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
public class RegisterHandler implements EventbusAddr{

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


            //注册mongoclient
            MongoClient mongoClient =new MongoClient();
            mongoClient.mongoConf(vertx);

            //注册redisclient
            RedisClient redisClient=new RedisClient();
            redisClient.redisConf(vertx);

            //topic处理
            TopicDao topicHandler=new TopicDao();
            vertx.eventBus().consumer(config.getString("consumer_saveTopic"),topicHandler::saveTopic);
            vertx.eventBus().consumer(config.getString("consumer_delTopic"),topicHandler::saveTopic);

            //离线消息储存
            MessageDao messageHandler=new MessageDao();
            vertx.eventBus().consumer(config.getString("consumer_saveOfflineMessage"),messageHandler::onSaveOfflineMsg);
            vertx.eventBus().consumer(MessageAddr.class.getName()+SAVE_CODE,messageHandler::onSaveVerityCode);
            vertx.eventBus().consumer(MessageAddr.class.getName()+GET_CODE_COUNT,messageHandler::onGetCodeCount);

            //连接处理
            UserDao userDao=new UserDao();
            vertx.eventBus().consumer(config.getString("consumer_connect_dao"),userDao::getUser);
            vertx.eventBus().consumer(UserAddr.class.getName()+VERIFY_TEL,userDao::telLogin);
            vertx.eventBus().consumer(UserAddr.class.getName()+VERIFY_MAIL,userDao::mailLogin);
            vertx.eventBus().consumer(UserAddr.class.getName()+VERIFY_LOGIN,userDao::verifyLogin);
            vertx.eventBus().consumer(UserAddr.class.getName()+REGISTER_USER_TEL,userDao::registerTel);
            vertx.eventBus().consumer(UserAddr.class.getName()+REGISTER_USER_MAIL,userDao::registerMail);
            vertx.eventBus().consumer(UserAddr.class.getName()+GET_USER_NICKNAME,userDao::getNickname);
            vertx.eventBus().consumer(UserAddr.class.getName()+UPDATE_USER_NICKNAME,userDao::updateNickname);
            vertx.eventBus().consumer(UserAddr.class.getName()+UPDATE_USER_PWD,userDao::updateUserpwd);
            vertx.eventBus().consumer(UserAddr.class.getName()+FORGET_USER_PWD,userDao::forgetUserpwd);
            vertx.eventBus().consumer(UserAddr.class.getName()+SUGGEST_MSG,userDao::suggestMsg);
            vertx.eventBus().consumer(UserAddr.class.getName()+SUGGEST_MSG,userDao::suggestMsg);
            vertx.eventBus().consumer(UserAddr.class.getName()+USER_LOGOUT,userDao::logOut);

            //文件处理
            FileDao fileDao=new FileDao();
            vertx.eventBus().consumer(FileAddr.class.getName()+GET_FILE_HEADER,fileDao::onGetHeaderImg);
            vertx.eventBus().consumer(FileAddr.class.getName()+UPLOAD_HEADER_IMG,fileDao::onUploadHeaderImg);

            //job处理
            JobDao jobDao=new JobDao();
            vertx.eventBus().consumer(config.getString("consumer_verifyCodeCron"),jobDao::onMsgVerifyCodeCount);

            //锁相关处理
            AdminDevDao adminDevDao=new AdminDevDao();
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+CREATE_ADMIN_DEV,adminDevDao::createAdminDev);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+DELETE_EVEND_DEV,adminDevDao::deletevendorDev);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+DELETE_ADMIN_DEV,adminDevDao::deleteAdminDev);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+DELETE_NORMAL_DEV,adminDevDao::deleteNormalDev);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+CREATE_NORMAL_DEV,adminDevDao::createNormalDev);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+GET_OPEN_LOCK_RECORD,adminDevDao::downloadOpenLocklist);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+UPDATE_USER_PREMISSON,adminDevDao::updateNormalDevlock);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+REQUEST_USER_OPEN_LOCK,adminDevDao::adminOpenLock);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+GET_DEV_LIST,adminDevDao::getAdminDevlist);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+GET_DEV_USER_LIST,adminDevDao::getNormalDevlist);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+EDIT_ADMIN_DEV,adminDevDao::editAdminDev);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+GET_DEV_LONGTITUDE,adminDevDao::getAdminDevlocklongtitude);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+UPDATE_ADMIN_DEV_AUTO_LOCK,adminDevDao::updateAdminDevAutolock);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+UPDATE_DEV_NICKNAME,adminDevDao::updateAdminlockNickName);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+CHECK_DEV,adminDevDao::checkAdmindev);
            vertx.eventBus().consumer(AdminlockAddr.class.getName()+UPLOAD_OPEN_LOCK_RECORD,adminDevDao::uploadOpenLockList);
        } else {
            // failed!
            logger.fatal(res.cause().getMessage(), res.cause());
        }
    }



}
