package com.product.RealestateTokenization.Elasticsearch;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PropertySearchRepository extends ElasticsearchRepository<PropertySearch, String> {

    List<PropertySearch> findByNameContainingIgnoreCase(String name);

    List<PropertySearch> findByLocationTitleContaining(String location);
    @Query("""
{
  "bool": {
    "must": [
      {
        "multi_match": {
          "query": "?0",
          "fields": ["name", "about", "locationTitle"],
          "fuzziness": "AUTO"
        }
      }
    ],
    "filter": [
      { "term": { "islive": true }}
    ]
  }
}
""")
    List<PropertySearch> searchAll(String keyword);
}
