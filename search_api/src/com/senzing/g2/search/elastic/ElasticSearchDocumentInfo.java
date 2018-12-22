package com.senzing.g2.search.elastic;

public class ElasticSearchDocumentInfo
{
	// This class represents a data entry in the Elastic Search index.  It
	// consists of an ID used to identify the entry, and a JSON document which is to be
	// indexed for that entry.  (These could represent a resolved entity, a single record, etc.)
	
	private String m_id;
	private String m_jsonData;
	
	public ElasticSearchDocumentInfo(String id,String jsonData)
	{
		m_id = id;
		m_jsonData = jsonData;
	}
	
	public String getID() { return m_id; }
	public String getJsonData() { return m_jsonData; }
}
