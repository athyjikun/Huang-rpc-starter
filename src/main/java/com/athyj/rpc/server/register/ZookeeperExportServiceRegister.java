package com.athyj.rpc.server.register;

import com.alibaba.fastjson.JSON;
import com.athyj.rpc.common.serializer.ZookeeperSerializer;
import com.athyj.rpc.common.service.Service;
import org.I0Itec.zkclient.ZkClient;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLEncoder;

import static com.athyj.rpc.common.constants.LeisureConstant.*;


/**
 * Zookeeper服务注册器，提供服务注册、服务暴露的能力
 */
public class ZookeeperExportServiceRegister extends DefaultServiceRegister implements ServiceRegister {

    /**
     * Zk客户端
     */
    private ZkClient client;

    public ZookeeperExportServiceRegister(String zkAddress, Integer port, String protocol) {
        client = new ZkClient(zkAddress);
        client.setZkSerializer(new ZookeeperSerializer());
        this.port = port;
        this.protocol = protocol;
    }

    /**
     * 服务注册
     *
     * @param so 服务持有者
     * @throws Exception 注册异常
     */
    @Override
    public void register(ServiceObject so) throws Exception {
        super.register(so);
        Service service = new Service();

        String host = InetAddress.getLocalHost().getHostAddress();
        String address = host + ":" + port;
        service.setAddress(address);
        service.setName(so.getClazz().getName());
        service.setProtocol(protocol);
        this.exportService(service);

    }

    /**
     * 服务暴露
     *
     * @param serviceResource 需要暴露的服务信息
     */
    private void exportService(Service serviceResource) {
        String serviceName = serviceResource.getName();
        String uri = JSON.toJSONString(serviceResource);
        try {
            uri = URLEncoder.encode(uri, UTF_8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String servicePath = ZK_SERVICE_PATH + PATH_DELIMITER + serviceName + "/service";
        if (!client.exists(servicePath)) {
            // 创建持久化节点
            client.createPersistent(servicePath, true);
        }
        String uriPath = servicePath + PATH_DELIMITER + uri;
        if (client.exists(uriPath)) {
            client.delete(uriPath);
        }
        // 创建临时节点
        client.createEphemeral(uriPath);
    }
}
