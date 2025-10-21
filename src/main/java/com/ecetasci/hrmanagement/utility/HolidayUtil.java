package com.ecetasci.hrmanagement.utility;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class HolidayUtil {

    // Burada resmi tatil günlerini tanımlayabilirsin
    private static final Set<LocalDate> OFFICIAL_HOLIDAYS = new HashSet<>();

    static {
        // Örnek tatiller (Türkiye için)
        OFFICIAL_HOLIDAYS.add(LocalDate.of(2025, 1, 1));   // Yılbaşı
        OFFICIAL_HOLIDAYS.add(LocalDate.of(2025, 4, 23));  // 23 Nisan
        OFFICIAL_HOLIDAYS.add(LocalDate.of(2025, 5, 1));   // İşçi Bayramı
        OFFICIAL_HOLIDAYS.add(LocalDate.of(2025, 5, 19));  // 19 Mayıs
        OFFICIAL_HOLIDAYS.add(LocalDate.of(2025, 8, 30));  // Zafer Bayramı
        OFFICIAL_HOLIDAYS.add(LocalDate.of(2025, 10, 29)); // Cumhuriyet Bayramı
        // dini bayramlar yıllara göre değişiyor, dilersen DB’den veya API’den çekebilirsin
    }

    /**
     * startDate ve endDate arasındaki iş günlerini (hafta sonu + tatiller hariç) sayar.
     */
    public static int calculateWorkingDays(LocalDate startDate, LocalDate endDate) {
        int workingDays = 0;
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            if (isWorkingDay(current)) {
                workingDays++;
            }
            current = current.plusDays(1);
        }

        return workingDays;
    }

    /**
     * Belirli bir günün hafta sonu veya tatil olup olmadığını kontrol eder.
     */
    public static boolean isWorkingDay(LocalDate date) {
        // Cumartesi veya Pazar mı?
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        // Resmi tatil mi?
        if (OFFICIAL_HOLIDAYS.contains(date)) {
            return false;
        }
        return true;
    }
}
