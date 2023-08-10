package com.athyj.rpc.config;

import com.athyj.rpc.client.ClientProxyFactory;
import com.athyj.rpc.client.discovery.ZookeeperServiceDiscoverer;
import com.athyj.rpc.client.net.NettyNetClient;
import com.athyj.rpc.common.protocol.JavaSerializeMessageProtocol;
import com.athyj.rpc.common.protocol.MessageProtocol;
import com.athyj.rpc.properties.LeisureRpcProperty;
import com.athyj.rpc.server.NettyRpcServer;
import com.athyj.rpc.server.RequestHandler;
import com.athyj.rpc.server.RpcServer;
import com.athyj.rpc.server.register.DefaultRpcProcessor;
import com.athyj.rpc.server.register.ServiceRegister;
import com.athyj.rpc.server.register.ZookeeperExportServiceRegister;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring boot 自动配置类
 */
@Configuration
public class AutoConfiguration {

    @Bean
    public DefaultRpcProcessor defaultRpcProcessor() {
        return new DefaultRpcProcessor();
    }

    @Bean
    public ClientProxyFactory clientProxyFactory(@Autowired LeisureRpcProperty leisureRpcProperty) {
        ClientProxyFactory clientProxyFactory = new ClientProxyFactory();
        // 设置服务发现者
        clientProxyFactory.setSid(new ZookeeperServiceDiscoverer(leisureRpcProperty.getRegisterAddress()));

        // 设置支持的协议
        Map<String, MessageProtocol> supportMessageProtocols = new HashMap<>();
        supportMessageProtocols.put(leisureRpcProperty.getProtocol(), new JavaSerializeMessageProtocol());
        clientProxyFactory.setSupportMessageProtocols(supportMessageProtocols);

        // 设置网络层实现
        clientProxyFactory.setNetClient(new NettyNetClient());
        return clientProxyFactory;
    }

    @Bean
    public ServiceRegister serviceRegister(@Autowired LeisureRpcProperty leisureRpcProperty) {
        return new ZookeeperExportServiceRegister(
                leisureRpcProperty.getRegisterAddress(),
                leisureRpcProperty.getServerPort(),
                leisureRpcProperty.getProtocol());
    }

    @Bean
    public RequestHandler requestHandler(@Autowired ServiceRegister serviceRegister) {
        return new RequestHandler(new JavaSerializeMessageProtocol(), serviceRegister);
    }

    @Bean
    public RpcServer rpcServer(@Autowired RequestHandler requestHandler,
                               @Autowired LeisureRpcProperty leisureRpcProperty) {
        return new NettyRpcServer(leisureRpcProperty.getServerPort(),
                leisureRpcProperty.getProtocol(), requestHandler);
    }

    @Bean
    public LeisureRpcProperty leisureRpcProperty() {
        return new LeisureRpcProperty();
    }
}
