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
package ch.unil.gps.net.view;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;

import ch.unil.gps.App;
import ch.unil.gps.net.model.NetworkCollection;
import ch.unil.gps.net.model.NetworkModel;
import ch.unil.gps.view.FileStringConverter;
import ch.unil.gps.view.SimpleInfoController;
import ch.unil.gps.view.ViewController;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;
import javafx.scene.control.cell.CheckBoxTreeTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * Controller for "Other networks" pane 
 */
public class NetworkCollectionController extends ViewController {

	/** The network collection */
	private NetworkCollection networkCollection;
	
	/** Files selected using Browse button */
	private List<File> filesToBeAdded;
	/** Disables the selection handle (used when programmatically changing the selection */
	private boolean enableHandleSelection = true;
	/** The selected networks */
	private LinkedHashSet<TreeItem<NetworkModel>> selectedNetworks = new LinkedHashSet<>();
	
	/** Bound to networkDirTextField */
	private ObjectProperty<File> networkDirProperty = new SimpleObjectProperty<File>();
	
	
	// ============================================================================
	// FXML

	/** Network collection directory */
	@FXML
	private TextField networkDirTextField; // bound
	@FXML
	private Button networkDirBrowseButton;
	@FXML
	private Hyperlink networkDirDownloadLink;
	@FXML
	private HBox networkDirHBox;
	
    /** Network table */
    @FXML
    private TreeTableView<NetworkModel> networksTable;
    @FXML
    private TreeTableColumn<NetworkModel, Boolean> directedColumn;
    @FXML
    private TreeTableColumn<NetworkModel, Boolean> weightedColumn;
    @FXML
    private TreeTableColumn<NetworkModel, String> nameColumn;
    @FXML
    private TreeTableColumn<NetworkModel, String> notesColumn;
    @FXML
    private Label numNetworksSelectedLabel;
    @FXML
    private HBox isDirectedHBox;
    @FXML
    private HBox isWeightedHBox;

    /** Load */
    @FXML
    private TextField fileTextField;
    @FXML
    private Button fileBrowseButton;
    @FXML
    private Button addButton;
    @FXML
    private RadioButton directedRadio;
    @FXML
    private RadioButton undirectedRadio;
    @FXML
    private RadioButton weightedRadio;
    @FXML
    private RadioButton unweightedRadio;
    @FXML
    private CheckBox removeSelfCheckBox;
    @FXML
    private VBox contentVBox;
    
	
	// ============================================================================
	// PUBLIC METHODS

    /** Initialize, called after the fxml file has been loaded */
    protected void initialize(NetworkAnalysisController networkAnalysisController) {

    	// TODO arrgh
    	//app.getRootLayoutController();
    	//app.getRootLayoutController().getNetworkAnalysisController();
    	//networkCollection = app.getRootLayoutController().getNetworkAnalysisController().getNetworkCollection();
    	networkCollection = networkAnalysisController.getNetworkCollection();
    	
    	// Initialize the network tree
    	TreeItem<NetworkModel> tree = networkCollection.getNetworkTree();
    	tree.setExpanded(true);
    	// Expand the two main branches
    	tree.getChildren().get(0).setExpanded(true); // My networks
    	tree.getChildren().get(1).setExpanded(true); // Network collection
    	// Add to table
    	networksTable.setRoot(tree);
        networksTable.setShowRoot(false);
        
        // Enable selection of multiple networks 
    	TreeTableViewSelectionModel<NetworkModel> selectionModel = networksTable.getSelectionModel();
    	selectionModel.setSelectionMode(SelectionMode.MULTIPLE);
        // Add selection change listener
    	selectionModel.getSelectedItems().addListener(
        		(ListChangeListener.Change<? extends TreeItem<NetworkModel>> c) -> {
        			handleSelectionChange();
        		});
        
    	// Initialize columns
        // For TableView there's two ways to do it: 
        // (1) Java lambdas, the first one (should look up the details, supposed to be elegant)
        // (2) Create property value factory
        // For strings, both work. For Integers, I only get it to work with (2), for checkboxes only with (1)...
        
        nameColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().nameProperty());
        //numNodesColumn.setCellValueFactory(new PropertyValueFactory<NetworkModel, Integer>("numNodes"));
        //numEdgesColumn.setCellValueFactory(new PropertyValueFactory<NetworkModel, Integer>("numEdges"));

        directedColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().isDirectedProperty());
        // This is the simple way, but shows a checkbox for each row
        //directedColumn.setCellFactory(tc -> new CheckBoxTreeTableCell<>());
        
        // Custom rendering of the table cell
        directedColumn.setCellFactory(column -> { 
        	return new CheckBoxTreeTableCell<NetworkModel, Boolean>() {
        		@Override
        		public void updateItem(Boolean item, boolean empty) {
        			super.updateItem(item, empty);

        			if (item == null || empty)
        				setGraphic(null);
        			//else
        			//	setStyle(...);
        		}
        	};
        });

        weightedColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().isWeightedProperty());
        // Custom rendering of the table cell
        weightedColumn.setCellFactory(column -> { 
        	return new CheckBoxTreeTableCell<NetworkModel, Boolean>() {
        		@Override
        		public void updateItem(Boolean item, boolean empty) {
        			super.updateItem(item, empty);

        			if (item == null || empty)
        				setGraphic(null);
        			//else
        			//	setStyle(...);
        		}
        	};
        });
        
        // isDirected icon -- Now set directly in SceneBuilder
		//InputStream in = App.class.getClassLoader().getResourceAsStream("ch/unil/gps/resources/isDirectedIcon.png");
        //Image icon = new Image(in);
        //isDirectedIcon.setImage(icon);
        
        notesColumn.setCellValueFactory(cellData -> cellData.getValue().getValue().notesProperty());
        
        // Bindings
        Bindings.bindBidirectional(networkDirTextField.textProperty(), networkDirProperty, new FileStringConverter());
        
        // Tooltips
        initTooltips();
    }


    // ----------------------------------------------------------------------------

    /** Load saved/default preferences */
    @Override
    public void loadPreferences() {

        // Initialize network collection directory based on saved setting
        networkDirProperty.setValue(getFilePreference("networkCollectionDir"));
        networkCollection.initDirectory(networkDirProperty.getValue());
        
        // Clear selected networks
    	Platform.runLater(() -> {
    		networksTable.getSelectionModel().clearSelection();
    	});
    }

    
    // ----------------------------------------------------------------------------

    /** Save preferences */
    @Override
    public void savePreferences() {
    	saveFilePreference("networkCollectionDir", networkDirProperty.get());
    }

    
	// ============================================================================
	// HANDLES

    /** Network directory browse button */
    @FXML
    private void handleNetworkDirBrowseButton() {
    	
    	// Choose directory
    	chooseDir(networkDirProperty, "Locate network collection directory");
    	// Initialize network collection
    	networkCollection.initDirectory(networkDirProperty.get());
    }
    	

    // ----------------------------------------------------------------------------

    /** Network directory download link */
    @FXML
    private void handleNetworkDirDownloadLink() {

    	SimpleInfoController.show("net/view/DownloadNetworks.fxml", 
    			"Download networks", 
    			"Installing the network compendium");
    }

    	
    // ----------------------------------------------------------------------------

    /** Browse button */
    @FXML
    private void handleFileBrowseButton() {
        
    	// Open file chooser
    	final FileChooser fileChooser = new FileChooser();
    	fileChooser.setTitle("Select network files");

    	App.app.disableMainWindow(true);
    	filesToBeAdded = fileChooser.showOpenMultipleDialog(app.getPrimaryStage());
    	App.app.disableMainWindow(false);

    	if (filesToBeAdded == null) {
    		fileTextField.setText(null);
    		return;
    	}
    	
    	// Set file text field
    	int numFiles = filesToBeAdded.size();
    	if (numFiles == 0)
    		return;
    	else if (numFiles == 1)
    		fileTextField.setText(filesToBeAdded.get(0).getName());
    	else
    		fileTextField.setText(numFiles + " files selected");
    	
    	// Enable add networks button
    	addButton.setDisable(false);
    }

    
    // ----------------------------------------------------------------------------

    /** Add network button */
    @FXML
    private void handleAddNetworkButton() {
        
    	// Disable add networks button
    	addButton.setDisable(true);
    	// Reset file text field
    	fileTextField.setText(null);
    	
    	if (filesToBeAdded == null)
    		return;
    	
    	// Get relevant options
    	boolean directed = directedRadio.isSelected();
    	boolean weighted = weightedRadio.isSelected();
    	boolean removeSelf = removeSelfCheckBox.isSelected();
    			
    	// The thread responsible for loading the networks
    	//ThreadLoadNetworks threadLoad = new ThreadLoadNetworks(
    	//		networkCollection.getMyNetworks(), 
    	//		filesToBeAdded, directed, weighted, removeSelf);
    	
    	// The thread controller / dialog
    	//ThreadController threadController = new ThreadController(threadLoad);
    	//threadController.start();
    	
    	// Add the network without loading
    	networkCollection.addNetworks(filesToBeAdded, directed, weighted, removeSelf);
    }


    // ----------------------------------------------------------------------------

    /** Called when networks were selected */
    @FXML
    private void handleSelectionChange() {
    	
    	// Return if the handle is disabled
    	if (!enableHandleSelection)
    		return;
    	
    	TreeTableViewSelectionModel<NetworkModel> selectionModel = networksTable.getSelectionModel();
    	ObservableList<Integer> selection = selectionModel.getSelectedIndices();
    	selectedNetworks.clear();
    	if (selection == null)
    		return;

    	for (Integer i : selection) {
    		assert i != null;
    		TreeItem<NetworkModel> item = selectionModel.getModelItem(i);
    		// Sometimes that happens, don't ask me why
    		if (item == null)
    			continue;
    		
    		String name = item.getValue().getName();
    		if (networkCollection.selectionDisabled(name))
    			continue;

    		if (!item.getValue().getFileExists()) {
    			// If no directory has been set
    			if (item.getValue().notesProperty().get() == null) {
    				if (item.isLeaf()) {
    					Alert alert = new Alert(AlertType.WARNING);
    					alert.getDialogPane().setPrefWidth(500);
    					alert.setTitle("Warning");
    					alert.setHeaderText("Network compendium directory not set!");
    					alert.setContentText("Use the \"Browse\" button to locate the \"Network_compendium\" directory.\n\n" + 
    							"If you haven't done so already, download it:\n" +
    							"- click the 'Download' link or\n" +
    							"- visit regulatorycircuits.org");
    					alert.showAndWait();
    					break;
    				} else {
    					// Expand
    					Platform.runLater(() -> item.setExpanded(true));
    					break;
    				}

    				// Directory was set but the file / directory was not found
    			} else {
    				Alert alert = new Alert(AlertType.WARNING);
    				alert.getDialogPane().setPrefWidth(600);
    				alert.setTitle("Warning");

    				// Get info on the file / dir
    				File file = item.getValue().getFile();
    				String fileOrDir = "File";
    				if (file.isDirectory())
    					fileOrDir = "Directory";

    				// Set text
    				alert.setHeaderText(fileOrDir + " not found!");
    				alert.setContentText(fileOrDir + " not found in the \"Network_compendium\" directory:\n" + 
    						file.getAbsolutePath() + "\n\n" +
    						"- Click the \"Download\" link for instructions how to install the networks\n" +
    						"- Note that the 394 individual networks need to be downloaded separately\n" +
    						"- Verify that the \"Network_compendium\" directory was selected");

    				// Show the dialog
    				alert.showAndWait();
    				break;
    			}
    		}
    		
    		// Add leafs
    		if (item.isLeaf()) {
    			selectedNetworks.add(item);
    			
   			// Add all children
    		} else {
    			// Expand
    			if (!item.isExpanded())
    				Platform.runLater(() -> item.setExpanded(true));
    			// Add kids
    			for (TreeItem<NetworkModel> child : item.getChildren()) {
    				if (!child.isLeaf())
    					throw new RuntimeException("Did not except nested categories in network tree: " + child.getValue().getName());
    				if (child.getValue().getFileExists())
    					selectedNetworks.add(child);
    			}
    		}
    	}
    	
    	// Perform selection, set texts, update EnrichmentController
    	Platform.runLater(() -> {
        	// Disable the handle for updates made below to avoid recursion
    		enableHandleSelection = false;
    		selectionModel.clearSelection();
    		for (TreeItem<NetworkModel> item : selectedNetworks)
    			selectionModel.select(item);
    		enableHandleSelection = true;
    		
    		if (selectedNetworks.size() == 0)
    			numNetworksSelectedLabel.setText("No networks selected");
    		else if (selectedNetworks.size() == 1)
    			numNetworksSelectedLabel.setText("1 network selected");
    		else
    			numNetworksSelectedLabel.setText(selectedNetworks.size() + " networks selected");
    	});
    	
		// Let the enrichment controller know
    	// TODO arrgh
		app.getRootLayoutController().getNetworkAnalysisController().getEnrichmentController().networkSelectionUpdated();
    }


	// ============================================================================
	// PRIVATE METHODS

    /** Add tooltips */
	private void initTooltips() {
		
		Tooltip tip = new Tooltip("The \"Network_compendium\" directory: click the\n"
				+ "\"Download\" link for instructions how to install");
		Tooltip.install(networkDirHBox, tip);
		networkDirBrowseButton.setTooltip(tip);

		tip = new Tooltip("Select one or multiple\n"
				+ "networks in the table");
		//networksTable.setTooltip(tip);
		numNetworksSelectedLabel.setTooltip(tip);
		
		Tooltip.install(isDirectedHBox, new Tooltip("Directed networks"));
		Tooltip.install(isWeightedHBox, new Tooltip("Weighted networks"));
		
		tip = new Tooltip("Choose your own networks to add");
		fileTextField.setTooltip(tip);
		fileBrowseButton.setTooltip(tip);
		addButton.setTooltip(new Tooltip("Add the chosen networks: they appear\n"
				                       + "under \"My networks\" in the table"));
		
		tip = new Tooltip("Specify if your networks are directed");
		directedRadio.setTooltip(tip);
		undirectedRadio.setTooltip(tip);
		
		tip = new Tooltip("Specify if your networks are weighted");
		weightedRadio.setTooltip(tip);
		unweightedRadio.setTooltip(tip);
		removeSelfCheckBox.setTooltip(new Tooltip("Specify if self-loops should be\nremoved from your network"));
	}
    
	
	// ============================================================================
	// SETTERS AND GETTERS

	public LinkedHashSet<TreeItem<NetworkModel>> getSelectedNetworks() {
		return selectedNetworks;
	}

	  
}
