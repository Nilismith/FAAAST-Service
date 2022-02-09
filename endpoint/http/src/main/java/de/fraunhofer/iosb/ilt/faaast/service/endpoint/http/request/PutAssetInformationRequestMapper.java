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
package de.fraunhofer.iosb.ilt.faaast.service.endpoint.http.request;

import de.fraunhofer.iosb.ilt.faaast.service.endpoint.http.http.HttpMethod;
import de.fraunhofer.iosb.ilt.faaast.service.endpoint.http.http.HttpRequest;
import de.fraunhofer.iosb.ilt.faaast.service.model.v3.api.Request;
import de.fraunhofer.iosb.ilt.faaast.service.model.v3.api.request.PutAssetInformationRequest;
import de.fraunhofer.iosb.ilt.faaast.service.util.EncodingUtils;
import de.fraunhofer.iosb.ilt.faaast.service.util.IdUtils;
import io.adminshell.aas.v3.model.AssetInformation;


/**
 * class to map HTTP-PUT-Request path:
 * shells/{aasIdentifier}/aas/asset-information
 */
public class PutAssetInformationRequestMapper extends RequestMapper {

    private static final HttpMethod HTTP_METHOD = HttpMethod.PUT;
    private static final String PATTERN = "^shells/(.*)/aas/asset-information$";

    @Override
    public Request parse(HttpRequest httpRequest) {
        if (httpRequest.getPathElements() == null || httpRequest.getPathElements().size() != 4) {
            throw new IllegalArgumentException(String.format("invalid URL format (request: %s, url pattern: %s)",
                    PutAssetInformationRequest.class.getSimpleName(),
                    PATTERN));
        }
        PutAssetInformationRequest request = new PutAssetInformationRequest();
        request.setId(IdUtils.parseIdentifier(EncodingUtils.base64Decode(httpRequest.getPathElements().get(1))));
        request.setAssetInformation(parseBody(httpRequest, AssetInformation.class));
        return request;
    }


    @Override
    public boolean matches(HttpRequest httpRequest) {
        return httpRequest.getMethod().equals(HTTP_METHOD)
                && httpRequest.getPath().matches(PATTERN);
    }
}
