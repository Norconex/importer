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
package com.norconex.collector.http.brokenURLs;

import java.io.File;
import java.io.Serializable;


/**
 * Reports broken URLs of the website that is being crawled.
 * This feature is useful for webmaster for example to detect broken links 
 * on certain websites. The list reflects the broken links counter 
 * that is displayed at the end of each crawl, which provides the total number 
 * of broken links.
 * This feature can be enabled using the configration file.
 * @author Khalid AlHomoud
 */
public interface IBrokenURLsReporter extends Serializable  {

    /**
     * Adds a broken URL to the list.
     * @param brokenURL a simple string of the broken URL.
     */
    void addBrokenURL(String brokenURL);
    /**
     * Create a report of the broken URLs.
     * @return 
     */
    File createReport(String crawlerName, File baseDir);
}
