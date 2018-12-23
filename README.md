# Senzing with ElasticSearch (PROTOTYPE)

## Overview

This code project demonstrates how the G2 engine may be used with an ElasticSearch indexing engine.  ElasticSearch provides enhanced searching capabilities on entity data.  This project is currently in the prototype stage and the APIs and functionality are expected to change significantly.

The G2 data repository contains data records and observations about known entities.  It determines which records match/merge to become single resolved entities.  These resolved entities can be indexed through the ElasticSearch engine, to provide more searchable data entities.

ElasticSearch stores its indexed entity data in a separate data repository than the G2 engine does.  Thus, ElasticSearch and G2 must both be managed in order to keep them in sync.

This Java project shows how these two tools can be combined in common code.

## Installation

### Install an ElasticSearch software package

ElasticSearch itself is a separate software product, not made by Senzing.  You must install that software package, in order to host an ElasticSearch data repository.

### Create SENZING_DIR for the G2 engine

If you do not already have an `/opt/senzing` directory on your local system, visit
   [HOWTO - Create SENZING_DIR](https://github.com/Senzing/knowledge-base/blob/master/HOWTO/create-senzing-dir.md).

### Add the G2 interface for ElasticSearch to your Senzing installation

1. Set GitHub repository environment variables.  These variables may be modified, but do not need to be modified.  The variables are used throughout the installation procedure.

    ```console
    export GIT_ACCOUNT=senzing
    export GIT_REPOSITORY=elasticsearch
    export GIT_REPOSITORY_URL="git@github.com:${GIT_ACCOUNT}/${GIT_REPOSITORY}.git"
    ```

1. Set local environment variables.

    ```console
    export GIT_ACCOUNT_DIR=~/${GIT_ACCOUNT}.git
    export GIT_REPOSITORY_DIR="${GIT_ACCOUNT_DIR}/${GIT_REPOSITORY}"
    export SENZING_DIR=/opt/senzing
    ```

1. Download the code for the ElasticSearch interface for G2 to your local Git directory.

    ```console
    mkdir --parents ${GIT_ACCOUNT_DIR}
    cd  ${GIT_ACCOUNT_DIR}
    git clone ${GIT_REPOSITORY_URL}
    ```

1. Copy G2 engine jar file into your maven repository.

    ```console
    cd ${GIT_REPOSITORY_DIR}/search_api

    mvn \
      install:install-file \
      -Dmaven.repo.local=${GIT_REPOSITORY_DIR}/search_api/maven_resources \
      -Dfile=${SENZING_DIR}/g2/lib/g2.jar \
      -DgroupId=com.senzing \
      -DartifactId=g2 \
      -Dversion=1.0.0-SNAPSHOT \
      -Dpackaging=jar
    ```

1. Build the interface for ElasticSearch.

    ```console
    cd ${GIT_REPOSITORY_DIR}/search_api

    mvn \
      -Dmaven.repo.local=${GIT_REPOSITORY_DIR}/search_api/maven_resources \
      install
    ````

1. Copy the interface library into your Senzing directory

    ```console
    sudo mkdir ${SENZING_DIR}/g2/elasticsearch
    cd ${SENZING_DIR}/g2/elasticsearch

    sudo cp \
      ${GIT_REPOSITORY_DIR}/search_api/target/g2elasticsearch-1.0.0-SNAPSHOT.jar \
      ${SENZING_DIR}/g2/elasticsearch/g2elasticsearch.jar
    ```

## Compiling Programs with the G2 interface for ElasticSearch

Using the interface jar, you can create programs for your own individual requirements.  Programs may be compiled and run by including the elastic search interface jar and the G2 engine interface jar on the Java classpath.

```console
java -classpath g2elasticsearch.jar;g2.jar my.example.program.package.MyProgamApp
```

**Note:**  As of 21 Dec 2018, the two interface jars must be specified in this order.  The two jars have conflicting versions of ElasticSearch, with the first jar being more current.  This will be resolved in future releases.

## Example Programs

The following sample programs demonstrate how entity data can be loaded into the both the G2 engine and the ElasticSearch index.  

1) G2IndexingWhileLoadingDataApp -- A program demonstrating how data may be loaded into both the G2 data repository and the ElasticSearch index concurrently.  Individual data records are loaded first into the G2 repository through the engine, and then into the ElasticSearch API through the `G2EntityDataIndexer`.  Once loaded and indexed, several queries are run to retrieve the data.

1) G2IndexingFromExportDataApp -- A program demonstrating how data may be loaded into the ElasticSearch index after the data has been previously loaded in the G2 engine.  Several data records are first loaded into the G2 engine repository.  Then, an entity export is run to pull back the resolved entities from the system.  These entity records are indexed into ElasticSearch in a streaming fashion.  Once all the data has been indexed, several queries are run to retrieve the data.

## Main Classes

The following classes represent the primary pieces of the indexing tools.

1) Class "G2EntityDataIndexer" -- A class used to load entity data into the ElasticSearch index.   It contains the necessary functions to create/destroy/clear indexes, and functions to load individual data entries into the index.

1) Class "G2QueryImp" -- A class used to query the ElasticSearch index for our entity data.  It allows for searching for specific entities based on the data attributes of those entities.  It also allows a type-ahead lookup for terms that exist as attributes in the system.

## Query results

The responses from the search queries made from the `G2QueryImp` class are returned as JSON documents.  The following are examples of the data that may be returned.

### Search Request Example (Results Found)

Search request: `"ROBERT las vegas 8922"`

Search response:

```console
{
  "QUERY": "ROBERT las vegas 8922",
  "RESULT_COUNT": 1,
  "RESULT_ENTITIES": [{
    "MATCH_SCORE": 1.1478369235992432,
    "ENTITY_DATA": {
      "RESOLVED_ENTITY": {
        "ENTITY_ID": 1,
        "LENS_ID": 1,
        "ENTITY_NAME": "ROBERT M JONES",
        "FEATURES": {
          "ADDRESS": [{
            "FEAT_DESC": "111 FIRST ST LAS VEGAS NV 89111",
            "LIB_FEAT_ID": 4,
            "UTYPE_CODE": "HOME",
            "FEAT_DESC_VALUES": [{
              "FEAT_DESC": "111 FIRST ST LAS VEGAS NV 89111",
              "LIB_FEAT_ID": 4
            },
            {
              "FEAT_DESC": "111 1ST ST LAS VEGAS NV 89222",
              "LIB_FEAT_ID": 29
            }]
          },
          {
            "FEAT_DESC": "PO BOX 111 LAS VEGAS NV 89111",
            "LIB_FEAT_ID": 5,
            "UTYPE_CODE": "MAIL",
            "FEAT_DESC_VALUES": [{
              "FEAT_DESC": "PO BOX 111 LAS VEGAS NV 89111",
              "LIB_FEAT_ID": 5
            }]
          }],
          "DOB": [{
            "FEAT_DESC": "1981-01-02",
            "LIB_FEAT_ID": 2,
            "FEAT_DESC_VALUES": [{
              "FEAT_DESC": "1981-01-02",
              "LIB_FEAT_ID": 2
            },
            {
              "FEAT_DESC": "1981-02-01",
              "LIB_FEAT_ID": 28
            }]
          }],
          "DRLIC": [{
            "FEAT_DESC": "DL11111 NV",
            "LIB_FEAT_ID": 9,
            "FEAT_DESC_VALUES": [{
              "FEAT_DESC": "DL11111 NV",
              "LIB_FEAT_ID": 9
            }]
          }],
          "EMAIL": [{
            "FEAT_DESC": "BOB@JONESFAMILY.COM",
            "LIB_FEAT_ID": 12,
            "FEAT_DESC_VALUES": [{
              "FEAT_DESC": "BOB@JONESFAMILY.COM",
              "LIB_FEAT_ID": 12
            }]
          }],
          "GENDER": [{
            "FEAT_DESC": "M",
            "LIB_FEAT_ID": 3,
            "FEAT_DESC_VALUES": [{
              "FEAT_DESC": "M",
              "LIB_FEAT_ID": 3
            }]
          }],
          "LOGIN_ID": [{
            "FEAT_DESC": "@BOBJONES27",
            "LIB_FEAT_ID": 11,
            "FEAT_DESC_VALUES": [{
              "FEAT_DESC": "@BOBJONES27",
              "LIB_FEAT_ID": 11
            }]
          }],
          "NAME": [{
            "FEAT_DESC": "ROBERT M JONES",
            "LIB_FEAT_ID": 1,
            "UTYPE_CODE": "PRIMARY",
            "FEAT_DESC_VALUES": [{
              "FEAT_DESC": "ROBERT M JONES",
              "LIB_FEAT_ID": 1
            },
            {
              "FEAT_DESC": "BOBBY JONES",
              "LIB_FEAT_ID": 27
            }]
          }],
          "PASSPORT": [{
            "FEAT_DESC": "PP11111 US",
            "LIB_FEAT_ID": 10,
            "FEAT_DESC_VALUES": [{
              "FEAT_DESC": "PP11111 US",
              "LIB_FEAT_ID": 10
            }]
          }],
          "PHONE": [{
            "FEAT_DESC": "702-222-2222",
            "LIB_FEAT_ID": 6,
            "UTYPE_CODE": "CELL",
            "FEAT_DESC_VALUES": [{
              "FEAT_DESC": "702-222-2222",
              "LIB_FEAT_ID": 6
            }]
          },
          {
            "FEAT_DESC": "800-201-2001",
            "LIB_FEAT_ID": 7,
            "UTYPE_CODE": "WORK",
            "FEAT_DESC_VALUES": [{
              "FEAT_DESC": "800-201-2001",
              "LIB_FEAT_ID": 7
            }]
          }],
          "SSN": [{
            "FEAT_DESC": "311-11-1111",
            "LIB_FEAT_ID": 8,
            "FEAT_DESC_VALUES": [{
              "FEAT_DESC": "311-11-1111",
              "LIB_FEAT_ID": 8
            }]
          }]
        },
        "RECORD_SUMMARY": [{
          "DATA_SOURCE": "PERSON",
          "RECORD_COUNT": 2,
          "FIRST_SEEN_DT": "2018-12-20 03:42:19.100",
          "LAST_SEEN_DT": "2018-12-20 03:42:28.406"
        }],
        "LAST_SEEN_DT": "2018-12-20 03:42:28.406",
        "RECORDS": [{
          "JSON_DATA": {
            "RECORD_ID": 1001,
            "NAMES": [{
              "NAME_TYPE": "PRIMARY",
              "NAME_LAST": "Jones",
              "NAME_FIRST": "Robert",
              "NAME_MIDDLE": "M",
              "NAME_PREFIX": "Mr",
              "NAME_SUFFIX": "Jr"
            }],
            "GENDER": "M",
            "DATE_OF_BIRTH": "1/2/1981",
            "PASSPORT_NUMBER": "PP11111",
            "PASSPORT_COUNTRY": "US",
            "DRIVERS_LICENSE_NUMBER": "DL11111",
            "DRIVERS_LICENSE_STATE": "NV",
            "SSN_NUMBER": "311-11-1111",
            "ADDRESSES": [{
              "ADDR_TYPE": "HOME",
              "ADDR_LINE1": "111 First St",
              "ADDR_CITY": "Las Vegas",
              "ADDR_STATE": "NV",
              "ADDR_POSTAL_CODE": "89111",
              "ADDR_COUNTRY": "US"
            },
            {
              "ADDR_TYPE": "MAIL",
              "ADDR_LINE1": "PO Box 111",
              "ADDR_CITY": "Las Vegas",
              "ADDR_STATE": "NV",
              "ADDR_POSTAL_CODE": "89111",
              "ADDR_COUNTRY": "US"
            }],
            "PHONES": [{
              "PHONE_TYPE": "WORK",
              "PHONE_NUMBER": "800-201-2001"
            },
            {
              "PHONE_TYPE": "CELL",
              "PHONE_NUMBER": "702-222-2222"
            }],
            "EMAIL_ADDRESS": "bob@jonesfamily.com",
            "SOCIAL_HANDLE": "@bobjones27",
            "SOCIAL_NETWORK": "twitter",
            "DATA_SOURCE": "PERSON",
            "ENTITY_TYPE": "PERSON",
            "DSRC_ACTION": "A",
            "LENS_CODE": "DEFAULT"
          },
          "DATA_SOURCE": "PERSON",
          "ENTITY_TYPE": "PERSON",
          "ENTITY_KEY": "0AD619D3FA7A7C85282F28935AD7593CDB06FAE0",
          "ENTITY_NAME": "Mr Robert M Jones Jr",
          "RECORD_ID": "1001",
          "MATCH_KEY": "",
          "MATCH_SCORE": "",
          "ERRULE_CODE": "",
          "REF_SCORE": 0,
          "MATCH_LEVEL": 0,
          "LAST_SEEN_DT": "2018-12-20 03:42:19.100",
          "NAME_DATA": ["PRIMARY: Jones Robert M Mr Jr"],
          "ATTRIBUTE_DATA": ["DOB: 1/2/1981",
          "GENDER: M"],
          "IDENTIFIER_DATA": ["DRLIC: DL11111 NV",
          "EMAIL: bob@jonesfamily.com",
          "LOGIN_ID: twitter @bobjones27",
          "PASSPORT: PP11111 US",
          "SSN: 311-11-1111"],
          "ADDRESS_DATA": ["HOME: 111 First St Las Vegas NV 89111 US",
          "MAIL: PO Box 111 Las Vegas NV 89111 US"],
          "PHONE_DATA": ["WORK: 800-201-2001",
          "CELL: 702-222-2222"],
          "RELATIONSHIP_DATA": [],
          "ENTITY_DATA": [],
          "OTHER_DATA": []
        },
        {
          "JSON_DATA": {
            "RECORD_ID": 1002,
            "NAMES": [{
              "NAME_TYPE": "PRIMARY",
              "NAME_LAST": "Jones",
              "NAME_FIRST": "Bobby"
            }],
            "GENDER": "M",
            "DATE_OF_BIRTH": "2/1/1981",
            "ADDRESSES": [{
              "ADDR_TYPE": "HOME",
              "ADDR_LINE1": "111 1st St",
              "ADDR_CITY": "Las Vegas",
              "ADDR_STATE": "NV",
              "ADDR_POSTAL_CODE": "89222"
            }],
            "DATA_SOURCE": "PERSON",
            "ENTITY_TYPE": "PERSON",
            "DSRC_ACTION": "A",
            "LENS_CODE": "DEFAULT"
          },
          "DATA_SOURCE": "PERSON",
          "ENTITY_TYPE": "PERSON",
          "ENTITY_KEY": "09045287B49611BDBD2E0522AFBFC91B65C7ACBB",
          "ENTITY_NAME": "Bobby Jones",
          "RECORD_ID": "1002",
          "MATCH_KEY": "+NAME+DOB+GENDER+ADDRESS",
          "MATCH_SCORE": "14",
          "ERRULE_CODE": "CNAME_CFF_CEXCL",
          "REF_SCORE": 8,
          "MATCH_LEVEL": 1,
          "LAST_SEEN_DT": "2018-12-20 03:42:28.406",
          "NAME_DATA": ["PRIMARY: Jones Bobby"],
          "ATTRIBUTE_DATA": ["DOB: 2/1/1981",
          "GENDER: M"],
          "IDENTIFIER_DATA": [],
          "ADDRESS_DATA": ["HOME: 111 1st St Las Vegas NV 89222"],
          "PHONE_DATA": [],
          "RELATIONSHIP_DATA": [],
          "ENTITY_DATA": [],
          "OTHER_DATA": []
        }]
      },
      "RELATED_ENTITIES": [{
        "ENTITY_ID": 3,
        "LENS_ID": 1,
        "MATCH_LEVEL": 2,
        "MATCH_KEY": "+PASSPORT",
        "MATCH_SCORE": "3",
        "ERRULE_CODE": "SF1E",
        "REF_SCORE": 6,
        "IS_DISCLOSED": 0,
        "IS_AMBIGUOUS": 0,
        "ENTITY_NAME": "MARTIN JONZE",
        "RECORD_SUMMARY": [{
          "DATA_SOURCE": "PERSON",
          "RECORD_COUNT": 1,
          "FIRST_SEEN_DT": "2018-12-20 03:42:30.830",
          "LAST_SEEN_DT": "2018-12-20 03:42:30.830"
        }],
        "LAST_SEEN_DT": "2018-12-20 03:42:30.830"
      },
      {
        "ENTITY_ID": 4,
        "LENS_ID": 1,
        "MATCH_LEVEL": 3,
        "MATCH_KEY": "+SURNAME+ADDRESS-GENDER-SSN-DRLIC-PASSPORT",
        "MATCH_SCORE": "12",
        "ERRULE_CODE": "CFF_SURNAME",
        "REF_SCORE": 4,
        "IS_DISCLOSED": 0,
        "IS_AMBIGUOUS": 0,
        "ENTITY_NAME": "ELIZABETH R JONES",
        "RECORD_SUMMARY": [{
          "DATA_SOURCE": "PERSON",
          "RECORD_COUNT": 1,
          "FIRST_SEEN_DT": "2018-12-20 03:42:30.843",
          "LAST_SEEN_DT": "2018-12-20 03:42:30.843"
        }],
        "LAST_SEEN_DT": "2018-12-20 03:42:30.843"
      }]
    },
    "MATCH_FIELDS": {
      "RECORDS.JSON_DATA.ADDRESSES.ADDR_CITY": ["<G2_MATCHED_FIELD>Las</G2_MATCHED_FIELD> <G2_MATCHED_FIELD>Vegas</G2_MATCHED_FIELD>"],
      "RECORDS.JSON_DATA.ADDRESSES.ADDR_POSTAL_CODE": ["<G2_MATCHED_FIELD>89222</G2_MATCHED_FIELD>"],
      "RECORDS.JSON_DATA.NAMES.NAME_FIRST": ["<G2_MATCHED_FIELD>Robert</G2_MATCHED_FIELD>"]
    }
  }]
}
```

### Search Request Example (No Results Found)

Search request: `"my_pretend_data"`

Search response:

```console
{
  "QUERY": "my_pretend_data",
  "RESULT_COUNT": 0,
  "RESULT_ENTITIES": []
}
```

### Look-ahead Example

Lookahead query: `"ROB"`

Lookahead response:

```console
{
  "LOOKAHEAD_PREFIX": "Ra",
  "RESULT_COUNT": 3,
  "RESULT_TERMS": ["Rachel","Ray","Raymond"]
}
```
