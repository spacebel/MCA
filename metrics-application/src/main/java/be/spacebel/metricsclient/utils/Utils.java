/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package be.spacebel.metricsclient.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;

/**
 * Common utilities
 * 
 * @author mng
 */
public class Utils {

    public static String defaultEndDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        return formatter.format(new Date());
    }

    public static String defaultStartDate(int numberOfDays) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, ((-1) * numberOfDays));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
        return formatter.format(cal.getTime());
    }

    public static long startDateInSeconds(String startDate) throws ParseException {
        return toSeconds(startDate + " 00:00:00");
    }

    public static long endDateInSeconds(String endDate) throws ParseException {
        return toSeconds(endDate + " 23:59:59");
    }

    public static double validateDouble(double d) {
        Double dObj = d;
        if (dObj.isInfinite() || dObj.isNaN() || d < 0) {
            return 0;
        }
        return d;
    }

    public static String formatDate(long unixTimestamp) {
        Date time = new Date((long) unixTimestamp * 1000);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(time);
    }

    private static long toSeconds(String dateStr) throws ParseException {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = formatter.parse(dateStr);
        return (date.getTime() / 1000);
    }

    public static String encloseField(String field) {
        if (field == null || field.isEmpty()) {
            return "''";
        }

        field = field.replaceAll("'", "''");

        return "'" + field + "'";
    }

    public static String removeLastSlashs(String path) {
        if (StringUtils.isNotEmpty(path)) {
            while (true) {
                if (path.endsWith("/")) {
                    path = path.substring(0, (path.length() - 1));
                } else {
                    break;
                }
            }
        }
        return path;
    }

    public static String removeFirstSlashs(String path) {
        if (StringUtils.isNotEmpty(path)) {
            while (true) {
                if (path.startsWith("/")) {
                    path = path.substring(1);
                } else {
                    break;
                }
            }
        }
        return path;
    }

    public static String trimSlashs(String path) {
        path = removeFirstSlashs(path);
        path = removeLastSlashs(path);
        return path;
    }

}
