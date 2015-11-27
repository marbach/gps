/*
Copyright (c) 2013 Daniel Marbach

We release this software open source under an MIT license (see below). If this
software was useful for your scientific work, please cite our paper available at:
http://networkinference.org

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
package ch.unil.gps.view;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.prefs.Preferences;

import ch.unil.gps.*;
import javafx.beans.property.ObjectProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;


/**
 * Abstract class for controllers 
 */
public class ViewController {

	/** The preferences */
	final static public Preferences prefs = Preferences.userNodeForPackage(App.class);

    /** Reference to the main application */
    protected App app;
    /** The root node of this view */
    protected Node root;
    
	
	// ============================================================================
	// PUBLIC METHODS
	    
    /** Constructor */
    public ViewController() {
    	
    	app = App.app;
    }


    // ----------------------------------------------------------------------------

    /** Load the fxml file and initialize the associated controller */
    public static ViewController loadFxml(String location) {
        
        // Load root layout from fxml file.
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(App.class.getResource(location));

        // Load FXML
        Node root = null;
        try {
			root = loader.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
        
        // Initialize controller
        ViewController controller = loader.getController();
        controller.setRoot(root);
        controller.init();
        return controller;
    }

    
	// ----------------------------------------------------------------------------

    /** Open web page in user's default browser */
    public static void openWebpage(String url) {
    	
    	//url.toURI()
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
            	URI uri = new URI(url);
                desktop.browse(uri);
                
            } catch (Exception e) {
                e.printStackTrace();
                // Show error dialog
                showErrorDialog("Sorry, your platform does not support automatic download", 
                		"You have to manually enter the link into your web browser:\n\n" + url,
                		600);
            }
        }
    }
    

    // ----------------------------------------------------------------------------

    /** Show an error dialog */
    public static void showErrorDialog(String header, String content, int width) {
    	
		Alert alert = new Alert(AlertType.ERROR);
		alert.getDialogPane().setPrefWidth(width);
		alert.setTitle("Error");
		alert.setHeaderText(header);
		alert.setContentText(content);
		
		App.app.disableMainWindow(true);
		alert.showAndWait();
		App.app.disableMainWindow(false);
    }

    
    // ----------------------------------------------------------------------------

    /** Open dialog and choose a file */
    public static void chooseFile(ObjectProperty<File> fileProp, String title) {
        
    	// File chooser
    	FileChooser fileChooser = new FileChooser();
    	fileChooser.setTitle(title);
    	
    	// Set initial directory to that of current file
    	if (fileProp.get() != null && fileProp.get().exists())
    		fileChooser.setInitialDirectory(fileProp.get().getParentFile());

    	// Open dialog
    	App.app.disableMainWindow(true);
    	File file = fileChooser.showOpenDialog(App.app.getPrimaryStage());
    	App.app.disableMainWindow(false);
    	
    	// Set file
    	fileProp.set(file);
    }

    
    // ----------------------------------------------------------------------------

    /** Open dialog and choose a directory */
    public static void chooseDir(ObjectProperty<File> dirProp, String title) {
        
    	// Directory chooser
    	DirectoryChooser dirChooser = new DirectoryChooser();
    	dirChooser.setTitle(title);
    	
    	App.app.disableMainWindow(true);
    	File dir = dirChooser.showDialog(App.app.getPrimaryStage());
    	App.app.disableMainWindow(false);
    	
    	// Set directory
    	dirProp.set(dir);
    }

    
	// ============================================================================
	// PROTECTED

    /** 
     * Abstract method can be implemented by subclasses to initialize stuff.
     * Do not call it initialize(), otherwise it gets called by the FXML loader.
     */
    protected void init() {  }
    
    /** Abstract method can be implemented by subclasses to load saved/default preferences */
    public void loadPreferences() {  }
    /** Abstract method can be implemented by subclasses to save preferences */
    public void savePreferences() {  }

    
	// ----------------------------------------------------------------------------

    /** Get a file from a string preference, return null if preference or file doesn't exist */
    protected File getFilePreference(String key) {
    	String filename = prefs.get(key, null);
    	File file = new FileStringConverter().fromString(filename);
    	return file;
    }


	// ----------------------------------------------------------------------------

    /** Save a file to the preferences if it is not null */
    protected void saveFilePreference(String key, File file) {
    	if (file != null)
    		prefs.put(key, file.getAbsolutePath());
    }

    
	// ============================================================================
	// GETTERS AND SETTERS

	public void setRoot(Node root) {
		this.root = root;
	}

	public Node getRoot() {
		return root;
	}


    
}
