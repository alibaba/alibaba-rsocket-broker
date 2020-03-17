package com.alibaba.rsocket.encoding.hessian.io;

import com.caucho.hessian.io.HessianSerializerInput;
import com.caucho.hessian.io.HessianSerializerOutput;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.*;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Hessian test
 *
 * @author leijuan
 */
public class HessianTest {

    @Test
    public void testSerializers() throws Exception {
        HessianObject hessianObject = new HessianObject();
        hessianObject.setLocalDate(LocalDate.now());
        hessianObject.setLocalDateTime(LocalDateTime.now());
        hessianObject.setLocalTime(LocalTime.now());
        hessianObject.setDuration(Duration.ofSeconds(111));
        hessianObject.setInstant(Instant.now());
        hessianObject.setPeriod(Period.of(1, 3, 16));
        hessianObject.setMonthDay(MonthDay.of(1, 2));
        hessianObject.setYearMonth(YearMonth.of(3, 2));
        hessianObject.setOptional(Optional.of("hello world"));
        hessianObject.setLocale(Locale.CHINESE);
        hessianObject.setUuid(UUID.randomUUID());
        hessianObject.setOffsetTime(OffsetTime.of(1, 2, 13, 11, ZoneOffset.UTC));
        hessianObject.setOffsetDateTime(OffsetDateTime.now());
        hessianObject.setZonedDateTime(ZonedDateTime.now());
        hessianObject.setLongArray(new Long[]{1L, 3L});
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        HessianSerializerOutput output = new HessianSerializerOutput(bos);
        output.writeObject(hessianObject);
        output.flush();
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        HessianSerializerInput input = new HessianSerializerInput(bis);
        HessianObject hessianObject2 = (HessianObject) input.readObject();
        Assertions.assertThat(hessianObject).isEqualToComparingFieldByField(hessianObject2);
        System.out.println(hessianObject2.getEmojiBear());
    }
}
