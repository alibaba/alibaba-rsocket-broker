package com.alibaba.rsocket.gateway.grpc.impl;

import com.alibaba.dummy.DummyServiceGrpc;
import com.google.protobuf.Int32Value;
import io.grpc.stub.StreamObserver;
import org.lognet.springboot.grpc.GRpcService;

@GRpcService
public class DummyServiceImpl extends DummyServiceGrpc.DummyServiceImplBase {
    @Override
    public void echo(Int32Value request, StreamObserver<Int32Value> responseObserver) {
        final Int32Value response = Int32Value.newBuilder().setValue(request.getValue()).build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
