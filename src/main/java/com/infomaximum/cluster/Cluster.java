package com.infomaximum.cluster;

import com.infomaximum.cluster.builder.transport.TransportBuilder;
import com.infomaximum.cluster.component.manager.ManagerComponent;
import com.infomaximum.cluster.core.service.transport.TransportManager;
import com.infomaximum.cluster.exception.ClusterException;
import com.infomaximum.cluster.exception.CompatibilityException;
import com.infomaximum.cluster.exception.CyclicDependenceException;
import com.infomaximum.cluster.struct.Component;
import com.infomaximum.cluster.utils.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Класс потоконебезопасен
 */
public class Cluster implements AutoCloseable {

    private final static Logger log = LoggerFactory.getLogger(Cluster.class);

    private TransportManager transportManager;

    private final Map<Class<? extends Component>, List<Component>> components;
    private final List<Component> dependencyOrderedComponents;

    private Cluster(TransportManager transportManager) {
        this.transportManager = transportManager;
        this.components = new HashMap<>();
        this.dependencyOrderedComponents = new ArrayList<>();

        log.info("init Cluster...");
    }

    public TransportManager getTransportManager() {
        return transportManager;
    }

    private void appendComponent(Component component) throws ClusterException {
        component.init(transportManager);

        List<Component> componentInstances = components.get(component.getClass());
        if (componentInstances == null) {
            componentInstances = new ArrayList<>();
            components.put(component.getClass(), componentInstances);
        }
        componentInstances.add(component);
        dependencyOrderedComponents.add(component);
    }

    public <T extends Component> T getAnyComponent(Class<T> classComponent){
        List<Component> components = this.components.get(classComponent);
        if (components == null) {
            return null;
        }
        return (T) components.get(RandomUtil.random.nextInt(components.size()));
    }

    public <T extends Component> List<T> getDependencyOrderedComponentsOf(Class<T> baseClass) {
        return dependencyOrderedComponents.stream()
                .filter(component -> baseClass.isAssignableFrom(component.getClass()))
                .map(component -> (T)component)
                .collect(Collectors.toList());
    }

    @Override
    public void close() {
        for (int i = dependencyOrderedComponents.size() - 1; i > -1; --i) {
            closeComponent(dependencyOrderedComponents.remove(i));
        }

        transportManager.destroy();
    }

    private void closeComponent(Component component) {
        List<Component> list = components.get(component.getClass());
        if (list != null) {
            list.remove(component);
            if (list.isEmpty()) {
                components.remove(component.getClass());
            }
        }

        component.destroy();
    }

    public static class Builder {

        private TransportBuilder transportBuilder;
        private List<ComponentBuilder> componentBuilders = new ArrayList<>();
        private Version environmentVersion;

        public Builder() {}

        public Builder withEnvironmentVersion(Version environmentVersion) {
            this.environmentVersion = environmentVersion;
            return this;
        }

        public Builder withTransport(TransportBuilder transportBuilder) {
            this.transportBuilder = transportBuilder;
            return this;
        }

        public Builder withComponent(ComponentBuilder componentBuilder) {
            if (containsComponent(componentBuilder)) {
                throw new RuntimeException(componentBuilder.getComponentClass() + " already exists.");
            }
            componentBuilders.add(componentBuilder);
            return this;
        }

        public Builder withComponentIfNotExist(ComponentBuilder componentBuilder) {
            if (!containsComponent(componentBuilder)) {
                componentBuilders.add(componentBuilder);
            }
            return this;
        }

        public Cluster build() throws ClusterException {
            Cluster cluster = null;

            try {
                List<Component> components = new ArrayList<>(componentBuilders.size() + 1);

                //TODO необходима правильная инициализация менеджера, в настоящий момент считаем, что приложение у нас одно поэтому инициализируем его прямо тут
                components.add(new ManagerComponent(null));
                for (ComponentBuilder builder : componentBuilders) {
                    components.add(builder.build());
                }
                appendNotExistenceDependencies(components);

                cluster = new Cluster(transportBuilder.build());

                while (!components.isEmpty()) {
                    Component nextComponent = null;
                    int componentIndex = 0;
                    for (; componentIndex < components.size(); ++componentIndex) {
                        //Проверяем все ли зависимости загружены
                        Cluster finalCluster = cluster;
                        Component component = components.get(componentIndex);
                        boolean isSuccessDependencies = Arrays.stream(component.getInfo().getDependencies())
                                .noneMatch(aClass -> finalCluster.getAnyComponent(aClass) == null);
                        if (isSuccessDependencies) {
                            nextComponent = component;
                            break;
                        }
                    }

                    if (nextComponent == null) {
                        throw new CyclicDependenceException(components.stream().map(cb -> cb.getClass().getName()).collect(Collectors.toList()));
                    }

                    if (!nextComponent.getInfo().isCompatibleWith(environmentVersion)) {
                        throw new CompatibilityException(nextComponent, environmentVersion);
                    }

                    cluster.appendComponent(nextComponent);

                    components.remove(componentIndex);
                }
            } catch (ClusterException ex) {
                if (cluster != null) {
                    cluster.close();
                }
                throw ex;
            }

            return cluster;
        }

        private static void appendNotExistenceDependencies(List<Component> source) throws ClusterException {
            Set<Class> componentClasses = source.stream().map(component -> component.getClass()).collect(Collectors.toSet());
            for (int i = 0; i < source.size(); ++i) {
                for (Class dependence : source.get(i).getInfo().getDependencies()) {
                    if (!componentClasses.contains(dependence)) {
                        source.add(new ComponentBuilder(dependence).build());
                        componentClasses.add(dependence);
                    }
                }
            }
        }

        private boolean containsComponent(ComponentBuilder builder) {
            return componentBuilders.stream().anyMatch(cb -> cb.getComponentClass() == builder.getComponentClass());
        }
    }
}
