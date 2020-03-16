/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.rsocket.encoding.hessian.io.java8;

import com.caucho.hessian.io.HessianHandle;

import java.io.Serializable;
import java.util.Optional;


public class OptionalHandle implements HessianHandle, Serializable {
    private boolean present;
    private Object value;

    public OptionalHandle() {
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public OptionalHandle(Optional<?> o) {
        if (o.isPresent()) {
            present = true;
            this.value = o.get();
        } else {
            present = false;
        }
    }

    private Object readResolve() {
        if (present) {
            return Optional.ofNullable(value);
        } else {
            return Optional.empty();
        }
    }
}
