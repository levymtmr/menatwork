package com.neonshift.app;

/** Pure calculation layer; currency-neutral estimates, never a tax assessment. */
public final class CostEngine {
    public static double[] calculate(long millis, double hourly, double taxPercent,
            double km, double litresPer100, double fuelPrice, double tyresPerKm,
            double maintenancePerKm, double depreciationPerKm, double extras) {
        double gross = Math.max(0, millis) / 3600000.0 * hourly;
        double tax = gross * taxPercent / 100.0;
        double fuel = km * litresPer100 / 100.0 * fuelPrice;
        double tyres = km * tyresPerKm;
        double maintenance = km * maintenancePerKm;
        double depreciation = km * depreciationPerKm;
        double costs = fuel + tyres + maintenance + depreciation + extras;
        return new double[]{gross, tax, fuel, tyres, maintenance, depreciation,
            extras, costs, gross - tax - costs};
    }
    public static boolean valid(double value, double max) {
        return !Double.isNaN(value) && !Double.isInfinite(value) && value >= 0 && value <= max;
    }
}
