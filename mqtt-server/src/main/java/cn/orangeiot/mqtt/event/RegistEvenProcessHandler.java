package cn.orangeiot.mqtt.event;

import cn.orangeiot.common.options.SendOptions;
import cn.orangeiot.mqtt.MQTTSession;
import cn.orangeiot.mqtt.MQTTSocket;
import cn.orangeiot.mqtt.parser.MQTTEncoder;
import cn.orangeiot.mqtt.util.QOSConvertUtils;
import cn.orangeiot.reg.EventbusAddr;
import cn.orangeiot.reg.gateway.GatewayAddr;
import cn.orangeiot.reg.log.LogAddr;
import cn.orangeiot.reg.message.MessageAddr;
import io.vertx.core.AsyncResult;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dna.mqtt.moquette.proto.messages.*;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType.EXACTLY_ONCE;
import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType.LEAST_ONE;
import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType.MOST_ONE;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-09-27
 */
public class RegistEvenProcessHandler implements EventbusAddr {

    private static Logger logger = LogManager.getLogger(RegistEvenProcessHandler.class);

    private Vertx vertx;

    private Map<String, MQTTSession> sessions;

    private final String GATEWAY_ON_OFF_STATE = "gatewayState";//網關狀態

    private final String REPLY_MESSAGE = "/clientId/rpc/reply";

    private final String USER_PREFIX = "app:";//用戶前綴

    private final String GATEWAY_PREFIX = "gw:";//网关前綴

    public RegistEvenProcessHandler(Vertx vertx, Map<String, MQTTSession> sessions) {
        this.vertx = vertx;
        this.sessions = sessions;
    }


    /**
     * @Description 初始化時間處理
     * @author zhang bo
     * @date 18-9-27
     * @version 1.0
     */
    public void initHandle() {
        sendMessage();
        sendGWMessage();
        sendStorage();
        getloginAll();
        deviceState();
        sendPubRel();
        kickOut();
        sendUpgradeMessage();
    }

    /**
     * @Description qos发送消息
     * @author zhang bo
     * @date 18-9-27
     * @version 1.0
     */
    private void sendMessage() {
        vertx.eventBus().consumer(MessageAddr.class.getName() + SEND_ADMIN_MSG, (Message<JsonObject> rs) -> {
            String topicName = SEND_USER_REPLAY.replace("clientId", rs.headers().get("uid").replace("app:", ""));
            rs.headers().set("topicName", topicName);
            sendMessageProcess(rs);
        });
    }

    /**
     * @Description qos发送消息
     * @author zhang bo
     * @date 18-9-27
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    private void sendGWMessage() {
        vertx.eventBus().consumer(MessageAddr.class.getName() + SEND_UPGRADE_MSG, (Message<JsonObject> rs) -> {
            String topicName = rs.headers().get("topic");
            if (Objects.nonNull(topicName))
                rs.headers().set("topicName", topicName);
            sendMessageProcess(rs);
        });
    }


    /**
     * @Description qos发送升級消息
     * @author zhang bo
     * @date 18-9-27
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    private void sendUpgradeMessage() {
        vertx.eventBus().consumer(MessageAddr.class.getName() + SEND_VERSION_UPGRADE_MSG, (Message<JsonObject> rs) -> {
            String topicName = rs.headers().get("topic");
            if (Objects.nonNull(topicName))
                rs.headers().set("topicName", topicName);

            vertx.executeBlocking(e -> {
                JsonArray jsonArray = rs.body().getJsonArray("ids");
                int size = jsonArray.size();
                rs.body().remove("ids");
                for (int i = 0; i < size; i++) {
                    String SN = jsonArray.getString(i);
                    rs.headers().add("uid"
                            , "gw:" + SN).add("topicName", MessageAddr.SEND_GATEWAY_REPLAY.replace("gwId", SN));
                    rs.body().put("gwId", SN).put("deviceId", SN).put("deviceList", new JsonArray().add(SN));
                    sendMessageProcess(rs);
                }
                e.complete();
            }, null);
        });
    }


    /**
     * @Description 发送storage消息
     * @author zhang bo
     * @date 18-9-27
     * @version 1.0
     */
    private void sendStorage() {
        vertx.eventBus().consumer(MessageAddr.class.getName() + SEND_STORAGE_MSG, (Message<JsonObject> rs) -> {
            String topicName = rs.headers().get("topic");
            rs.headers().set("topicName", topicName);
            sendMessageProcess(rs);
        });
    }


    /**
     * @Description 獲取登錄打狀態
     * @author zhang bo
     * @date 18-9-27
     * @version 1.0
     */
    private void getloginAll() {
        vertx.eventBus().consumer("cn.login.all", msg -> {
            msg.reply(new JsonArray(sessions.keySet().stream().collect(Collectors.toList())));
        });
    }


    /**
     * @Description 設備狀態
     * @author zhang bo
     * @date 18-9-13
     * @version 1.0
     */
    private void deviceState() {
        vertx.eventBus().consumer(GatewayAddr.class.getName() + SEND_GATEWAY_STATE, (Message<JsonObject> rs) -> {
            if (Objects.nonNull(rs.body()) && Objects.nonNull(rs.body().getValue("_id")) && Objects.nonNull(rs.body().getValue("gwId")))
                sendDeviceState(rs.body().getString("_id"), new JsonArray().add(new JsonObject().put("deviceSN", rs.body().getString("gwId"))
                        .put("uid", rs.body().getString("_id"))), "online", true);
        });
    }


    /**
     * @Description 发送pubRel消息
     * @author zhang bo
     * @date 18-9-27
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    private void sendPubRel() {
        vertx.eventBus().consumer(MessageAddr.class.getName() + SEND_PUBREL_MSG, (Message<JsonObject> rs) -> {
            PubRelMessage prelResp = new PubRelMessage();
            prelResp.setMessageID(Integer.parseInt(rs.body().getString("relId")));
            prelResp.setQos(LEAST_ONE);
            try {
                Buffer msg = new MQTTEncoder().enc(prelResp);
                DeliveryOptions opt = new DeliveryOptions().addHeader(MQTTSession.TENANT_HEADER, rs.headers().get("uid"));
                vertx.eventBus().publish(MQTTSession.TENANT_HEADER, msg, opt);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
            if (Objects.nonNull(sessions.get(rs.body().getString("clientid"))))
                sessions.get(rs.body().getString("clientid")).sendMessageToClient(prelResp);
        });
    }


    /**
     * @Description 踢出
     * @author zhang bo
     * @date 18-9-27
     * @version 1.0
     */
    private void kickOut() {
        vertx.eventBus().consumer(MessageAddr.class.getName() + KICK_OUT, (Message<JsonObject> rs) -> {
            MQTTSession session = null;
            if (Objects.nonNull(rs.headers().get("clientId")) && Objects.nonNull(session = sessions.get(rs.headers().get("clientId")))) {
                session.closeConnect();
            }
        });
    }


    /**
     * @param gwId  客户端连接id
     * @param state 状态
     * @param flag  狀態
     * @Description 發送設備狀態
     * @author zhang bo
     * @date 18-9-10
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    private void sendDeviceState(String gwId, JsonArray jsonArray, String state, boolean flag) {
        if (Objects.nonNull(jsonArray) && jsonArray.size() > 0) {
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObject = jsonArray.getJsonObject(i);
                PublishMessage publishMessage = new PublishMessage();
                publishMessage.setQos(AbstractMessage.QOSType.MOST_ONE);
                publishMessage.setRetainFlag(false);
                publishMessage.setDupFlag(false);
                publishMessage.setTopicName(REPLY_MESSAGE.replace("clientId", jsonObject.getString("uid")));
                try {
                    JsonObject sendJsonObject = new JsonObject().put("func", GATEWAY_ON_OFF_STATE).put("data", new JsonObject().put("state", state));
                    if (flag) {
                        sendJsonObject.put("devuuid", jsonObject.getString("deviceSN"));
                        MQTTSession mqttSession = null;
                        if (Objects.nonNull(mqttSession = sessions.get(GATEWAY_PREFIX + jsonObject.getString("deviceSN")))) {
                            sendJsonObject.getJsonObject("data").put("state", "online");
                            publishMessage.setPayload(sendJsonObject.toString());
                            MQTTSession mqttSession1;
                            if (Objects.nonNull(mqttSession1 = sessions.get(USER_PREFIX + jsonObject.getString("uid"))))
                                mqttSession1.sendMessageToClient(publishMessage);
                        } else {
                            sendJsonObject.getJsonObject("data").put("state", "offline");
                            publishMessage.setPayload(sendJsonObject.toString());
                            MQTTSession mqttSession1;
                            if (Objects.nonNull(mqttSession1 = sessions.get(USER_PREFIX + jsonObject.getString("uid"))))
                                mqttSession1.sendMessageToClient(publishMessage);
                        }
                    } else {
                        sendJsonObject.put("devuuid", gwId);
                        publishMessage.setPayload(sendJsonObject.toString());
                        MQTTSession mqttSession = null;
                        if (Objects.nonNull(mqttSession = sessions.get(USER_PREFIX + jsonObject.getString("uid"))))
                            mqttSession.sendMessageToClient(publishMessage);
                    }
                } catch (UnsupportedEncodingException e1) {
                    logger.error(e1.getCause().getMessage(), e1);
                }
            }
        }
    }


    /**
     * @Description 發送消息處理
     * @author zhang bo
     * @date 18-4-16
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    private void sendMessageProcess(Message<JsonObject> rs) {
        logger.debug("send message body topic -> {} , msg -> {} ", rs.headers().get("topicName"), rs.body().toString());
        if (!Objects.nonNull(rs.headers().get("uid"))) {
            logger.warn("message body header uid is null , params -> {}", rs.body());
            return;
        }
        PublishMessage publish = new PublishMessage();
        publish.setTopicName(rs.headers().get("topicName"));
        try {
            if (Objects.nonNull(rs.headers().get("msgId")))
                publish.setMessageID(Integer.parseInt(rs.headers().get("msgId")));
            JsonObject payload = rs.body();
            payload.remove("topicName");
            payload.remove("clientId");
            publish.setPayload(payload.toString());
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
        }
        switch (Integer.parseInt(rs.headers().get("qos"))) {
            case 0:
                publish.setQos(MOST_ONE);
                publish.setMessageID(0);
                if (rs.headers().get("uid").indexOf(":") >= 0) {
                    MQTTSession mqttSession;
                    if (Objects.nonNull(mqttSession = sessions.get(rs.headers().get("uid"))))
                        mqttSession.handlePublishMessage(publish, null);
                } else {
                    MQTTSession mqttSession;
                    if (Objects.nonNull(mqttSession = sessions.get(rs.headers().get("uid").length() == 13 ? "gw:" + rs.headers().get("uid")
                            : "app:" + rs.headers().get("uid"))))
                        mqttSession.handlePublishMessage(publish, null);
                }
                break;
            case 1:
                publish.setQos(LEAST_ONE);
                sendMSGToClient(rs.headers(), swapMsg(publish, rs.headers()));
                break;
            case 2:
                publish.setQos(EXACTLY_ONCE);
                sendMSGToClient(rs.headers(), swapMsg(publish, rs.headers()));
                break;
            default:
                logger.error("qos not in (0,1,2) , value -> {}", rs.headers().get("qos"));

        }
    }


    /**
     * @Description 发送消息到client
     * @author zhang bo
     * @date 18-9-28
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void sendMSGToClient(MultiMap multiMap, PublishMessage publish) {
        if (multiMap.get("uid").indexOf(":") >= 0) {
            MQTTSession mqttSession;
            if (Objects.nonNull(mqttSession = sessions.get(multiMap.get("uid")))) {
                mqttSession.handlePublishMessage(publish, permitted -> {
                });
            } else
                try {
                    writeLog(multiMap.get("uid"), publish);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
        } else {
            String clientId = multiMap.get("uid").length() == 13 ? "gw:" + multiMap.get("uid")
                    : "app:" + multiMap.get("uid");
            MQTTSession mqttSession;
            if (Objects.nonNull(mqttSession = sessions.get(clientId))) {
                mqttSession.handlePublishMessage(publish, permitted -> {
                });
            } else {
                try {
                    writeLog(clientId, publish);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * @Description 包装publish包消息
     * @author zhang bo
     * @date 18-9-28
     * @version 1.0
     */
    public PublishMessage swapMsg(PublishMessage publish, MultiMap multiMap) {
        if (Objects.nonNull(multiMap.get("messageId"))) {
            publish.setMessageID(Integer.parseInt(multiMap.get("messageId")));
        } else if (Objects.nonNull(multiMap.get("msgId"))) {
            publish.setMessageID(Integer.parseInt(multiMap.get("msgId")));
        } else {
            publish.setMessageID(1);
        }
        return publish;
    }


    /**
     * @Description 寫日志
     * @author zhang bo
     * @date 18-8-31
     * @version 1.0
     */

    @SuppressWarnings("Duplicates")
    private void writeLog(String clientId, PublishMessage publishMessage) throws Exception {
        DeliveryOptions opt = new DeliveryOptions().addHeader("tenant", clientId);

        publishMessage.setRetainFlag(false);
        Buffer msg = new MQTTEncoder().enc(publishMessage);
        if (publishMessage.getMessageID() != 0) {
            //持久化数据
            String[] arr = opt.getHeaders().get("tenant").split(":");
            JsonObject request = new JsonObject().put("msg", new JsonObject()
                    .put("message", publishMessage.getPayloadAsString())
                    .put("qos", QOSConvertUtils.toStr(publishMessage.getQos()))
                    .put("msgId", publishMessage.getMessageID())
                    .put("dst", arr[0]))
                    .put("msgId", publishMessage.getMessageID())
                    .put("topic", arr[1]).put("timeId", 0L);

            vertx.eventBus().send(LogAddr.class.getName() + WRITE_LOG, request, SendOptions.getInstance(), (AsyncResult<Message<Boolean>> rs) -> {
                if (rs.failed()) {
                    logger.error(rs.cause().getMessage(), rs.cause());
                }
            });
        }
    }

}
