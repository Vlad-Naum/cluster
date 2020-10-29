package com.infomaximum.cluster.core.remote;

import com.infomaximum.cluster.Cluster;
import com.infomaximum.cluster.core.component.RuntimeComponentInfo;
import com.infomaximum.cluster.core.remote.struct.RController;
import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.struct.Component;
import com.infomaximum.cluster.utils.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by kris on 28.10.16.
 */
public class Remotes {

    private final static Logger log = LoggerFactory.getLogger(Remotes.class);

    public final Cluster cluster;
    public final Component component;

    private final RemotePackerObjects remotePackerObjects;

    public Remotes(Cluster cluster, Component component) {
        this.cluster = cluster;
        this.component = component;
        this.remotePackerObjects = new RemotePackerObjects(this);
    }

    public RemotePackerObjects getRemotePackerObjects() {
        return remotePackerObjects;
    }

    public <T extends RController> T getFromCKey(int componentUniqueId, Class<T> remoteControllerClazz) {
        //Кешировать proxy remoteController нельзя т.к. Proxy.newProxyInstance может вернуться переиспользуемый объект в котором _properties уже заполнен и мы иего перезатрем
        RController remoteController = (RController) Proxy.newProxyInstance(
                remoteControllerClazz.getClassLoader(), new Class[]{remoteControllerClazz},
                new RemoteControllerInvocationHandler(component, componentUniqueId, remoteControllerClazz)
        );

        return (T) remoteController;
    }

    public <T extends RController> T get(String uuid, Class<T> remoteControllerClazz) {
        List<Integer> pretendents = new ArrayList<>();
        for (RuntimeComponentInfo componentInfo : component.getEnvironmentComponents().getActiveComponents()) {
            int componentUniqueId = componentInfo.uniqueId;
            String componentUuid = componentInfo.info.getUuid();

            if (componentUuid.equals(uuid)) pretendents.add(componentUniqueId);
        }
        if (pretendents.isEmpty()) throw new RuntimeException("Not found remote component: " + uuid);
        return getFromCKey(pretendents.get(RandomUtil.random.nextInt(pretendents.size())), remoteControllerClazz);
    }

    public <T extends RController> T get(Class<? extends Component> classComponent, Class<T> remoteControllerClazz) throws ClusterException {
        String uuid = cluster.getAnyComponent(classComponent).getInfo().getUuid();
        return get(uuid, remoteControllerClazz);
    }

    public <T extends RController> T get(Class<T> clazz) {
        throw new RuntimeException("Not implemented");
    }

    public <T extends RController> Collection<T> getControllers(Class<? extends Component> classComponent, Class<T> classController) {
        throw new RuntimeException("Not implemented");
    }

    public <T extends RController> Collection<T> getControllers(Class<T> remoteClassController) {
        List<T> controllers = new ArrayList<>();
        for (RuntimeComponentInfo componentInfo : component.getEnvironmentComponents().getActiveComponents()) {
            if (componentInfo.getClassNameRControllers().contains(remoteClassController.getName())) {
                //Нашли подсиситему в которой зарегистрирован этот контроллер
                controllers.add(getFromCKey(componentInfo.uniqueId, remoteClassController));
            }
        }
        return controllers;
    }
}
