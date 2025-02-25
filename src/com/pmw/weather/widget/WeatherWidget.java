/**
 * Copyright 2015-2025 Paul Walters
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     https://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pmw.weather.widget;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyStore;
import java.text.DecimalFormat;
import java.util.Properties;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.pmw.pmwApplication;

public class WeatherWidget extends pmwApplication {

	private static final String appName = new String("Weather Widget");
	private static final String appVersion = new String("v2024.1");
    private static final String appCopyright = new String("Copyright 2015-2024");
    private static final String appAuthor = new String("Paul Walters");

	public WeatherWidget() {
		super(appName, appVersion, appCopyright, appAuthor);
		
		final WeatherWidgetFrame frame = new WeatherWidgetFrame(this);

		String propFile = this.getAppDataDir() + "/weatherWidget.properties";

		try {
			loadAppProperties(propFile);
			Properties props = getAppProperties();
			if (props.isEmpty()) {
				props.put("showWeatherURL", "https://www.weather.gov/lwx");

				String apiKey = ""; // TODO must provide an valid openweathermap API key and desired location
				String cityName = "New York,NY,US";	
				props.put("wxConditionsURL", "http://api.openweathermap.org/data/2.5/weather?"
						+ "q=" + cityName
						+ "&units=imperial&mode=json"
						+ "&appid="+apiKey);

				saveAppProperties(new File(propFile));
			}

			props.put("propertiesFile", propFile);

		} catch (IOException ex) {
			System.err.println("ERROR loading " + propFile + ": " + ex.getMessage());
		}

		// Event dispatch thread
	 	EventQueue.invokeLater(new Runnable()
    	{
	 		@Override
    		public void run()
    		{
	 			frame.start();
    		}
    	});

		String myCert = getAppProperties().getProperty("trustStore");
		if (myCert != null && !myCert.isBlank()) {
			 try {
				 InputStream is = WeatherWidget.class.getResourceAsStream(myCert);
				 
				 KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				 keyStore.load(null);

				 KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
				 keyFactory.init(keyStore, null);

				 KeyManager[] keyManagers = keyFactory.getKeyManagers();
				 KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				 trustStore.load(is, null);
				 TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
				 trustFactory.init(trustStore);
				 TrustManager[] trustManagers = trustFactory.getTrustManagers();
				 SSLContext sslContext = SSLContext.getInstance("SSL");
				 sslContext.init(keyManagers, trustManagers,  null);
				 SSLContext.setDefault(sslContext);
				 is.close();
			 }
			 catch (Exception ex) {
				 System.err.println("Error opening " + myCert + ": " + ex.getMessage());
				 System.exit(-1);
			 }
		}
		
		DecimalFormat df = new DecimalFormat("###.0");
		DecimalFormat pf = new DecimalFormat("##.00");
		DecimalFormat wf = new DecimalFormat("###");
	 			
		while (frame != null && frame.isRunning()) {
			
			try {
				URL wx = new URL(getAppProperties().getProperty("wxConditionsURL"));
				BufferedReader in = new BufferedReader(new InputStreamReader(wx.openStream()));
				String line = null;
				StringBuffer sb = new StringBuffer();
				sb.append("Weather Conditions:");
				while ((line = in.readLine()) != null) {
					// System.out.println("DEBUG " + line);
					if (line.startsWith("temperature|")) {
						String temperature = getWeatherValue(line, "temperature|");
						frame.setIconTemperature(Math.round(Double.parseDouble(temperature)));
						sb.append("\n Temperature: " + temperature + WeatherWidgetFrame.DEGREES + " ");
					}
					if (line.contains("<temp_f>")) {
						String temperature = getXMLValue(line, "<temp_f>");
						frame.setIconTemperature(Math.round(Double.parseDouble(temperature)));
						sb.append("\n Temperature: " + temperature + WeatherWidgetFrame.DEGREES + " ");
					}
					if (line.contains("\"temp\":")) {
						double temperature = getJSONDouble(line, "temp");
						frame.setIconTemperature(Math.round(temperature));
						sb.append("\n Temperature: " + df.format(temperature) + WeatherWidgetFrame.DEGREES + " ");
					}
					if (line.startsWith("wind_gust|") && line.length() > 10) {
						sb.append("\n Wind Speed: " + getWeatherValue(line, "wind_gust|") + " ");
					}
					if (line.contains("<wind_string>")) {
						sb.append("\n Wind Speed: " + getXMLValue(line, "<wind_string>") + " ");
					}
					if (line.contains("\"speed\":")) {
						sb.append("\n Wind Speed: " + df.format(getJSONDouble(line, "speed")) + " MPH ");
					}
					if (line.contains("\"deg\":")) {
						sb.append("\n Wind Direction: " + wf.format(getJSONDouble(line, "deg")) + WeatherWidgetFrame.DEGREES + " ");
					}
					if (line.startsWith("dew_point|")) {
						sb.append("\n Dew Point: " + getWeatherValue(line, "dew_point|") + WeatherWidgetFrame.DEGREES + " ");
					}
					if (line.contains("<dewpoint_f>")) {
						sb.append("\n Dew Point: " + getXMLValue(line, "<dewpoint_f>") + WeatherWidgetFrame.DEGREES + " ");
					}
					if (line.contains("\"feels_like\":")) {
						sb.append("\n Feels Like: " + df.format(getJSONDouble(line, "feels_like")) + WeatherWidgetFrame.DEGREES + " ");
					}
					if (line.startsWith("humidity|")) {
						sb.append("\n Humidity: " + getWeatherValue(line, "humidity|") + " ");
					}
					if (line.contains("<relative_humidity>")) {
						sb.append("\n Humidity: " + getXMLValue(line, "<relative_humidity>") + "% ");
					}
					if (line.contains("\"humidity\":")) {
						sb.append("\n Humidity: " + getJSONDouble(line, "humidity") + "% ");
					}
					if (line.startsWith("pressure|")) {
						sb.append("\n Pressure: " + getWeatherValue(line, "pressure|") + " ");
					}
					if (line.contains("<pressure_in>")) {
						sb.append("\n Pressure: " + getXMLValue(line, "<pressure_in>") + " inches ");
					}
					if (line.contains("\"pressure\":")) {
						sb.append("\n Pressure: " + pf.format((Double.valueOf(getJSONDouble(line, "pressure"))/33.863889532610884)) + " inches ");
					}
					if (line.startsWith("rain|")) {
						sb.append("\n Rainfall: " + getWeatherValue(line, "rain|") + " inches ");
					}
					if (line.startsWith("current_wx|")) {
						sb.append("\n Weather: " + getWeatherValue(line, "current_wx|") + " ");
					}
					if (line.contains("<weather>")) {
						sb.append("\n Weather: " + getXMLValue(line, "<weather>") + " ");
					}
					if (line.startsWith("period_0_weather|")) {
						if (sb.length() > 0) sb.append("\n");
						sb.append("\n" + getWeatherValue(line, "period_0_weather|") + " ");
					}
					if (line.contains("\"main\":")) {
						sb.append("\n Weather: " + getJSONValue(line, "main") + " ");
					}
				}
				in.close();
				frame.setToolTip(sb.toString());
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			try {
				Thread.sleep(60000);
			} catch (InterruptedException ex) {
				System.exit(0);
			}
		}
	}

	private String getWeatherValue(String line, String key) {
		String value = line.substring(key.length());
		int htmlIndex = value.indexOf("&");
		if (htmlIndex >0 && value.endsWith(";")) {
			// Strip off any trailing HTML escape codes
			value = value.substring(0,htmlIndex);
		}
		return value;
	}
	
	public String getXMLValue(String line, String key) {
		int keyIndex = line.indexOf(key);
		if (keyIndex < 0) return line;
		
		int endIndex = line.lastIndexOf("</");
		if (endIndex < 0) return line;
		
		return line.substring(keyIndex+key.length(),endIndex);
	}
	
	public double getJSONDouble(String line, String key) {
		return Double.parseDouble(getJSONValue(line, key));
	}

	public String getJSONValue(String line, String key) {
		if (!key.startsWith("\"")) {
			key = "\"" + key + "\"";
		}
		if (!key.endsWith(":")) {
			key = key + ":";
		}
		int keyIndex = line.indexOf(key);
		if (keyIndex < 0) return line;
		
		int commaIndex = line.indexOf(",",keyIndex+key.length());
		int bracketIndex = line.indexOf("}",keyIndex+key.length());
		
		int endIndex = -1;
		if (commaIndex > 0 && (bracketIndex == -1 || commaIndex < bracketIndex)) {
			endIndex = commaIndex;
		}
		else if (bracketIndex > 0) {
			endIndex = bracketIndex;
		}
		
		line = line.substring(keyIndex+key.length(),endIndex);
		
		if (line.startsWith("\"")) {
			line = line.substring(1);
		}
		if (line.endsWith("\"")) {
			line = line.substring(0, line.length()-1);
		}
		return line;
	}

	public static void main(String[] args) {
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("apple.awt.brushMetalLook", "true");
		System.setProperty("apple.awt.UIElement", "true");	// Removes dock icon since we only need it in the tray
		new WeatherWidget();
	}
}
