#!/bin/bash
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Copyright (c) 2017 by Delphix. All rights reserved.
#
# Program Name : vdb_ase_operations.sh
# Description  : Delphix APIs to perform basic operations on ASE VDBs
# Author       : Alan Bitterman
# Created      : 2017-08-09
# Version      : v1.0.0
#
# Requirements :
#  1.) curl and jq command line libraries
#  2.) Populate Delphix Engine Connection Information . ./delphix_engine.conf
#  3.) Include ./jqJSON_subroutines.sh
#
# Usage: ./vdb_ase_operations.sh
#
# Delphix Docs Reference:
#   https://docs.delphix.com/display/DOCS/API+Cookbook%3A+Refresh+VDB
#
#########################################################
#                   DELPHIX CORP                        #
#         NO CHANGES REQUIRED BELOW THIS POINT          #
#########################################################

#########################################################
## Subroutines ...

source ./jqJSON_subroutines.sh

#########################################################
## Parameter Initialization ...

. ./delphix_engine.conf

#########################################################
#
# Command Line Arguments ...
#
ACTION=$1
if [[ "${ACTION}" == "" ]] 
then
   echo "Usage: ./vdb_operations [sync | refresh | rollback] [VDB_Name]"
   echo "Please Enter Operation: "
   read ACTION
   if [ "${ACTION}" == "" ]
   then
      echo "No Operation Provided, Exiting ..."
      exit 1;
   fi
   ACTION=$(echo "${ACTION}" | tr '[:upper:]' '[:lower:]')
fi;

SOURCE_SID="$2"
if [[ "${SOURCE_SID}" == "" ]]
then
   echo "Please enter dSource or VDB Name (case sensitive): "
   read SOURCE_SID
   if [ "${SOURCE_SID}" == "" ]
   then
      echo "No dSource or VDB Name Provided, Exiting ..."
      exit 1;
   fi
fi;
export SOURCE_SID

#########################################################
## Authentication ...

echo "Authenticating on ${BaseURL}"

RESULTS=$( RestSession "${DMUSER}" "${DMPASS}" "${BaseURL}" "${COOKIE}" "${CONTENT_TYPE}" )
#echo "Results: ${RESULTS}"
if [ "${RESULTS}" != "OK" ]
then
   echo "Error: Exiting ..."
   exit 1;
fi

echo "Session and Login Successful ..."

#########################################################
## Get database container

STATUS=`curl -s -X GET -k ${BaseURL}/database -b "${COOKIE}" -H "${CONTENT_TYPE}"`
RESULTS=$( jqParse "${STATUS}" "status" )
#echo "results> $RESULTS"

#
# Parse out container reference for name of $SOURCE_SID ...
#
CONTAINER_REFERENCE=`echo ${STATUS} | jq --raw-output '.result[] | select(.name=="'"${SOURCE_SID}"'") | .reference '`
echo "database container reference: ${CONTAINER_REFERENCE}"

#########################################################
## Get provision source database container

STATUS=`curl -s -X GET -k ${BaseURL}/database/${CONTAINER_REFERENCE} -b "${COOKIE}" -H "${CONTENT_TYPE}"`
RESULTS=$( jqParse "${STATUS}" "status" )
#echo "results> $RESULTS"

#echo "${STATUS}"
PARENT_SOURCE=`echo ${STATUS} | jq --raw-output '.result | select(.reference=="'"${CONTAINER_REFERENCE}"'") | .provisionContainer '`
echo "provision source container: ${PARENT_SOURCE}"

#########################################################
#
# start or stop the vdb based on the argument passed to the script
#
case ${ACTION} in
sync)
## ASELatestBackupSyncParameters         ASENewBackupSyncParameters            ASESpecificBackupSyncParameters 
json="{
   \"type\": \"ASELatestBackupSyncParameters\"
}"
;;
refresh)
json="{
    \"type\": \"RefreshParameters\",
    \"timeflowPointParameters\": {
        \"type\": \"TimeflowPointSemantic\",
        \"container\": \"${PARENT_SOURCE}\"
    }
}"
;;
rollback)
json="{
    \"type\": \"RollbackParameters\",
    \"timeflowPointParameters\": {
        \"type\": \"TimeflowPointSemantic\",
        \"container\": \"${CONTAINER_REFERENCE}\"
    }
}"
;;
*)
  echo "Unknown option (sync | refresh | rollback): ${ACTION}"
  echo "Exiting ..."
  exit 1;
;;
esac

echo "json> ${json}"

#
# Submit VDB operations request ...
#
STATUS=`curl -s -X POST -k --data @- ${BaseURL}/database/${CONTAINER_REFERENCE}/${ACTION} -b "${COOKIE}" -H "${CONTENT_TYPE}" <<EOF
${json}
EOF
`

#########################################################
#
# Get Job Number ...
#
JOB=$( jqParse "${STATUS}" "job" )
echo "Job: ${JOB}"

jqJobStatus "${JOB}"            # Job Status Function ...

############## E O F ####################################
echo "Done ..."
echo " "
exit 0;

