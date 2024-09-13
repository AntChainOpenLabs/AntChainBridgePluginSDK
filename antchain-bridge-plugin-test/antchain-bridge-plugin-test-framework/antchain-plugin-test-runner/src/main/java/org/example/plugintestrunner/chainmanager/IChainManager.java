package org.example.plugintestrunner.chainmanager;

import lombok.Getter;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Getter
public abstract class IChainManager {

    public abstract boolean isConnected() throws ExecutionException, InterruptedException, IOException;

    public abstract void close();

}
