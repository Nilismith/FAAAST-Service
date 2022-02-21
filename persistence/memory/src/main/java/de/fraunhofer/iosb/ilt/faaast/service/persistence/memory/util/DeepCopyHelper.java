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
package de.fraunhofer.iosb.ilt.faaast.service.persistence.memory.util;

import io.adminshell.aas.v3.dataformat.DeserializationException;
import io.adminshell.aas.v3.dataformat.SerializationException;
import io.adminshell.aas.v3.dataformat.json.JsonDeserializer;
import io.adminshell.aas.v3.dataformat.json.JsonSerializer;
import io.adminshell.aas.v3.model.Referable;
import java.util.ArrayList;
import java.util.List;


public class DeepCopyHelper {

    private DeepCopyHelper() {}


    public static <T extends Referable> T deepCopy(Referable referable, Class<T> outputClass) {
        try {
            Referable deepCopy = new JsonDeserializer().readReferable(new JsonSerializer().write(referable), outputClass);
            if (deepCopy.getClass().isAssignableFrom(outputClass)) {
                return (T) deepCopy;
            }
        }
        catch (SerializationException | DeserializationException e) {
            return null;
        }
        return null;
    }


    public static <T extends Referable> List<T> deepCopy(List<T> referableList,
                                                         Class<T> outputClass) {
        List<T> deepCopyList = new ArrayList<>();
        for (Referable referable: referableList) {
            deepCopyList.add(deepCopy(referable, outputClass));
        }
        return deepCopyList;
    }
}
