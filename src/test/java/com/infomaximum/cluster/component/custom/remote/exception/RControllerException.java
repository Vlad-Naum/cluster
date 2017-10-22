package com.infomaximum.cluster.component.custom.remote.exception;

import com.infomaximum.cluster.core.remote.struct.RController;

import java.nio.file.NotLinkException;
import java.util.concurrent.CompletableFuture;

/**
 * Created by kris on 28.10.16.
 */
public interface RControllerException extends RController {

	public String getException(String value) throws NotLinkException;

}
