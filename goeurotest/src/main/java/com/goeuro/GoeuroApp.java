package com.goeuro;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goeuro.location.City;

public class GoeuroApp {
	
	private static final String FILE_NAME = System.getProperty("user.home") + File.separator + "goeurotest.csv";
	private static Logger logger = Logger.getLogger(GoeuroApp.class.getName());
	private static final String COMMA_DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";
	private static final String FILE_HEADER = " _id, name, type, latitude, longitude";
	private static  String finalJson;

	public GoeuroApp() {
	}

	public static void main(String[] args) 
	{
		try 
		{
			if(args.length == 0){
				logger.log(Level.INFO, "Did not enter valid argument. Please verify");
				System.exit(0);
			}
			
			DefaultHttpClient httpClient = new DefaultHttpClient();

			int timeout = 5; // seconds
			HttpParams httpParams = httpClient.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParams, timeout * 1000);
			HttpConnectionParams.setSoTimeout(httpParams, timeout * 1000);
			HttpGet getRequest = new HttpGet("http://api.goeuro.com/api/v2/position/suggest/en/"+args[0]);
			getRequest.addHeader("accept", "application/json");

			HttpResponse response = httpClient.execute(getRequest);

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : " + response.getStatusLine().getStatusCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader((response.getEntity().getContent())));

			StringBuffer json = new StringBuffer();
			String temp;
			while ((temp = br.readLine()) != null) {
				json.append(temp);
			}

			finalJson = json.toString();

			httpClient.getConnectionManager().shutdown();

			prepareCSVFromJson();

			logger.log(Level.INFO,FILE_NAME+" file was created successfully !!!");
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error occurred while invoking Rest API", e);
		}
	}

	private static void prepareCSVFromJson() {
		FileWriter fileWriter = null;
		try 
		{
			logger.log(Level.INFO,"Executing prepareCSVFromJson...");
			
			if(!finalJson.isEmpty()){
				
				List<City> cities = new ObjectMapper().readValue(finalJson, new TypeReference<List<City>>() {});

				if (cities != null && !cities.isEmpty()) {

					fileWriter = new FileWriter(FILE_NAME);
					
					fileWriter.append(FILE_HEADER.toString());
					fileWriter.append(NEW_LINE_SEPARATOR);
					for (City city : cities) {
						fileWriter.append(city.get_id()+COMMA_DELIMITER+city.getName()+COMMA_DELIMITER+city.getType()
						+COMMA_DELIMITER+city.getGeo_position().getLatitude()+COMMA_DELIMITER+city.getGeo_position().getLongitude());
						fileWriter.append(NEW_LINE_SEPARATOR);
					}

				}
			}

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error occurred while generating CSV file", e);
		}finally {
			if(fileWriter != null){
				try {
					fileWriter.flush();
					fileWriter.close();
				} catch (Exception e) {
					logger.log(Level.SEVERE, "Error occurred while flusing/closing CSV File", e);
				}
				
			}
		}

	}

}
