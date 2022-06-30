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
package de.fraunhofer.iosb.ilt.faaast.service.persistence;

import de.fraunhofer.iosb.ilt.faaast.service.config.Config;
import io.adminshell.aas.v3.model.AssetAdministrationShellEnvironment;
import io.adminshell.aas.v3.model.builder.ExtendableBuilder;


/**
 * Generic persistence configuration. When implementing a custom persistence
 * inherit from this class to create a custom configuration.
 *
 * @param <T> type of the persistence
 */
public class PersistenceConfig<T extends Persistence> extends Config<T> {

    public static boolean DEFAULT_DECOUPLE_ENVIRONMENT = true;
    private String modelPath;
    private AssetAdministrationShellEnvironment environment;
    private boolean decoupleEnvironment;

    public PersistenceConfig(String modelPath) {
        this.modelPath = modelPath;
        decoupleEnvironment = DEFAULT_DECOUPLE_ENVIRONMENT;
    }


    public PersistenceConfig() {
        decoupleEnvironment = DEFAULT_DECOUPLE_ENVIRONMENT;
    }


    public String getModelPath() {
        return modelPath;
    }


    /**
     * Could be overwritten by setting an AASEnvironment
     */
    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }


    public AssetAdministrationShellEnvironment getEnvironment() {
        return environment;
    }


    /**
     * Overwrites the AASEnvironment from model path
     * 
     * @param environment
     */
    public void setEnvironment(AssetAdministrationShellEnvironment environment) {
        this.environment = environment;
    }


    public boolean isDecoupleEnvironment() {
        return decoupleEnvironment;
    }


    /**
     * If true then a copied version of the environment is used
     *
     * @param decoupleEnvironment
     */
    public void setDecoupleEnvironment(boolean decoupleEnvironment) {
        this.decoupleEnvironment = decoupleEnvironment;
    }

    /**
     * Abstract builder class that should be used for builders of inheriting
     * classes.
     *
     * @param <T> type of the persistence of the config to build
     * @param <C> type of the config to build
     * @param <B> type of this builder, needed for inheritance builder pattern
     */
    public abstract static class AbstractBuilder<T extends Persistence, C extends PersistenceConfig<T>, B extends AbstractBuilder<T, C, B>> extends ExtendableBuilder<C, B> {

        public B modelPath(String modelPath) {
            getBuildingInstance().setModelPath(modelPath);
            return getSelf();
        }


        public B environment(AssetAdministrationShellEnvironment env) {
            getBuildingInstance().setEnvironment(env);
            return getSelf();
        }


        public B decoupleEnvironment(boolean decouple) {
            getBuildingInstance().setDecoupleEnvironment(decouple);
            return getSelf();
        }

    }

    /**
     * Builder for PersistenceConfig class.
     *
     * @param <T> type of the persistence of the config to build
     */
    public static class Builder<T extends Persistence> extends AbstractBuilder<T, PersistenceConfig<T>, Builder<T>> {

        @Override
        protected Builder<T> getSelf() {
            return this;
        }


        @Override
        protected PersistenceConfig<T> newBuildingInstance() {
            return new PersistenceConfig<>();
        }

    }
}
