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
import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;

public class OffsetTimeHandle implements HessianHandle, Serializable {
    private static final long serialVersionUID = -3269846941421652860L;

    private LocalTime localTime;
    private ZoneOffset zoneOffset;

    public OffsetTimeHandle() {
    }

    public OffsetTimeHandle(OffsetTime o) {
        this.zoneOffset = o.getOffset();
        this.localTime = o.toLocalTime();
    }

    private Object readResolve() {
        return OffsetTime.of(localTime, zoneOffset);
    }
}
