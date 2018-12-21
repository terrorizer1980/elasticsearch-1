package com.senzing.g2.search.elastic;

public class G2EntityInfo 
{
	// This is a container class for a JSON document for a resolved entity.
	// It corresponds to what is returned from the G2_getEntityBy*() functions.
	
	private String m_entityJsonData;
	
	public G2EntityInfo(String entityJsonData)
	{
		m_entityJsonData = entityJsonData;
	}

	public String getEntityJsonData() { return m_entityJsonData; }
	
	public G2EntitySearchData getEntitySearchData()
	{
		return new G2EntitySearchData(m_entityJsonData);
	}
}

