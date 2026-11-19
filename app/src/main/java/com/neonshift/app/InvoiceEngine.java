package com.neonshift.app;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/** Invoice Deck rules: ceil each entry to 15 min, then round money half up. */
public final class InvoiceEngine {
    public static final class Item {
        public final LocalDate date;
        public final String address, sourceId;
        public final BigDecimal actual, billed, amount, premium;
        public final boolean dayOff;
        public Item(String date, String address, String hours, String rate, String sourceId) {
            this(date,address,hours,rate,sourceId,"0");
        }
        public Item(String date,String address,String hours,String rate,String sourceId,String extra) {
            try { this.date = LocalDate.parse(date); }
            catch (java.time.DateTimeException e) { throw new IllegalArgumentException("Confira a data de cada lançamento."); }
            this.address = address.trim().replaceAll("\\s+", " ");
            if (this.address.length() > 1000) throw new IllegalArgumentException("Local de trabalho muito longo.");
            this.sourceId = sourceId;
            this.dayOff = this.address.equalsIgnoreCase("Day off");
            this.actual = dayOff ? BigDecimal.ZERO : hours(hours);
            if (actual.signum() > 0 && this.address.isEmpty()) throw new IllegalArgumentException("Informe o local de cada trabalho.");
            this.billed = actual.multiply(new BigDecimal("4")).setScale(0, RoundingMode.CEILING).divide(new BigDecimal("4"));
            try{premium=new BigDecimal(extra);if(premium.signum()<0||premium.compareTo(new BigDecimal("1000000"))>0)throw new NumberFormatException();}catch(Exception e){throw new IllegalArgumentException("Confira o adicional de overtime.");}
            this.amount = billed.multiply(rate(rate)).add(premium).setScale(2, RoundingMode.HALF_UP);
        }
    }
    public static BigDecimal hours(String raw) {
        String s = raw.trim().replace(',', '.');
        if (s.isEmpty()) s = "0";
        if (s.length() > 32) throw new IllegalArgumentException("Horas inválidas.");
        BigDecimal result;
        try {
            if (s.matches("\\d{1,2}:[0-5]\\d")) {
                String[] p = s.split(":");
                // Fifteen-minute ceilings must not be disturbed by recurring fractions.
                result = new BigDecimal(p[0]).add(new BigDecimal(p[1]).divide(new BigDecimal("60"), 16, RoundingMode.HALF_UP));
            } else {
                if (!s.matches("\\d+(?:\\.\\d+)?")) throw new NumberFormatException();
                result = new BigDecimal(s);
            }
        } catch (Exception e) { throw new IllegalArgumentException("Horas inválidas. Use 8,5 ou 08:30."); }
        if (result.signum() < 0 || result.compareTo(new BigDecimal("24")) > 0)
            throw new IllegalArgumentException("Cada lançamento deve ter entre 0 e 24 horas.");
        return result;
    }
    public static BigDecimal rate(String raw) {
        try {
            BigDecimal r = new BigDecimal(raw.trim().replace(',', '.'));
            if (r.signum() <= 0 || r.compareTo(new BigDecimal("10000")) > 0) throw new NumberFormatException();
            return r;
        } catch (Exception e) { throw new IllegalArgumentException("Confira o valor por hora do invoice."); }
    }
    public static BigDecimal total(List<Item> items) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean worked = false;
        for (Item i : items) { total = total.add(i.amount); worked |= i.actual.signum() > 0; }
        if (!worked) throw new IllegalArgumentException("Inclua pelo menos um trabalho com horas.");
        return total;
    }
    public static String number(int value) {
        if (value < 1 || value > 999999999) throw new IllegalArgumentException("Número de invoice inválido.");
        return String.format(Locale.ROOT, "%03d", value);
    }
}
