package com.infomaximum.cluster.core.component.active;

import com.infomaximum.cluster.core.component.RuntimeComponentInfo;
import com.infomaximum.cluster.struct.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by kris on 04.10.16.
 */
public class ActiveComponentsImpl implements ActiveComponents {

	private final Component component;

	private final Map<String, RuntimeComponentInfo> componentInfos;

	public ActiveComponentsImpl(Component component, Collection<RuntimeComponentInfo> activeSubSystems) {
		this.component = component;

		this.componentInfos = new ConcurrentHashMap<>();
		for (RuntimeComponentInfo subSystemInfo: activeSubSystems){
			this.componentInfos.put(subSystemInfo.key, subSystemInfo);
		}
	}

	@Override
	public Collection<RuntimeComponentInfo> registerActiveRole(RuntimeComponentInfo subSystemInfo) {
		this.componentInfos.put(subSystemInfo.key, subSystemInfo);
		return componentInfos.values();
	}

	@Override
	public Collection<RuntimeComponentInfo> unRegisterActiveRole(String key) {
		this.componentInfos.remove(key);
		return componentInfos.values();
	}

	@Override
	public Collection<RuntimeComponentInfo> getActiveComponents() {
		return componentInfos.values();
	}

	@Override
	public Collection<String> getActiveComponentKeys() {
		return componentInfos.keySet();
	}

	@Override
	public Collection<String> getActiveComponentUuids() {
		HashSet<String> subSystemUuids = new HashSet<>();
		for(RuntimeComponentInfo subSystemInfo: componentInfos.values()){
			subSystemUuids.add(subSystemInfo.info.getUuid());
		}
		return subSystemUuids;
	}

	@Override
	public boolean isActiveComponent(String uuid) {
		return getActiveComponentUuids().contains(uuid);
	}
}
