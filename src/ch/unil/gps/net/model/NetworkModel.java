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
package ch.unil.gps.net.model;

import java.io.File;

import ch.unil.gps.App;
import edu.mit.magnum.net.Network;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;


/**
 * Represents a network
 */
public class NetworkModel {

	/** Name used for display */
	private StringProperty name;
	
	/** Filename */
	private StringProperty filename;
	/** The file */
	private File file;
	/** Flag showing if file exists */
	private boolean fileExists;
	/** Flag indicates that this is not a network, but a directory in the network collection tree */
	private boolean isGroup;

	/** Notes */
	private StringProperty notes;
	
	/** Directed network */
	private BooleanProperty isDirected;
	/** Weighted network */
	private BooleanProperty isWeighted;
	/** Remove self */
	private BooleanProperty removeSelf;

//	/** Number of regulators */
//	private IntegerProperty numRegulators;
//	/** Number of nodes */
//	private IntegerProperty numNodes;
//	/** Number of edges */
//	private IntegerProperty numEdges;

	
	// ============================================================================
	// PUBLIC METHODS
	    
	/** Constructor with given network */
	public NetworkModel(Network network) {
		initialize(network);
	}

	
	/** Copy constructor */
	public NetworkModel(NetworkModel other) {
		this(other.name.get(), other.filename.get(), other.isDirected.get(), other.isWeighted.get(), other.removeSelf.get());
		file = other.file;
		fileExists = other.fileExists;
		isGroup = other.isGroup;
		notes = new SimpleStringProperty(other.notes.get());
//		numRegulators = new SimpleIntegerProperty(other.numRegulators.get());
//		numNodes = new SimpleIntegerProperty(other.numNodes.get());
//		numEdges = new SimpleIntegerProperty(other.numEdges.get());
	}

	
	/** Constructor initializing only the name (useful for root nodes in tree view) */
	public NetworkModel(String name, String filename, boolean isGroup) {
		this.isGroup = isGroup;
		this.name = new SimpleStringProperty(name);
		this.filename = new SimpleStringProperty(filename);
		this.notes = new SimpleStringProperty();
	}

	
	/** Constructor initializing most fields */
	public NetworkModel(String name, String filename, boolean isDirected, boolean isWeighted, boolean removeSelf) {
		
		this.isGroup = false;
		this.name = new SimpleStringProperty(name);
		this.filename = new SimpleStringProperty(filename);
		this.isDirected = new SimpleBooleanProperty(isDirected);
		this.isWeighted = new SimpleBooleanProperty(isWeighted);
		this.removeSelf = new SimpleBooleanProperty(removeSelf);
		this.notes = new SimpleStringProperty();
	}

	
	/** Constructor from file */
	public NetworkModel(File file, boolean isDirected, boolean isWeighted, boolean removeSelf) {

		this(null, null, isDirected, isWeighted, removeSelf);
		this.name = new SimpleStringProperty(App.mag.utils.extractBasicFilename(file.getName(), false));
		this.filename = new SimpleStringProperty(file.getName());
		this.file = file;
		this.fileExists = file.exists();
	}

	
    // ----------------------------------------------------------------------------

	/** Set network and initialize fields of the model accordingly */
	public void initialize(Network network) {
		
		String name = App.mag.utils.extractBasicFilename(network.getFile().getName(), false);
		this.name = new SimpleStringProperty(name);
		filename = new SimpleStringProperty(network.getFile().getPath());
		
		isWeighted = new SimpleBooleanProperty(network.getIsWeighted());
		isDirected = new SimpleBooleanProperty(network.getIsDirected());
		
//		numRegulators = new SimpleIntegerProperty(network.getNumRegulators());
//		numNodes = new SimpleIntegerProperty(network.getNumNodes());
//		numEdges = new SimpleIntegerProperty(network.getNumEdges());
		notes = new SimpleStringProperty();
	}


    // ----------------------------------------------------------------------------

	/** Initializes the file given the directory and filename */
	public void initFile(File directory) {

		if (filename.get() == null) {
			setFile(directory);
			return;
		}
		
		File file = null;
		if (directory != null)
			file = new File(directory, filename.get());
		setFile(file);
	}
	
		
    // ----------------------------------------------------------------------------

	/** Set file, initialize fileExists and notes with error message */
	public void setFile(File file) {
		
		// The example network is a resource, file is null but we set exists true
		if (name.get().startsWith("Example")) {
			this.file = new File(filename.get());
			fileExists = true;
			notes.set("Click me!");
			return;
		}
		
		this.file = file;
		fileExists = (file != null && file.exists());
		
		// Use notes property to display warning if file does not exist
		if (file != null && !file.exists())
			notes.set("Not installed");
		else
			notes.set(null);
	}
	

	// ============================================================================
	// PRIVATE METHODS

	
	// ============================================================================
	// SETTERS AND GETTERS
	
	public String getName() { return name.getValue(); }
	public File getFile() { return file; }
	public boolean getFileExists() { return fileExists; }
	
	public StringProperty filenameProperty() { return filename; }
	public StringProperty nameProperty() { return name; }	
	public StringProperty notesProperty() { return notes; }
//	public IntegerProperty numRegulatorsProperty() { return numRegulators; }
//	public IntegerProperty numNodesProperty() { return numNodes; }
//	public IntegerProperty numEdgesProperty() { return numEdges; }
	public BooleanProperty isWeightedProperty() { return isWeighted; }
	public BooleanProperty isDirectedProperty() { return isDirected; }


	public boolean getIsDirected() {
		return isDirected.get();
	}


	public boolean getIsWeighted() {
		return isWeighted.get();
	}


	public boolean getRemoveSelf() {
		return removeSelf.get();
	}

}
