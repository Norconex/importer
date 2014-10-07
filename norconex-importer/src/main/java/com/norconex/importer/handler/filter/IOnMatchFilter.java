/* Copyright 2014 Norconex Inc.
 * 
 * This file is part of Norconex Importer.
 * 
 * Norconex Importer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Importer is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Importer. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.importer.handler.filter;


/**
 * Tells the collector that a filter is of "OnMatch" type.  This means,
 * if one or more filters of type "include" exists in a set of filters, 
 * at least one of them must be matched for a document (or other object)
 * to be "included".  Only one filter of type "exclude" needs to be 
 * matched or the document (or other object) to be excluded.
 * Filters of type "exclude" have precedence over includes.
 * @author Pascal Essiembre
 */
public interface IOnMatchFilter {

    /**
     * Gets the the on match action (exclude or include).
     * @return on match (exclude or include)
     */
    OnMatch getOnMatch();
}
