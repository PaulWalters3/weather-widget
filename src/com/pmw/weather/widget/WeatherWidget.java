/**
 * Copyright 2015-2023 Paul Walters
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
import java.util.Properties;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.pmw.pmwApplication;

public class WeatherWidget extends pmwApplication {

	private static final String appName = new String("Weather Widget");
	private static final String appVersion = new String("v2023.1");
    private static final String appCopyright = new String("Copyright 2015-2023");
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
				props.put("wxConditionsURL", "https://w1.weather.gov/xml/current_obs/KBWI.xml");
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
					else if (line.contains("<temp_f>")) {
						String temperature = getXMLValue(line, "<temp_f>");
						frame.setIconTemperature(Math.round(Double.parseDouble(temperature)));
						sb.append("\n Temperature: " + temperature + WeatherWidgetFrame.DEGREES + " ");
					}
					else if (line.startsWith("wind_gust|") && line.length() > 10) {
						sb.append("\n Wind Speed: " + getWeatherValue(line, "wind_gust|").substring(6) + " ");
					}
					else if (line.contains("<wind_string>")) {
						sb.append("\n Wind Speed: " + getXMLValue(line, "<wind_string>") + " ");
					}
					else if (line.startsWith("dew_point|")) {
						sb.append("\n Dew Point: " + getWeatherValue(line, "dew_point|") + WeatherWidgetFrame.DEGREES + " ");
					}
					else if (line.contains("<dewpoint_f>")) {
						sb.append("\n Dew Point: " + getXMLValue(line, "<dewpoint_f>") + WeatherWidgetFrame.DEGREES + " ");
					}
					else if (line.startsWith("humidity|")) {
						sb.append("\n Humidity: " + getWeatherValue(line, "humidity|") + " ");
					}
					else if (line.contains("<relative_humidity>")) {
						sb.append("\n Humidity: " + getXMLValue(line, "<relative_humidity>") + "% ");
					}
					else if (line.startsWith("pressure|")) {
						sb.append("\n Pressure: " + getWeatherValue(line, "wind_dir|") + " ");
					}
					else if (line.contains("<pressure_in>")) {
						sb.append("\n Pressure: " + getXMLValue(line, "<pressure_in>") + " inches ");
					}
					else if (line.startsWith("rain|")) {
						sb.append("\n Rainfall: " + getWeatherValue(line, "rain|") + " inches ");
					}
					else if (line.startsWith("current_wx|")) {
						sb.append("\n Weather: " + getWeatherValue(line, "current_wx|") + " ");
					}
					else if (line.contains("<weather>")) {
						sb.append("\n Weather: " + getXMLValue(line, "<weather>") + " ");
					}
					else if (line.startsWith("period_0_weather|")) {
						if (sb.length() > 0) sb.append("\n");
						sb.append("\n" + getWeatherValue(line, "period_0_weather|") + " ");
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

	public static void main(String[] args) {
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("apple.awt.brushMetalLook", "true");
		System.setProperty("apple.awt.UIElement", "true");	// Removes dock icon since we only need it in the tray
		new WeatherWidget();
	}
}
