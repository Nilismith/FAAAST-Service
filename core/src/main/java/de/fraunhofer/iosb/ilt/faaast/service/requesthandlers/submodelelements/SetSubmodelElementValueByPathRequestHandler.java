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
package de.fraunhofer.iosb.ilt.faaast.service.requesthandlers.submodelelements;

import de.fraunhofer.iosb.ilt.faaast.service.assetconnection.AssetConnectionManager;
import de.fraunhofer.iosb.ilt.faaast.service.exception.ResourceNotFoundException;
import de.fraunhofer.iosb.ilt.faaast.service.messagebus.MessageBus;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.Extend;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.OutputModifier;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.StatusCode;
import de.fraunhofer.iosb.ilt.faaast.service.model.api.response.SetSubmodelElementValueByPathResponse;
import de.fraunhofer.iosb.ilt.faaast.service.model.request.SetSubmodelElementValueByPathRequest;
import de.fraunhofer.iosb.ilt.faaast.service.model.valuedata.ElementValue;
import de.fraunhofer.iosb.ilt.faaast.service.persistence.Persistence;
import de.fraunhofer.iosb.ilt.faaast.service.requesthandlers.RequestHandler;
import de.fraunhofer.iosb.ilt.faaast.service.util.ElementPathUtils;
import de.fraunhofer.iosb.ilt.faaast.service.util.ElementValueMapper;
import io.adminshell.aas.v3.model.Reference;
import io.adminshell.aas.v3.model.Submodel;
import io.adminshell.aas.v3.model.SubmodelElement;


public class SetSubmodelElementValueByPathRequestHandler extends RequestHandler<SetSubmodelElementValueByPathRequest<?>, SetSubmodelElementValueByPathResponse> {

    public SetSubmodelElementValueByPathRequestHandler(Persistence persistence, MessageBus messageBus, AssetConnectionManager assetConnectionManager) {
        super(persistence, messageBus, assetConnectionManager);
    }


    @Override
    public SetSubmodelElementValueByPathResponse process(SetSubmodelElementValueByPathRequest request) {
        SetSubmodelElementValueByPathResponse response = new SetSubmodelElementValueByPathResponse();
        try {
            Reference reference = ElementPathUtils.toReference(request.getPath(), request.getId(), Submodel.class);
            SubmodelElement submodelElement = persistence.get(reference, new OutputModifier.Builder()
                    .extend(Extend.WithBLOBValue)
                    .build());
            ElementValue oldValue = ElementValueMapper.toValue(submodelElement);

            if (request.getValueParser() != null) {
                ElementValue newValue = request.getValueParser().parse(request.getRawValue(), oldValue.getClass());
                ElementValueMapper.setValue(submodelElement, newValue);

                writeValueToAssetConnection(reference, newValue);
                persistence.put(null, reference, submodelElement);

                response.setStatusCode(StatusCode.Success);
                publishValueChangeEventMessage(reference, oldValue, newValue);
            }
            else {
                throw new RuntimeException("Value parser of request must be non-null");
            }

        }
        catch (ResourceNotFoundException ex) {
            response.setStatusCode(StatusCode.ClientErrorResourceNotFound);
        }
        catch (Exception ex) {
            response.setStatusCode(StatusCode.ServerInternalError);
        }
        return response;
    }

}
