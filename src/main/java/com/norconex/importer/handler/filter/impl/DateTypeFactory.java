package com.norconex.importer.handler.filter.impl;

public class DateTypeFactory 
{
	public DateTypeFactory () {}
	
	public Date generateDateType (String date)
	{
		if (date == null)
			return null;
		
		if (date.contains("."))
			return new LongDate();
		else if (date.contains("M"))
			return new NormalDate();
		else
			return new CondensedDate();
	}

}
