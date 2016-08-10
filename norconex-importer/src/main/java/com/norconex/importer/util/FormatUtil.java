/* Copyright 2015-2016 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.importer.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Utility methods related to formatting.  Date formatting from string
 * uses the US English locale when the locale is not specified.
 * @author Pascal Essiembre
 * @since 2.2.0
 */
public final class FormatUtil {
//TODO consider moving to Norconex Commons Lang

    private static final Logger LOG = LogManager.getLogger(FormatUtil.class);
    
    private FormatUtil() {
    }


    /**
     * Formats a string representation of a date, into another string date
     * format.
     * @param dateString the date to format
     * @param fromFormat source format (<code>null</code> means EPOCH)
     * @param toFormat target format (<code>null</code> means EPOCH)
     * @return formatted date string, or <code>null</code> if unable to format
     */
    public static String formatDateString(
            String dateString, String fromFormat, String toFormat) {
        return formatDateString(dateString, fromFormat, toFormat, null);
    }

    /**
     * Formats a string representation of a date, into another string date
     * format.
     * @param dateString the date to format
     * @param fromFormat source format
     * @param toFormat target format
     * @param fieldName optional field name for referencing in error messages
     * @return formatted date string
     */
    public static String formatDateString(
            String dateString, String fromFormat, 
            String toFormat, String fieldName) {
        return formatDateString(
                dateString, fromFormat, null, toFormat, null, fieldName);
    }
    
    
    /**
     * Formats a string representation of a date, into another string date
     * format.
     * @param dateString the date to format
     * @param fromFormat source format
     * @param fromLocale source format locale
     * @param toFormat target format
     * @param toLocale target format locale
     * @param fieldName optional field name for referencing in error messages
     * @return formatted date string
     * @since 2.5.2
     */
    public static String formatDateString(String dateString, 
            String fromFormat, Locale fromLocale, 
            String toFormat, Locale toLocale, String fieldName) {
        if (StringUtils.isBlank(dateString)) {
            return null;
        }
        
        //--- Parse from date ---
        Locale sourceLocale = fromLocale;
        if (sourceLocale == null) {
            sourceLocale = Locale.US;
        }
        Date date;
        if (isEpochFormat(fromFormat)) {
            // From date format is EPOCH
            long millis = NumberUtils.toLong(dateString, -1);
            if (millis == -1) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Invalid date format" + formatFieldMsg(fieldName)
                            + "The date is expected to be of EPOCH format: "
                            + dateString);
                }
                return null;
            }
            date = new Date(millis);
        } else {
            // From date is custom format
            try {
                date = new SimpleDateFormat(
                        fromFormat, sourceLocale).parse(dateString);
            } catch (ParseException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Invalid date format" 
                            + formatFieldMsg(fieldName) + e.getMessage());
                }
                return null;
            }
        }

        //--- Format to date ---
        Locale targetLocale = toLocale;
        if (targetLocale == null) {
            targetLocale = Locale.US;
        }
        String toDate;
        if (isEpochFormat(toFormat)) {
            // To date format is EPOCH
            toDate = Long.toString(date.getTime());
        } else {
            toDate = new SimpleDateFormat(
                    toFormat, targetLocale).format(date);
        }
        return toDate;
    }
    
    // returns true if blank or "EPOCH" (case insensitive).
    private static boolean isEpochFormat(String format) {
        return StringUtils.isBlank(format) || "EPOCH".equalsIgnoreCase(format);
    }
    
    private static String formatFieldMsg(String fieldName) {
        String fieldMsg = ". ";
        if (StringUtils.isNotBlank(fieldName)) {
            fieldMsg = " for field " + fieldName + ". ";
        }
        return fieldMsg;
    }
}
