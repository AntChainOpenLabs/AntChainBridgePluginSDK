package com.alipay.antchain.bridge.relayer.server.admin;

import javax.annotation.Resource;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alipay.antchain.bridge.relayer.commons.exception.AntChainBridgeRelayerException;
import com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminRequest;
import com.alipay.antchain.bridge.relayer.core.grpc.admin.AdminResponse;
import com.alipay.antchain.bridge.relayer.core.grpc.admin.AdministratorServiceGrpc;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Service;

@Service
public class AdminRpcServerImpl extends AdministratorServiceGrpc.AdministratorServiceImplBase {

    @Resource
    private NamespaceManager namespaceManager;

    @Override
    public void adminRequest(AdminRequest request, StreamObserver<AdminResponse> responseObserver) {

        String namespaceReq = request.getCommandNamespace();
        String commandReq = request.getCommand();
        String[] argsReq = request.getArgsList().toArray(new String[0]);

        AdminResponse.Builder response = AdminResponse.newBuilder();

        Namespace namespace = namespaceManager.get(namespaceReq);
        if (ObjectUtil.isNull(namespace)) {
            response.setSuccess(false);
            response.setErrorMsg(StrUtil.format("namespace {} not exist", namespaceReq));
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
            return;
        }

        try {
            Object result = namespace.executeCommand(commandReq, argsReq);

            if (ObjectUtil.isNotNull(result)) {
                String resultStr;
                if (result instanceof String) {
                    resultStr = (String) result;
                } else {
                    resultStr = JSON.toJSONString(result);
                }
                response.setResult(resultStr);
            }

            response.setSuccess(true);
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();

        } catch (AntChainBridgeRelayerException e) {
            response.setSuccess(false);
            response.setErrorMsg(e.getMessage());
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        }
    }
}
