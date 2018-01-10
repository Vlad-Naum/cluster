package com.infomaximum.cluster.core.io;

import com.infomaximum.cluster.core.remote.controller.clusterfile.RControllerClusterFile;
import com.infomaximum.cluster.struct.Component;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Формат
 * cfile:com.infomaximum.cluster.component.manager:00000000-0000-0000-0000-00000000000/UUID
 */
public class ClusterFile {

    private static String SCHEME_CLUSTER_FILE = "cfile";
    private static String SCHEME_FILE = "file";

    private final Component component;
    private final URI uri;

    public ClusterFile(Component component, URI uri) {
        this.component = component;

        this.uri = uri;
        if (uri.getScheme() == null) throw new RuntimeException("Scheme is null, uri: " + uri.toString());
        if (!uri.getScheme().equals(SCHEME_FILE) && !uri.getScheme().equals(SCHEME_CLUSTER_FILE)) {
            throw new RuntimeException("Scheme is not support, uri: " + uri.toString() + ", scheme: " + uri.getScheme());
        }
    }

    public void copyTo(Path file) throws IOException {
        if (isLocalFile()) {
            Files.copy(Paths.get(uri), file);
        } else {
            //TODO Ulitin V. Необходимо переписать на поточную обработку, сейчас есть большие накладные расходы на оперативку
            RControllerClusterFile controllerClusterFile = component.getRemotes().getFromSSKey(getPathComponentKey(), RControllerClusterFile.class);
            byte[] content = controllerClusterFile.getContent(getPathFileUUID());
            Files.write(file, content);
        }
    }

    public void delete() throws IOException {
        if (isLocalFile()) {
            Files.delete(Paths.get(uri));
        } else {
            RControllerClusterFile controllerClusterFile = component.getRemotes().getFromSSKey(getPathComponentKey(), RControllerClusterFile.class);
            controllerClusterFile.delete(getPathFileUUID());
        }
    }

    public void deleteIfExists() throws IOException {
        if (isLocalFile()) {
            Files.deleteIfExists(Paths.get(uri));
        } else {
            RControllerClusterFile controllerClusterFile = component.getRemotes().getFromSSKey(getPathComponentKey(), RControllerClusterFile.class);
            controllerClusterFile.deleteIfExists(getPathFileUUID());
        }
    }

    public void moveTo(Path target) throws IOException {
        if (isLocalFile()) {
            Files.move(Paths.get(uri), target);
        } else {
            copyTo(target);
            delete();
        }
    }

    public long getSize() throws IOException {
        if (isLocalFile()) {
            return Files.size(Paths.get(uri));
        } else {
            RControllerClusterFile controllerClusterFile = component.getRemotes().getFromSSKey(getPathComponentKey(), RControllerClusterFile.class);
            return controllerClusterFile.getSize(getPathFileUUID());
        }
    }

    public boolean isLocalFile() {
        return SCHEME_FILE.equals(uri.getScheme());
    }

    private String getPathComponentKey() {
        if (!SCHEME_CLUSTER_FILE.equals(uri.getScheme())) throw new RuntimeException("Not support scheme");
        return uri.getSchemeSpecificPart().split("/")[0];
    }

    private String getPathFileUUID() {
        if (!SCHEME_CLUSTER_FILE.equals(uri.getScheme())) throw new RuntimeException("Not support scheme");
        return uri.getSchemeSpecificPart().split("/")[1];
    }
}