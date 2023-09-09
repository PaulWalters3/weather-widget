/**
 * Copyright 2013-2023 Paul Walters
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

package com.pmw;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class pmwApplication {

	private String appName;
	private String appVersion;
    private String appCopyright;
    private String appAuthor;
    private Properties appProperties;
    private File appPropertiesFile;
    private long appPropertiesLastLoaded;
        
    public pmwApplication(String name, String version, String copyright, String author) {
    	this.appName = name;
    	this.appVersion = version;
    	this.appCopyright = copyright;
    	this.appAuthor = author;
    }

    public final String getAppName() { return appName; }

    public final String getAppVersion() { return appVersion; }
    
    public final String getAppAuthor() { return appAuthor; }
    
    public final String getAppCopyright() { return appCopyright; }
    
    public Properties getAppProperties() { return appProperties; }

    public void setAppProperties(Properties props) {
    	this.appProperties = props;
    }
    
    public void loadAppProperties(String propFile) throws IOException {
	    synchronized (this) {
	    	this.appPropertiesFile = new File(propFile);
	    	this.appProperties = new Properties();
	
	    	if (new File(propFile).exists()) {
	    		FileInputStream is = new FileInputStream(propFile);
			  		appProperties.load(is);
			  		is.close();
	    	}
	
			this.appPropertiesLastLoaded = appPropertiesFile.lastModified();
	    }
    }

    public void reloadAppProperties() {
    	reloadAppProperties(false);
    }

    public void reloadAppProperties(boolean force) {
    	if (appPropertiesFile != null && (appPropertiesFile.lastModified() > appPropertiesLastLoaded || force)) {
    		try {
    			loadAppProperties(appPropertiesFile.getAbsolutePath());
    		}
    		catch (Exception ex) {
    			System.err.println("Error reloading properties file: " + ex.getMessage());
    		}
    	}
    }

    public void saveAppProperties() throws IOException {
    	saveAppProperties(this.appPropertiesFile);
    }
    
    public void saveAppProperties(File propFile) throws IOException {
		FileOutputStream os = new FileOutputStream(propFile);
		appProperties.store(os, null);
		os.close();
    }

    private String userHomeDir;
    
    public String getUserHomeDir() {
    	if (userHomeDir != null && !userHomeDir.isEmpty()) return userHomeDir;
    	
    	userHomeDir = System.getenv("HOME");
    	
    	return userHomeDir;
    }

    private String userAppDataDir;
    
    public String getAppDataDir() {
    	
    	if (userAppDataDir != null && !userAppDataDir.isEmpty()) return userAppDataDir;
    	
		if ( System.getProperty("os.name").startsWith("Windows") ) {
			userAppDataDir = System.getenv("APPDATA");
		}
		else if ( System.getProperty("os.name").contains("Mac OS X") ) {
			userAppDataDir = System.getenv("HOME");
			if ( !userAppDataDir.equals("") ) {
				userAppDataDir += "/Library/Application Support";
			}
		}
		if ( !userAppDataDir.equals("") ) {
			userAppDataDir += "/com.pmw/" + appName;
			File f = new File(userAppDataDir);
			if ( !f.exists() ) {
				f.mkdirs();
			}
		}
		return userAppDataDir;
    }
}