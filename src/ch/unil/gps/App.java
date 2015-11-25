/*
Copyright (c) 2013-2015 Daniel Marbach

We release this software open source under an MIT license (see below). If this
software was useful for your scientific work, please cite our paper available at:
http://regulatorycircuits.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
 */
package ch.unil.gps;

import java.io.File;
import java.util.prefs.BackingStoreException;

import ch.unil.gps.view.*;
import edu.mit.magnum.Magnum;
import edu.mit.magnum.MagnumSettings;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


/**
 * The main class starting the JavaFX app
 * 
 */
public class App extends Application {

	/** For convenience, a magnum reference -- do not use in threads, they need their own one! */
	public static Magnum mag; 
	/** Reference to unique instance of app (singleton design pattern) */
	public static App app;
	/** The logger (also set for mag) */
	public static AppLogger log;
	
	/** The main stage */
    private Stage primaryStage;
    
    /** Root layout controller */
    private RootLayoutController rootLayoutController;
    /** "Settings" controller */
    private PreferencesDialogController preferencesController;
        

	// ============================================================================
	// STATIC METHODS

	/** Main */
	public static void main(String[] args) {

		try {
			// The logger
			log = new AppLogger();
			log.keepLogCopy();
			File logFile = new File(System.getProperty("user.home"), ".gps_log.txt");
			log.createLogFile(logFile);
			// Say hello
			log.println(AppSettings.gpsVersion);

			// Initialize magnum with our logger
			mag = new Magnum(args, log);

			// Calls start()
			launch(args);

			// Save settings
			log.println("Saving preferences...");
			app.savePreferences();
			log.println("Bye!");
			
		} catch (Throwable e) {
			log.setConsole(null);
			log.printStackTrace(e);

		} finally {
			log.closeLogFile();
		}
	}

		
	// ============================================================================
	// PUBLIC METHODS

	/** Constructor */
	public App() {
			
		if (app != null)
			throw new RuntimeException("There should be only one instance of App");
		else
			app = this;			
	}
	
	
	// ----------------------------------------------------------------------------

	/** Called when the App is launched */
	@Override
	public void start(Stage primaryStage) {

        this.primaryStage = primaryStage;
        this.primaryStage.setTitle(AppSettings.gpsVersion);

        // Set icons
        //primaryStage.getIcons().add(new Image(App.class.getResourceAsStream("resources/icons/magnum-logo-small.png"))); 
        
        // Load preferences controller -- needs to be done first because of rememberSettings
    	preferencesController = (PreferencesDialogController) ViewController.loadFxml("view/PreferencesDialog.fxml");
    	
    	// The root layout, rootLayoutControllers also initializes also the analysis controllers
        initRootLayout();
        
        // Load saved/default settings
        loadPreferences();
	}
	

	// ----------------------------------------------------------------------------
	
    /** Initialize the controls with saved/default settings */
    public void loadPreferences() {
    	
    	try {
    		preferencesController.loadPreferences();
    		rootLayoutController.loadPreferences();
    		
    	} catch (Exception e) {
    		// Corrupted preferences, clear them
    		try {
    			log.warning("Failed to load preferences");
    			log.printStackTrace(e);
    			log.println("Clearing corrupted preferences...");
				ViewController.prefs.clear();
			} catch (BackingStoreException e1) {
				log.warning("Failed to clear prefences");
				throw new RuntimeException(e1);
			}
    	}
    }

    
	// ----------------------------------------------------------------------------
	
    /** Initialize the controls with saved/default settings */
    public void savePreferences() {
    	preferencesController.savePreferences();
    	rootLayoutController.savePreferences();
    }
    
    
	// ----------------------------------------------------------------------------
	
    /** Apply settings from the given magnum settings instance */
    public void applySettings(MagnumSettings set) {
    	rootLayoutController.applySettings(set);
    }

    
	// ----------------------------------------------------------------------------
	
    /** Disable the main window / stage */
    public void disableMainWindow(boolean value) {
    	rootLayoutController.getRoot().setDisable(value);
    }
    
    
	// ----------------------------------------------------------------------------
	
    /** Show an error dialog */
    public void showErrorDialog(String header, String content, int width) {
    	
		Alert alert = new Alert(AlertType.ERROR);
		alert.getDialogPane().setPrefWidth(width);
		alert.setTitle("Error");
		alert.setHeaderText(header);
		alert.setContentText(content);
		
		disableMainWindow(true);
		alert.showAndWait();
		disableMainWindow(false);
    }



	// ============================================================================
	// PRIVATE METHODS

    /** Initializes the root layout */
    private void initRootLayout() {
    	
    	rootLayoutController = (RootLayoutController) ViewController.loadFxml("view/RootLayout.fxml");
    	rootLayoutController.show();
        
    	// Show the scene containing the root layout.
    	Scene scene = new Scene(rootLayoutController.getRoot());
    	primaryStage.setScene(scene);
    	primaryStage.show();
    }

    

    

    
	// ============================================================================
	// SETTERS AND GETTERS

    public Stage getPrimaryStage() { return primaryStage; }
    public BorderPane getRootLayout() { return rootLayoutController.getRoot(); }
    
    public PreferencesDialogController getPreferencesController() { return preferencesController; }
    public RootLayoutController getRootLayoutController() { return rootLayoutController; }
    
}
