package com.neonshift.app;

import java.math.*;
import java.time.*;

public final class JournalEngine {
    public static final class Entry {
        public final long startedAt, endedAt, workedMs;
        Entry(long start, long end, long work) { startedAt=start; endedAt=end; workedMs=work; }
    }
    public static Entry plan(String date, String mode, String hours, String start, String end,
            boolean nextDay, String pauseMinutes, ZoneId zone, long now) {
        try {
            LocalDate day=LocalDate.parse(date);
            if(day.isAfter(Instant.ofEpochMilli(now).atZone(zone).toLocalDate()))
                throw new IllegalArgumentException("A data do trabalho não pode estar no futuro.");
            if("hours".equals(mode)) {
                long ms=InvoiceEngine.hours(hours).multiply(new BigDecimal("3600000")).setScale(0,RoundingMode.HALF_UP).longValueExact();
                long anchor=day.atStartOfDay(zone).toInstant().toEpochMilli();
                return new Entry(anchor,anchor+ms,ms);
            }
            if(!"times".equals(mode))throw new IllegalArgumentException("Escolha como informar as horas.");
            LocalDateTime a=day.atTime(LocalTime.parse(start)),b=day.plusDays(nextDay?1:0).atTime(LocalTime.parse(end));
            if(zone.getRules().getValidOffsets(a).isEmpty()||zone.getRules().getValidOffsets(b).isEmpty())
                throw new IllegalArgumentException("Esse horário não existe no fuso do celular. Confira o horário de verão.");
            long from=a.atZone(zone).toInstant().toEpochMilli(),to=b.atZone(zone).toInstant().toEpochMilli();
            if(to<from)throw new IllegalArgumentException("O término deve vir após o início. Para atravessar a meia-noite, marque Dia seguinte.");
            if(to>now)throw new IllegalArgumentException("O horário de término não pode estar no futuro.");
            BigDecimal pause=new BigDecimal(pauseMinutes.trim().replace(',','.'));
            if(pause.signum()<0||pause.compareTo(new BigDecimal("1440"))>0)throw new IllegalArgumentException("Confira os minutos de pausa.");
            long unpaid=pause.multiply(new BigDecimal("60000")).setScale(0,RoundingMode.HALF_UP).longValueExact();
            long work=to-from-unpaid;
            if(work<0)throw new IllegalArgumentException("As pausas não podem ser maiores que o período trabalhado.");
            if(work>86400000L)throw new IllegalArgumentException("Registre no máximo 24 horas pagas por lançamento.");
            return new Entry(from,to,work);
        } catch(IllegalArgumentException e){throw e;}
        catch(Exception e){throw new IllegalArgumentException("Confira a data, os horários e as pausas.");}
    }
}
