package com.norconex.importer.handler.filter.impl;

public class LongDate extends Date 
{

	@Override
	public String getDateType() 
	{
		return "yyyy-MM-dd'T'HH:mm:ss.SSS";
	}

}
