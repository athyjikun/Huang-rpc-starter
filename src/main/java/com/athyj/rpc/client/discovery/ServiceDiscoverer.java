package com.athyj.rpc.client.discovery;


import com.athyj.rpc.common.service.Service;

import java.util.List;

/**
 * 服务发现抽象类，定义服务发现规范
 */
public interface ServiceDiscoverer {
    List<Service> getServices(String name);
}
