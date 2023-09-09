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

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import com.pmw.pmwApplication;

@SuppressWarnings("serial")
public class WeatherWidgetFrame extends JFrame {

	private pmwApplication app = null;
	private ImageIcon icon96 = null;

	public WeatherWidgetFrame(final pmwApplication app) {

		this.app = app;

		icon96 = new ImageIcon(Toolkit.getDefaultToolkit().getImage(WeatherWidgetFrame.class.getResource("/Images/Widget96.png")));
		
		try {
			UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		}
		catch (Exception ignore) {}
		
		createSystemTray();
	}
	
	private boolean isRunning = true;

	public boolean isRunning() {
		return isRunning;
	}

	private SystemTray systemTray;
	private TrayIcon trayIcon;
	
	public void start() {
		Thread t = new Thread() {
			@Override
			public void run() {
				setIconImage(Toolkit.getDefaultToolkit().getImage(WeatherWidgetFrame.class.getResource("/Images/Widget128.png")));
				setTitle(app.getAppName() + " " + app.getAppVersion());
				setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				
				addWindowListener(new WindowAdapter() {
					@Override
					public void windowIconified(WindowEvent event) {
						setVisible(false);	// hide to the system tray
					}
				});

				addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent event) {
						setVisible(false);
					}
				});

				setVisible(false);
			}
		};
		t.start();
	}
	
	public static String DEGREES = "\u00b0";

	public void setIconTemperature(Long temperature) {
		
		BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = image.createGraphics();
		g2d.setColor(Color.LIGHT_GRAY);
		int fontSize = temperature != null && temperature >= 100 ? 16 : 22;
		g2d.setFont(new Font("Helvetica Neue", Font.PLAIN, fontSize));
		if (temperature != null) {
			g2d.drawString(temperature+DEGREES,0,24);
		}
		else {
			g2d.drawString("?"+DEGREES, 0, 24);
		}
		g2d.dispose();

		if (trayIcon == null) {
			trayIcon = new TrayIcon(image);
			trayIcon.setImageAutoSize(true);
		}
		trayIcon.setImage(image);
		if (temperature == null) trayIcon.setToolTip(null);
	}
	
	public void setToolTip(String toolTip) {
		trayIcon.setToolTip(toolTip);
	}

	private void createSystemTray() {
		systemTray = SystemTray.getSystemTray();

		setIconTemperature(null);

        // Popup menu

        PopupMenu systemTrayMenu = new PopupMenu();

		MenuItem mi = new MenuItem("About");
		mi.addActionListener(new AppAction("About"));
		systemTrayMenu.add(mi);

        // systemTrayMenu.addSeparator();

        mi = new MenuItem("Show Weather");
        mi.addActionListener(new AppAction("Show Weather"));
        systemTrayMenu.add(mi);

        systemTrayMenu.addSeparator();

        mi = new MenuItem("Quit");
        mi.addActionListener(new AppAction("Quit " + app.getAppName()));
        systemTrayMenu.add(mi);

        trayIcon.setPopupMenu(systemTrayMenu);
        trayIcon.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent event) {
                	System.out.println("CLICK");
                        if (event.getButton() != MouseEvent.BUTTON1 && event.getClickCount() != 2) return;
                        showWeather();
                }
        });

        try {
                systemTray.add(trayIcon);
        } catch (AWTException e) {
                System.err.println("Could not create tray menu:"+e.getMessage());
        }
	}
	
	private void showWeather() {
		if (Desktop.isDesktopSupported()) {
			try {
				Desktop.getDesktop().browse(new URI(app.getAppProperties().getProperty("showWeatherURL")));
			} catch (Exception ex) {
				System.err.println("Unable to open browser: " + ex.getMessage());
			}
		}
	}

	class AppAction extends AbstractAction
	{
		private static final long serialVersionUID = 1L;
		
		private JPanel component = null;
		
		public AppAction(String name) {
			super(name);
		}
		
		public AppAction(String name, JPanel component) {
			this(name);
			this.component = component;
		}
		
		@Override
		public void actionPerformed(ActionEvent event) {
			
			if (getValue(Action.NAME).equals("Show Weather")) {
				showWeather();
			}

			if (getValue(Action.NAME).equals("Quit " + app.getAppName())) {
				isRunning = false;
				System.exit(0);
			}

			if (getValue(Action.NAME).equals("About")) {
        		String message = app.getAppName() + " " 
						+ app.getAppVersion() + "\n"
						+ app.getAppAuthor() + "\n"
						+ app.getAppCopyright();
               
		       	showMessageDialog(this.component, message, "About " + app.getAppName(), JOptionPane.INFORMATION_MESSAGE, icon96);	
			}
		}
	}
	
	private Map<String,JDialog> dialogSet = new HashMap<String,JDialog>();
	
	private JDialog showMessageDialog(Component parent, String message, String title, int messageType, Icon icon) {
		String key = message + "|" + title + "|" + messageType;
		JDialog d = null;
		if (dialogSet.containsKey(key)) {
			d = dialogSet.get(key);
		}
		else {
			JOptionPane p = new JOptionPane(message, messageType);
			p.setIcon(icon);
			d = p.createDialog(parent, title);
			dialogSet.put(key,d);
		}
				
		d.setAlwaysOnTop(true);
		d.requestFocus();
		d.setVisible(true);
		d.toFront();
		d.setAlwaysOnTop(false);
	
		return d;
	}
}
