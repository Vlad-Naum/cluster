package com.infomaximum.cluster.core.remote.packer;

import com.infomaximum.cluster.core.remote.RemotePackerObjects;
import com.infomaximum.cluster.struct.Component;

/**
 * Created by user on 06.09.2017.
 */
public interface RemotePacker<T> {

    boolean isSupport(Class classType);

    default String getClassName(Class classType){
        return classType.getName();
    }

    Object serialize(Component component, T value);

    T deserialize(Component component, Class<T> classType, Object value);
}
