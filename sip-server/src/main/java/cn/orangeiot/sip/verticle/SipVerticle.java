package cn.orangeiot.sip.verticle;

import cn.orangeiot.sip.SipServerStart;
import cn.orangeiot.sip.SipVertxFactory;
import cn.orangeiot.sip.constant.SipOptions;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sip.SipFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author zhang bo
 * @version 1.0
 * @Description
 * @date 2018-01-30
 */
public class SipVerticle extends AbstractVerticle {

    private JsonObject jsonObject;//配置數據

    private static Logger logger = LogManager.getLogger(SipVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {

        loadConf();
        // 建立sip实例
        startFuture.complete();
    }


    /**
     * @Description 加載配置
     * @author zhang bo
     * @date 17-12-8
     * @version 1.0
     */
    @SuppressWarnings("Duplicates")
    public void loadConf() {
        InputStream zkIn = SipVerticle.class.getResourceAsStream("/zkConf.json");
        InputStream configIn = SipVerticle.class.getResourceAsStream("/config.json");//全局配置
        String zkConf = "";//jdbc连接配置
        String config = "";
        try {
            zkConf = IOUtils.toString(zkIn, "UTF-8");//获取配置
            config = IOUtils.toString(configIn, "UTF-8");

            if (!zkConf.equals("")) {
                JsonObject json = new JsonObject(zkConf);
                jsonObject = new JsonObject(config);

                if (Objects.nonNull(System.getProperty("CLUSTER")))
                    json.put("rootPath", System.getProperty("CLUSTER"));

                System.setProperty("vertx.zookeeper.hosts", json.getString("hosts.zookeeper"));
                ClusterManager mgr = new ZookeeperClusterManager(json);
                VertxOptions options = new VertxOptions().setClusterManager(mgr);
                if (Objects.nonNull(json.getValue("node.host")))
                    options.setClusterHost(json.getString("node.host"));

                //集群
                Vertx.clusteredVertx(options, this::register);
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            if (null != zkIn)
                try {
                    zkIn.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            if (null != configIn)
                try {
                    configIn.close();
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
        }
    }

    /**
     * @Description 注册
     * @author zhang bo
     * @date 18-3-8
     * @version 1.0
     */
    public void register(AsyncResult<Vertx> res) {
        if (res.succeeded()) {
            Vertx vertx = res.result();

            //连接处理
            SipVertxFactory.getInstance().createListeningPoint(SipOptions.UDP, vertx, jsonObject);
        }
    }
}