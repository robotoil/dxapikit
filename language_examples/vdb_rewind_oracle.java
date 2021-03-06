import java.io.*;
import java.net.*;
import java.util.*;
import java.time.LocalDateTime; 
import org.json.simple.*;
import org.json.simple.parser.*;

//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// Copyright (c) 2019 by Delphix. All rights reserved.
//
// Program Name : vdb_rewind_oracle.java
// Description  : Delphix API Example for Java
// Author       : Alan Bitterman
// Created      : 2019-05-02
// Version      : v1.0.1 2019-05-06
//
// Requirements :
//  1.) Requires json-simple-1.1.jar 
//  2.) Change values below as required
//
// Compile:
//  javac -cp .:json-simple-1.1.jar vdb_rewind_oracle.java
//
// Usage:
//  java -cp .:json-simple-1.1.jar vdb_rewind_oracle
//
// Notes:
//  1.  Only valid for Oracle dSource/VDBs as coded
//  2.  This is a simple example and for production use additional
//      code is required, for example;
//      x.) Only search for dSource/VDB name based on type
//      x.) Error trapping logic
//      x.) etc.
//
///////////////////////////////////////////////////////////
//                    DELPHIX CORP                       //
// Please make changes to the parameters below as req'd! //
///////////////////////////////////////////////////////////

class vdb_rewind_oracle {
  public static void main(String[] args) throws Exception {

    //
    // Variables ...
    //
    String url_str = "http://172.16.160.195/resources/json/delphix";
    String username = "delphix_admin";
    String password = "delphix";

    String source_name = "VBITT";     	// VDB name to rewind
    int delaysec = 10;     		// Job Monitor Delay in Seconds 

    ///////////////////////////////////////////////////////////
    //         NO CHANGES REQUIRED BELOW THIS POINT          //
    ///////////////////////////////////////////////////////////

    //
    // Local Variables ...
    //
    String urlParameters = "";
    String endpoint = "";
    String cookie = "";
    String line = "";
    URL endpointURL = null;
    HttpURLConnection request1 = null;

    String ref = "";
    String objtype = "";
    String namespace = "";
    String parent = "";

    //
    // Let the fun begin ...
    //
    //System.out.println("Trying ...\n");
    try {

      //
      // Session ...
      //
      endpoint = (url_str + "/session");
      urlParameters = "{\"type\":\"APISession\",\"version\":{\"type\":\"APIVersion\",\"major\":1,\"minor\":7,\"micro\":0}}";

      String session_str[] = getEndPoint("POST", endpoint, cookie, urlParameters);
      System.out.println("session> "+session_str[0]+"\n") ;
      //System.out.println("cookie> "+session_str[1]+"\n") ;
      cookie = session_str[1]; 

      //
      // Login ...
      //
      endpoint = (url_str + "/login");
      urlParameters = ("{\"type\":\"LoginRequest\",\"username\":\""+username+"\"," + "\"password\": \""+password+"\"" +  "}");

      endpointURL = new URL(endpoint);

      String login_str[] = getEndPoint("POST", endpoint, cookie, urlParameters);
      System.out.println("login> "+login_str[0]+"\n") ;

      //
      // 5.3 fix backwards compatible
      //
      // After a login, recreate the cookie ...
      //
      //System.out.println("cookie> "+login_str[1]+"\n") ;
      cookie = login_str[1];

      //
      // Database API call ...
      //
      endpoint = (url_str + "/database");   
      urlParameters = null;
      String str[] = getEndPoint("GET", endpoint, cookie, urlParameters);
      //
      // Quick and dirty readable JSON string ...
      //
      //String str = resp.toString().replaceAll(",",",\n");
      ////// System.out.println("system results> "+str[0]);


      JSONParser parser = new JSONParser();
      //Object obj = parser.parse(new FileReader(sessionFilename));
      Object obj = parser.parse(str[0]);
      JSONObject jsonObject = (JSONObject) obj;

/*
      //
      // Show key value pairs for jsonObject ...
      //
      for(Iterator iterator = jsonObject.keySet().iterator(); iterator.hasNext();) {
         String key = (String) iterator.next();
         System.out.println(key + "---" + jsonObject.get(key));
         //System.out.println("\n");
      }
*/

      //
      // Get result array ...
      //
      JSONArray arr = (JSONArray) jsonObject.get("result");
      Iterator i = arr.iterator();
      while (i.hasNext()) {
        //System.out.println(i.next());
        // 
        // Process objects in array ...
        // 
        JSONObject pobj = (JSONObject) i.next();
        /*
        for(Iterator iterator2 = pobj.keySet().iterator(); iterator2.hasNext();) {
           String key = (String) iterator2.next();
           System.out.println(key + " --- " + pobj.get(key) + "<br />");
        }
        */

        namespace = (String) pobj.get("namespace");
        String key = (String) pobj.get("name");
        if (key.equals(source_name) && namespace==null) {
           //source_name = (String) pobj.get("name");
           ref = (String) pobj.get("reference");
           objtype = (String) pobj.get("type");
           parent = (String) pobj.get("provisionContainer");
        }   
        System.out.print(".");
      }
        
      System.out.println(".");
      System.out.println("Name: "+source_name+"");
      System.out.println("Reference: "+ref+"");
      System.out.println("Namespace: "+namespace+"");
      System.out.println("Type: "+objtype+"");
      System.out.println("provisionContainer: "+parent+"");
      //if (namespace == null) System.out.println("Namespace: "+namespace+" is NULL");

/*
## Container type values ...
# OracleDatabaseContainer

## Sync Type Values ...
# OracleSyncParameters
*/

      //
      // Defaults for non-coded container types ...
      //
      String sync_type = "SyncParameters";
      String refresh_type = "RefreshParameters";
      String rollback_type = "RollbackParameters";

      if ( objtype.equals("OracleDatabaseContainer") ) {
         sync_type = "OracleSyncParameters";
         refresh_type = "OracleRefreshParameters";
         rollback_type = "OracleRollbackParameters";
      } else {
         System.out.println("Container Type: "+objtype+" code not included yet, trying default options ...");
      }

      //
      // Perform Action ... 
      //
      String json = "";
      json="{ \"type\": \""+rollback_type+"\", \"timeflowPointParameters\": { \"type\": \"TimeflowPointSemantic\", \"container\": \""+ref+"\" } }";
      System.out.println("json> "+json+"");

      //
      // VDB rollback (rewind) operations request ...
      //
      if (! ref.equals("")) {
         endpoint = (url_str + "/database/"+ref+"/rollback");
         urlParameters = (json);
         String results_str[] = getEndPoint("GET", endpoint, cookie, urlParameters);
         System.out.println("result> "+results_str[0]);

         // 
         // Get Job status and job number ...
         //
         //{"type":"OKResult","status":"OK","result":"","job":"JOB-400","action":"ACTION-953"}
         Object jobj = parser.parse(results_str[0]);
         JSONObject j1 = (JSONObject) jobj;
         String status = (String) j1.get("status");
         String job = (String) j1.get("job");

         //System.out.println("status> "+status);
         //System.out.println("job> "+job);
 
         // 
         // Monitor Job ...
         //
         if (status.equals("OK")) {
            System.out.println("Job: "+job+" submitted successfully ...");
            String job_results = jobStatus(job, url_str, cookie, delaysec);
            ///System.out.println("Job Results: "+job_results);
         } else {
            System.out.println("Error: "+status+", please check output for errors ..."+results_str[0]);
            System.exit(1);
         }

      }

    } catch (Exception e0) {
      System.out.println("Exception: " + e0.getMessage());
      e0.printStackTrace();
    }

  }

  private static String cut(String str, int l) {
    //String shorter = str.substring(l,str.length()-l+1);
    String shorter = str.substring(l);
    return shorter;
  }

  private static String[] getEndPoint(String request_type, String endpoint, String cookie, String urlParameters) {
    URL endpointURL = null;
    HttpURLConnection request1 = null;
    BufferedReader rd = null;
    DataOutputStream wr = null;
    StringBuilder resp = null;
    resp = new StringBuilder();
    String line = "";

    try {

      endpointURL = new URL(endpoint);
      request1 = (HttpURLConnection)endpointURL.openConnection();
      request1.setRequestProperty("Content-Type", "application/json");
      request1.setRequestProperty("Content-Language", "en-US");
      request1.setUseCaches(false);
      request1.setDoInput(true);
      request1.setDoOutput(true);
      // Get ...
      if (request_type.equals("GET")) {
         request1.setRequestMethod("GET");
      } else {
         request1.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
         request1.setRequestMethod("POST");
      } 
      if (! cookie.equals("")) {
         request1.setRequestProperty("Cookie", cookie);
      }
      if (urlParameters != null) {
         wr = new DataOutputStream(request1.getOutputStream());
         wr.writeBytes(urlParameters);
         wr.flush();
         wr.close();
      } else {
         request1.connect();
      }
      rd = new BufferedReader(new InputStreamReader(request1.getInputStream()));
      resp = new StringBuilder();
      line = null;
      while ((line = rd.readLine()) != null) {
         resp.append(line + '\n');
      }

    } catch (Exception e0) {
      System.out.println("Exception: " + e0.getMessage());
      e0.printStackTrace();
    } finally {
      if (rd != null) {
        try {
          rd.close();
        } catch (IOException ex) {}
        rd = null;
      }
      if (wr != null) {
        try {
          wr.close();
        } catch (IOException ex) {}
        wr = null;
      }
    }

    // 5.3 fix for setting cookie after login, backwards compatible ...
    if (endpoint.endsWith("login")) {
       cookie = "";
    }

    //
    // Session Cookie ...
    //
    if (cookie.equals("")) {
       String headerName = null;
       String cookieStr = null;
       for (int i = 1; (headerName = request1.getHeaderFieldKey(i)) != null; i++) {
         if (headerName.equals("Set-Cookie")) {
           cookieStr = request1.getHeaderField(i);
         }
       }
       cookieStr = cookieStr.substring(0, cookieStr.indexOf(";"));
       String cookieName = cookieStr.substring(0, cookieStr.indexOf("="));
       String cookieValue = cookieStr.substring(cookieStr.indexOf("=") + 1, cookieStr.length());
       cookie = cookieName + "=" + cookieValue;
    }
    return new String[] {resp.toString(), cookie};
  }

  private static String jobStatus(String job, String url_str, String cookie, int delaysec) {
    String line = "jobStatus: "+job+" ";
    try {
      if (! job.equals("")) {

        // Job Information ...
        String endpoint = (url_str + "/job/"+job+"");
        String urlParameters = null;
        String results_str[] = getEndPoint("GET", endpoint, cookie, urlParameters);
        //System.out.println("result> "+results_str[0]);

        JSONParser parser = new JSONParser();
        Object jobj = parser.parse(results_str[0]);
        JSONObject j1 = (JSONObject) jobj;
        JSONObject j2 = (JSONObject) j1.get("result");
        String jobState = (String) j2.get("jobState");
        String perComplete = String.valueOf((double) j2.get("percentComplete"));
        LocalDateTime myDate = LocalDateTime.now();
        System.out.println("Current status as of "+myDate+": "+jobState+" "+perComplete+"% Completed");
        while (jobState.equals("RUNNING")) {
          //System.out.println("Sleeping for "+delaysec+" secs ");
          //TimeUnit.SECONDS.sleep(delaysec);
          Thread.sleep(delaysec*1000);
          String str1[] = getEndPoint("GET", endpoint, cookie, urlParameters);
          jobj = parser.parse(str1[0]);
          j1 = (JSONObject) jobj;
          j2 = (JSONObject) j1.get("result");
          jobState = (String) j2.get("jobState");
          perComplete = String.valueOf((double) j2.get("percentComplete")); 
          myDate = LocalDateTime.now();
          System.out.println("Current status as of "+myDate+": "+jobState+" "+perComplete+"% Completed");
          //System.out.println("breaking");
          //break;
        }

        // Producing final status ...
        if ( ! jobState.equals("COMPLETED")) {
          System.out.println("Error: Delphix Job Did not Complete, please check GUI "+jobState+"");
          line = line+"Error: Delphix Job Did not Complete, please check GUI "+jobState+"";
        } else {
          System.out.println("Job: "+job+" "+jobState+" "+perComplete+"% Completed ...");
          line = line+jobState+" "+perComplete+"% Completed ...";
        }

      } else { 

        System.out.println("Job Number Missing: "+job+"");
        line = line+"Job Number Missing: "+job+"";
      }      
    } catch (Exception e) {
      System.out.println("Exception: " + e.getMessage());
      e.printStackTrace();
    }
    return line;
  }

  private static String getParams(String filename) {
    String line = "\"host\":\"local\"";
    try {
      System.out.println("test");
    } catch (Exception e) {
      System.out.println("Exception: " + e.getMessage());
      e.printStackTrace();
    }
    return line;
  }

}
