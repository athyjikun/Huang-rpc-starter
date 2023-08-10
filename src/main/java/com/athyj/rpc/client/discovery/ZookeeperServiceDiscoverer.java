package com.athyj.rpc.client.discovery;

import com.alibaba.fastjson.JSON;
import com.athyj.rpc.common.constants.LeisureConstant;
import com.athyj.rpc.common.serializer.ZookeeperSerializer;
import com.athyj.rpc.common.service.Service;
import org.I0Itec.zkclient.ZkClient;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Zookeeper服务发现者，定义以Zookeeper为注册中心的服务发现细则
 */
public class ZookeeperServiceDiscoverer implements ServiceDiscoverer {

    private ZkClient zkClient;

    public ZookeeperServiceDiscoverer(String zkAddress) {
        zkClient = new ZkClient(zkAddress);
        // 设置序列化
        zkClient.setZkSerializer(new ZookeeperSerializer());
    }

    /**
     * 使用Zookeeper客户端，通过服务名获取服务列表
     * 服务名格式：接口全路径
     *
     * @param name 服务名
     * @return 服务列表
     */
    @Override
    public List<Service> getServices(String name) {
        String servicePath = LeisureConstant.ZK_SERVICE_PATH +
                LeisureConstant.PATH_DELIMITER + name + "/service";
        List<String> children = zkClient.getChildren(servicePath);
        return Optional.ofNullable(children).orElse(new ArrayList<>()).stream().map(str -> {
            String deCh = null;
            try {
                // 解码
                deCh = URLDecoder.decode(str, LeisureConstant.UTF_8);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return JSON.parseObject(deCh, Service.class);
        }).collect(Collectors.toList());
    }
}
