package com.senzing.g2.search.elastic;

public class IndexNameKeeper 
{
	// index name identifiers
	private String m_indexCollectionName;
	private final static String m_indexSuffixForResEntities = "g2resentity";
	private final static String m_indexSuffixForRecords = "g2record";

	public IndexNameKeeper(String indexCollectionName)
	{
		m_indexCollectionName = indexCollectionName;
	}

	public String getIndexNameForEntities()
	{
		return m_indexCollectionName + "_" + m_indexSuffixForResEntities;
	}
	public String getIndexTypeForEntities()
	{
		return m_indexSuffixForResEntities;
	}

	public String getIndexNameForRecords()
	{
		return m_indexCollectionName + "_" + m_indexSuffixForRecords;
	}
	public String getIndexTypeForRecords()
	{
		return m_indexSuffixForRecords;
	}
}
