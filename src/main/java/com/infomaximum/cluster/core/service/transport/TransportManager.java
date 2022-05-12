package com.infomaximum.cluster.core.service.transport;

import com.infomaximum.cluster.core.remote.packer.RemotePacker;
import com.infomaximum.cluster.core.service.transport.executor.ExecutorTransport;
import com.infomaximum.cluster.core.service.transport.network.NetworkTransit;
import com.infomaximum.cluster.exception.ExceptionBuilder;
import com.infomaximum.cluster.struct.Component;
import com.infomaximum.cluster.utils.GlobalUniqueIdUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

/**
 * Created by kris on 12.09.16.
 */
public class TransportManager {

    public final NetworkTransit networkTransit;
    private final List<RemotePacker> remotePackers;

    private final Map<Integer, LocalTransport> localComponentUniqueIdTransports;

    private final ExceptionBuilder exceptionBuilder;

    public TransportManager(NetworkTransit.Builder builderNetworkTransit, List<RemotePacker> remotePackers, ExceptionBuilder exceptionBuilder) {
        this.networkTransit = builderNetworkTransit.build(this);
        this.remotePackers = Collections.unmodifiableList(remotePackers);

        this.localComponentUniqueIdTransports = new ConcurrentHashMap<Integer, LocalTransport>();

        this.exceptionBuilder = exceptionBuilder;
    }

    public List<RemotePacker> getRemotePackers() {
        return remotePackers;
    }

    public LocalTransport createTransport(Component component) {
        return new LocalTransport(this, component);
    }

    public ExceptionBuilder getExceptionBuilder() {
        return exceptionBuilder;
    }

    public synchronized void registerTransport(LocalTransport transport) {
        int uniqueId = transport.getComponent().getUniqueId();
        if (uniqueId < 0) {
            throw new RuntimeException("Internal error: Error in logic");
        }
        if (localComponentUniqueIdTransports.put(uniqueId, transport) != null) {
            throw new RuntimeException("Internal error: Error in logic");
        }
    }

    public synchronized void destroyTransport(LocalTransport transport) {
        localComponentUniqueIdTransports.remove(transport.getComponent().getUniqueId());
    }

    public Object request(int targetComponentUniqueId, String rControllerClassName, String methodName, Object[] args) throws Exception {
        byte targetNode = GlobalUniqueIdUtils.getNode(targetComponentUniqueId);
        if (targetNode == networkTransit.getNode()) {
            //локальный запрос
            return localRequest(targetComponentUniqueId, rControllerClassName, methodName, args);
        } else {
            //сетевой запрос
            return networkTransit.getRemoteControllerRequest().request(targetComponentUniqueId, rControllerClassName, methodName, args);
        }
    }

    public Object localRequest(int targetComponentUniqueId, String rControllerClassName, String methodName, Object[] args) throws Exception {
        LocalTransport targetTransport = localComponentUniqueIdTransports.get(targetComponentUniqueId);
        if (targetTransport == null) {
            throw new TimeoutException("Not found target component: " + targetComponentUniqueId);
        }

        ExecutorTransport targetExecutorTransport = targetTransport.getExecutor();
        return targetExecutorTransport.execute(rControllerClassName, methodName, args);
    }

    public void destroy() {
        networkTransit.close();
    }
}