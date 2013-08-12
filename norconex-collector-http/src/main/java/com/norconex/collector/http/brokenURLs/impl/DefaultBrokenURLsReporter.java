/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex HTTP Collector.
 * 
 * Norconex HTTP Collector is free software: you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex HTTP Collector is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex HTTP Collector. If not, 
 * see <http://www.gnu.org/licenses/>.
 */
package com.norconex.collector.http.brokenURLs.impl;

/**
 * <p>
 * Default implementation of creating a broken URLs report as a file inside
 * the working crawler directory. In this implementation, the file is called 
 * "crawlerName.log" under a directory called "brokenURLsReports".
 * </p>
 * 
 * @author Khalid AlHomoud
 */
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.norconex.collector.http.brokenURLs.IBrokenURLsReporter;

public class DefaultBrokenURLsReporter implements IBrokenURLsReporter {


    private static final long serialVersionUID = 4905249307194032181L;

    private static final Logger LOG = 
            LogManager.getLogger(DefaultBrokenURLsReporter.class);

    public static final String DIR_NAME = "brokenURLsReports";

    private ArrayList<String> brokenURLsList = new ArrayList<String>();



    @Override
    public void addBrokenURL(String brokenURL) {
        brokenURLsList.add(brokenURL);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("The URL " + brokenURL + " is being added to the broken"
                    + "links report.");
        }

    }

    @Override
    public File createReport(String crawlerName, File baseDir) {
        if (!brokenURLsList.isEmpty()) {
            File report = new File(baseDir + "/" + DIR_NAME + "/" + crawlerName 
                    + ".log");

            if (!report.getParentFile().exists()) {
                report.getParentFile().mkdir();
            }

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(
                        report));

                for (String item : brokenURLsList) {
                    writer.write(item);
                    writer.newLine();
                }
               
                writer.close();
                
            } catch (IOException e) {
                new IOException("The Broken URLs report could not be created " 
                        + e);
            }
            return report;

        }
        return null;
    }

}
