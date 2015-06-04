/* Copyright 2015 Norconex Inc.
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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
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
        if (StringUtils.isBlank(dateString)) {
            return null;
        }
        
        //--- Parse from date ---
        Date date = null;
        if (StringUtils.isBlank(fromFormat)) {
            // From date format is EPOCH
            long millis = NumberUtils.toLong(dateString, -1);
            if (millis == -1) {
                LOG.warn("Invalid date format" + formatFieldMsg(fieldName)
                        + "When no \"fromFormat\" is specified, the date "
                        + "value is expected to be of EPOCH format.");
                return null;
            }
            date = new Date(millis);
        } else {
            // From date is custom format
            try {
                date = new SimpleDateFormat(fromFormat).parse(dateString);
            } catch (ParseException e) {
                LOG.warn("Invalid date format" + formatFieldMsg(fieldName), e);
                return null;
            }
        }

        //--- Format to date ---
        String toDate = null;
        if (StringUtils.isBlank(toFormat)) {
            // To date foramt is EPOCH
            toDate = Long.toString(date.getTime());
        } else {
            toDate = new SimpleDateFormat(toFormat).format(date);
        }
        return toDate;
    }
    private static String formatFieldMsg(String fieldName) {
        String fieldMsg = ". ";
        if (StringUtils.isNotBlank(fieldName)) {
            fieldMsg = " for field " + fieldName + ". ";
        }
        return fieldMsg;
    }
}
