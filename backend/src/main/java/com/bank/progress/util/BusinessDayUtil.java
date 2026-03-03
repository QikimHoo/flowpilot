package com.bank.progress.util;

import com.bank.progress.repository.HolidayRepository;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;

@Component
public class BusinessDayUtil {

    private static HolidayRepository holidayRepository;

    public BusinessDayUtil(HolidayRepository holidayRepository) {
        BusinessDayUtil.holidayRepository = holidayRepository;
    }

    public static LocalDate plusWorkingDays(LocalDate start, int days) {
        LocalDate date = start;
        int remaining = days;
        while (remaining > 0) {
            date = date.plusDays(1);
            if (isWorkingDay(date)) {
                remaining--;
            }
        }
        return date;
    }

    public static int workingDaysBetween(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            return 0;
        }
        int count = 0;
        LocalDate d = from;
        while (d.isBefore(to)) {
            d = d.plusDays(1);
            if (isWorkingDay(d)) {
                count++;
            }
        }
        return count;
    }

    public static boolean isWorkingDay(LocalDate date) {
        // 周末判断
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return false;
        }
        // 节假日判断（如果 holidayRepository 可用）
        if (holidayRepository != null) {
            try {
                return !holidayRepository.existsByHolidayDate(date);
            } catch (Exception e) {
                // 如果查询失败，降级为只判断周末
                return true;
            }
        }
        return true;
    }
}
