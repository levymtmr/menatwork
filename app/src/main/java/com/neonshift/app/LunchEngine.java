package com.neonshift.app;
import java.math.*;
public final class LunchEngine {
 public static long deduction(String minutes,long trackedMs){
  try{BigDecimal value=new BigDecimal(minutes.trim().replace(',','.'));
   if(value.signum()<0||value.compareTo(new BigDecimal("1440"))>0)throw new IllegalArgumentException("Informe de 0 a 1.440 minutos de almoço.");
   long ms=value.multiply(new BigDecimal("60000")).setScale(0,RoundingMode.HALF_UP).longValueExact();
   if(ms>trackedMs)throw new IllegalArgumentException("O almoço não pode ser maior que o tempo registrado.");return ms;
  }catch(NumberFormatException|ArithmeticException e){throw new IllegalArgumentException("Confira os minutos de almoço.");}
 }
 public static long paid(long trackedMs,String minutes){return trackedMs-deduction(minutes,trackedMs);}
}
