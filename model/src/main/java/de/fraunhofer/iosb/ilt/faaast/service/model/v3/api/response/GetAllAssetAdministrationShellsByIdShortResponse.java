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
package de.fraunhofer.iosb.ilt.faaast.service.model.v3.api.response;

import de.fraunhofer.iosb.ilt.faaast.service.model.v3.api.BaseResponseWithPayload;
import io.adminshell.aas.v3.model.AssetAdministrationShell;
import java.util.List;


/**
 * Chapter 6.2.5
 */
public class GetAllAssetAdministrationShellsByIdShortResponse extends BaseResponseWithPayload<List<AssetAdministrationShell>> {
    public static GetAllAssetAdministrationShellsByIdShortResponse.Builder builder() {
        return new GetAllAssetAdministrationShellsByIdShortResponse.Builder();
    }

    public static class Builder extends AbstractBuilder<GetAllAssetAdministrationShellsByIdShortResponse, GetAllAssetAdministrationShellsByIdShortResponse.Builder> {

        @Override
        protected GetAllAssetAdministrationShellsByIdShortResponse.Builder getSelf() {
            return this;
        }


        public GetAllAssetAdministrationShellsByIdShortResponse.Builder payload(List<AssetAdministrationShell> value) {
            getBuildingInstance().setPayload(value);
            return getSelf();
        }


        @Override
        protected GetAllAssetAdministrationShellsByIdShortResponse newBuildingInstance() {
            return new GetAllAssetAdministrationShellsByIdShortResponse();
        }
    }
}
