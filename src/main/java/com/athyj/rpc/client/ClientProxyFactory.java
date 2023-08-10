package com.athyj.rpc.client;

import com.athyj.rpc.client.discovery.ServiceDiscoverer;
import com.athyj.rpc.client.net.NetClient;
import com.athyj.rpc.common.protocol.LeisureRequest;
import com.athyj.rpc.common.protocol.LeisureResponse;
import com.athyj.rpc.common.protocol.MessageProtocol;
import com.athyj.rpc.common.service.Service;
import com.athyj.rpc.exception.LeisureException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static java.lang.reflect.Proxy.newProxyInstance;

/**
 * 客户端代理工厂：用于创建远程服务代理类
 * 封装编组请求、请求发送、编组响应等操作。
 */
public class ClientProxyFactory {
    private ServiceDiscoverer serviceDiscoverer;

    private Map<String, MessageProtocol> supportMessageProtocols;

    private NetClient netClient;

    private Map<Class<?>, Object> objectCache = new HashMap<>();

    /**
     * 通过Java动态代理获取服务代理类
     *
     * @param clazz 被代理类Class
     * @param <T>   泛型
     * @return 服务代理类
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        /*
        * computeIfAbsent: hashmap中不存在这个key就放入
        * */
        return (T) this.objectCache.computeIfAbsent(clazz,
                cls -> newProxyInstance(cls.getClassLoader(),// 类加载器
                        new Class<?>[]{cls},                 // 被代理对象实现的所有的接口的字节码数组
                        new ClientInvocationHandler(cls)));  // 执行处理器 用于定义方法的增强规则（加强后的方法）
    }

    public ServiceDiscoverer getServiceDiscoverer() {
        return serviceDiscoverer;
    }

    public void setSid(ServiceDiscoverer serviceDiscoverer) {
        this.serviceDiscoverer = serviceDiscoverer;
    }

    public Map<String, MessageProtocol> getSupportMessageProtocols() {
        return supportMessageProtocols;
    }

    public void setSupportMessageProtocols(Map<String, MessageProtocol> supportMessageProtocols) {
        this.supportMessageProtocols = supportMessageProtocols;
    }

    public NetClient getNetClient() {
        return netClient;
    }

    public void setNetClient(NetClient netClient) {
        this.netClient = netClient;
    }

    /**
     * 客户端服务代理类invoke函数细节实现
     */
    private class ClientInvocationHandler implements InvocationHandler {
        private Class<?> clazz;

        private Random random = new Random();

        public ClientInvocationHandler(Class<?> clazz) {
            super();
            this.clazz = clazz;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Exception {

            if (method.getName().equals("toString")) {
                return proxy.getClass().toString();
            }

            if (method.getName().equals("hashCode")) {
                return 0;
            }

            // 1、获得服务信息
            String serviceName = this.clazz.getName();
            List<Service> services = serviceDiscoverer.getServices(serviceName);

            if (services == null || services.isEmpty()) {
                throw new LeisureException("No provider available!");
            }

            // 随机选择一个服务提供者（软负载均衡）
            Service service = services.get(random.nextInt(services.size()));

            // 2、构造request对象
            LeisureRequest req = new LeisureRequest();
            req.setServiceName(service.getName());
            req.setMethod(method.getName());
            req.setParameterTypes(method.getParameterTypes());
            req.setParameters(args);

            // 3、协议层编组
            // 获得该方法对应的协议
            MessageProtocol protocol = supportMessageProtocols.get(service.getProtocol());
            // 编组请求
            byte[] data = protocol.marshallingRequest(req);

            // 4、调用网络层发送请求
            byte[] repData = netClient.sendRequest(data, service);

            // 5解组响应消息
            LeisureResponse rsp = protocol.unmarshallingResponse(repData);

            // 6、结果处理
            if (rsp.getException() != null) {
                throw rsp.getException();
            }
            return rsp.getReturnValue();
        }
    }
}
