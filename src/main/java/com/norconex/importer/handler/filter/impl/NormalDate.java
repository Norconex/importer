package com.norconex.importer.handler.filter.impl;

public class NormalDate extends Date {

	@Override
	public String getDateType() 
	{		
		return "yyyy-MM-dd'T'HH:mm:ss";
	}

}
