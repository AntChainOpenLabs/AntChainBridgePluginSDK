package org.example.plugintestrunner.chainmanager;

import lombok.Getter;
import lombok.Setter;
import one.block.eosiojava.error.serializationProvider.SerializationProviderError;
import one.block.eosiojava.error.signatureProvider.GetAvailableKeysError;
import one.block.eosiojava.implementations.ABIProviderImpl;
import one.block.eosiojava.interfaces.ISerializationProvider;
import one.block.eosiojava.interfaces.ISignatureProvider;
import one.block.eosiojava.models.rpcProvider.request.GetRequiredKeysRequest;
import one.block.eosiojava.models.rpcProvider.response.GetInfoResponse;
import one.block.eosiojava.session.TransactionSession;
import one.block.eosiojavaabieosserializationprovider.AbiEosSerializationProviderImpl;
import one.block.eosiojavarpcprovider.error.EosioJavaRpcProviderInitializerError;
import one.block.eosiojavarpcprovider.implementations.EosioJavaRpcProviderImpl;
import one.block.eosiosoftkeysignatureprovider.SoftKeySignatureProviderImpl;
import one.block.eosiosoftkeysignatureprovider.error.ImportKeyError;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

@Getter
@Setter
public class EosChainManager extends IChainManager {

    private final EosioJavaRpcProviderImpl rpcProvider;

    public EosChainManager(String url) throws EosioJavaRpcProviderInitializerError {
        rpcProvider = new EosioJavaRpcProviderImpl(url);
    }

    @Override
    public boolean isConnected() {
        try {
            GetInfoResponse info = rpcProvider.getInfo();
            System.out.println(info.getChainId());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void close() {

    }
}
