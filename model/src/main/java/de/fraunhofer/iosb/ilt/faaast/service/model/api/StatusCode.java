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
package de.fraunhofer.iosb.ilt.faaast.service.model.api;

/**
 * Model class for defined status codes for {@link Request}.
 */
public enum StatusCode {
    SUCCESS(Type.SUCCESS),
    SUCCESS_CREATED(Type.SUCCESS),
    SUCCESS_NO_CONTENT(Type.SUCCESS),
    CLIENT_FORBIDDEN(Type.ERROR),
    CLIENT_ERROR_BAD_REQUEST(Type.ERROR),
    CLIENT_METHOD_NOT_ALLOWED(Type.ERROR),
    CLIENT_ERROR_RESOURCE_NOT_FOUND(Type.ERROR),
    SERVER_INTERNAL_ERROR(Type.EXCEPTION),
    SERVER_ERROR_BAD_GATEWAY(Type.EXCEPTION);

    private final Type type;

    private StatusCode(Type type) {
        this.type = type;
    }


    public boolean isSuccess() {
        return type == Type.SUCCESS;
    }


    public boolean isError() {
        return type == Type.ERROR;
    }


    public boolean isException() {
        return type == Type.ERROR;
    }

    private static enum Type {
        SUCCESS,
        ERROR,
        EXCEPTION
    }
}
