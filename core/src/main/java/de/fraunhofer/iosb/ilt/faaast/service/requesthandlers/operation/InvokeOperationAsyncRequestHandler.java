/*
 * Copyright (c) 2021 Fraunhofer IOSB, eine rechtlich nicht selbstaendige
 * Einrichtung der Fraunhofer-Gesellschaft zur Foerderung der angewandten
 * Forschung e.V.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.fraunhofer.iosb.ilt.faaast.service.requesthandlers.operation;

import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionException;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionManager;
import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetOperationProvider;
import de.fraunhofer.iosb.ilt.faaast.service.messagebus.MessageBus;
import de.fraunhofer.iosb.ilt.faaast.service.model.v3.api.ExecutionState;
import de.fraunhofer.iosb.ilt.faaast.service.model.v3.api.OperationHandle;
import de.fraunhofer.iosb.ilt.faaast.service.model.v3.api.OperationResult;
import de.fraunhofer.iosb.ilt.faaast.service.model.v3.api.StatusCode;
import de.fraunhofer.iosb.ilt.faaast.service.model.v3.api.request.InvokeOperationAsyncRequest;
import de.fraunhofer.iosb.ilt.faaast.service.model.v3.api.response.InvokeOperationAsyncResponse;
import de.fraunhofer.iosb.ilt.faaast.service.model.v3.valuedata.ElementValue;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.Persistence;
import de.fraunhofer.iosb.ilt.faaast.service.requesthandlers.RequestHandler;
import de.fraunhofer.iosb.ilt.faaast.service.requesthandlers.Util;
import de.fraunhofer.iosb.ilt.faaast.service.util.DataElementValueMapper;
import io.adminshell.aas.v3.model.OperationVariable;
import io.adminshell.aas.v3.model.Reference;
import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


public class InvokeOperationAsyncRequestHandler extends RequestHandler<InvokeOperationAsyncRequest, InvokeOperationAsyncResponse> {

    public InvokeOperationAsyncRequestHandler(Persistence persistence, MessageBus messageBus, AssetConnectionManager assetConnectionManager) {
        super(persistence, messageBus, assetConnectionManager);
    }


    @Override
    public InvokeOperationAsyncResponse process(InvokeOperationAsyncRequest request) {
        InvokeOperationAsyncResponse response = new InvokeOperationAsyncResponse();

        try {
            Reference reference = Util.toReference(request.getPath());

            OperationHandle operationHandle = executeOperationAsync(reference, request);
            response.setPayload(operationHandle);
            response.setStatusCode(StatusCode.Success);
            publishOperationInvokeEventMessage(reference,
                    request.getInputArguments().stream()
                            .map(x -> (ElementValue) DataElementValueMapper.toDataElement(x.getValue()))
                            .collect(Collectors.toList()),
                    request.getInoutputArguments().stream()
                            .map(x -> (ElementValue) DataElementValueMapper.toDataElement(x.getValue()))
                            .collect(Collectors.toList()));
        }
        catch (Exception ex) {
            response.setStatusCode(StatusCode.ServerInternalError);
        }
        return response;
    }


    public OperationHandle executeOperationAsync(Reference reference, InvokeOperationAsyncRequest request) {

        if (assetConnectionManager.hasOperationProvider(reference)) {
            OperationHandle operationHandle = this.persistence.putOperationContext(
                    null,
                    request.getRequestId(),
                    new OperationResult.Builder()
                            .requestId(request.getRequestId())
                            .inoutputArguments(request.getInoutputArguments())
                            .executionState(ExecutionState.Running)
                            .build());

            BiConsumer<OperationVariable[], OperationVariable[]> callback = (x, y) -> {
                OperationResult operationResult = persistence.getOperationResult(operationHandle.getHandleId());

                //TODO: What about Failed ...?
                operationResult.setExecutionState(ExecutionState.Completed);
                operationResult.setOutputArguments(Arrays.asList(x));
                operationResult.setInoutputArguments(Arrays.asList(y));

                persistence.putOperationContext(operationHandle.getHandleId(), operationHandle.getRequestId(), operationResult);
                publishOperationFinishEventMessage(reference,
                        Arrays.asList(x).stream()
                                .map(z -> (ElementValue) DataElementValueMapper.toDataElement(z.getValue()))
                                .collect(Collectors.toList()),
                        Arrays.asList(y).stream()
                                .map(z -> (ElementValue) DataElementValueMapper.toDataElement(z.getValue()))
                                .collect(Collectors.toList()));
            };

            AssetOperationProvider assetOperationProvider = assetConnectionManager.getOperationProvider(reference);
            try {
                assetOperationProvider.invokeAsync(
                        request.getInputArguments().toArray(new OperationVariable[0]),
                        request.getInoutputArguments().toArray(new OperationVariable[0]),
                        callback);
            }
            catch (AssetConnectionException ex) {
                OperationResult operationResult = persistence.getOperationResult(operationHandle.getHandleId());
                operationResult.setExecutionState(ExecutionState.Failed);
                operationResult.setInoutputArguments(request.getInoutputArguments());
                persistence.putOperationContext(operationHandle.getHandleId(), operationHandle.getRequestId(), operationResult);
                publishOperationFinishEventMessage(reference,
                        Arrays.asList(),
                        operationResult.getInoutputArguments().stream()
                                .map(x -> (ElementValue) DataElementValueMapper.toDataElement(x.getValue()))
                                .collect(Collectors.toList()));
            }
            return operationHandle;
        }
        else {
            throw new RuntimeException("No assetconnection available for running operation with request id" + request.getRequestId());
        }
    }
}
