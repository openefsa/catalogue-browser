package ui_main_panel;

import java.util.ArrayList;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue_browser_dao.AttributeDAO;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_object.Attribute;
import i18n_messages.CBMessages;
import property.EditingSupportSimpleProperty;
import property.LabelProviderDCFProperty;
import utilities.GlobalUtil;

/**
 * Editor for the catalogue attributes
 * @author avonva
 *
 */
public class AttributeEditor extends CatalogueObjectEditor<Attribute> {

	private static final String WINDOW_CODE = "AttributeEditor";
	private Catalogue catalogue;

	public AttributeEditor(Shell shell, Catalogue catalogue, String title) {
		super(shell, WINDOW_CODE, catalogue.getAttributes(), title);
		this.catalogue = catalogue;
	}

	@Override
	public void createColumns(TableViewer table) {
		
		// add code column
		TableViewerColumn codeCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Code"), 
				CBMessages.getString("HierarchyEditor.CodeColumn"), 100 );
		codeCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Code" ) );

		// add name column
		TableViewerColumn nameCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Name"), 
				CBMessages.getString("HierarchyEditor.NameColumn") );
		nameCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Name" ) );

		// add label column
		TableViewerColumn labelCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Label"), 
				"Label" );
		labelCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Label" ) );

		// add scopenotes column
		TableViewerColumn noteCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Scopenotes"), 
				"Scopenotes" );
		noteCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Scopenotes" ) );

		// add Reportable column
		TableViewerColumn reportableCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Reportable"), 
				"Reportable", 80 );
		reportableCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Reportable" ) );

		// add Visible column
		TableViewerColumn visibleCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Visible"), 
				"Visible", 80 );
		visibleCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Visible" ) );

		// add Searchable column
		TableViewerColumn searchableCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Searchable"), 
				"Searchable", 80 );
		searchableCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Searchable" ) );

		// add order column
		TableViewerColumn orderCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Order"), 
				"Order", 50, SWT.CENTER );
		orderCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Order" ) );

		// add type column
		TableViewerColumn typeCol = GlobalUtil.addStandardColumn(table, new LabelProviderDCFProperty("Type"), 
				CBMessages.getString("HierarchyEditor.TypeColumn"), 100, SWT.CENTER );
		typeCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Type" ) );

		// add Maxlength column
		TableViewerColumn lengthCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Maxlength"), 
				"Max Length", 50, SWT.CENTER );
		lengthCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Maxlength" ) );

		// add Precision column
		TableViewerColumn precisionCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Precision"), 
				"Precision", 50, SWT.CENTER );
		precisionCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Precision" ) );

		// add Scale column
		TableViewerColumn scaleCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Scale"), 
				"Scale", 50, SWT.CENTER );
		scaleCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Scale" ) );

		// add Catalogue Code column
		TableViewerColumn catCodeCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("catcode"), 
				"Catalogue Code", 100, SWT.CENTER );
		catCodeCol.setEditingSupport( new EditingSupportSimpleProperty( table , "catcode" ) );

		// add single_repeatable column
		TableViewerColumn singRepCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("single_repeatable"), 
				"Single/Repeatable", 100, SWT.CENTER );
		singRepCol.setEditingSupport( new EditingSupportSimpleProperty( table , "single_repeatable" ) );

		// add Inheritance column
		TableViewerColumn inheritanceCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Inheritance"), 
				"Inheritance", 80, SWT.CENTER );
		inheritanceCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Inheritance" ) );

		// add Uniqueness column
		TableViewerColumn uniqueCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Uniqueness"), 
				"Uniqueness", 80, SWT.CENTER );
		uniqueCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Uniqueness" ) );		

		// add termcodealias column
		TableViewerColumn aliasCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("termcodealias"), 
				"Term Code Alias", 80, SWT.CENTER );
		aliasCol.setEditingSupport( new EditingSupportSimpleProperty( table , "termcodealias" ) );	
	}

	@Override
	public Attribute createNewObject() {
		Attribute attr = Attribute.getDefaultAttribute();
		return attr;
	}

	@Override
	public boolean canRemove(Attribute obj) {
		return true;
	}

	@Override
	public boolean validateObject(Attribute obj) {
		return true;
	}

	@Override
	public void refresh() {
		// update the terms attributes also in RAM
		catalogue.refreshAttributes();
		catalogue.refreshTermAttributes();
	}

	@Override
	public ArrayList<Attribute> reset() {
		return catalogue.getAttributes();
	}

	@Override
	public CatalogueEntityDAO<Attribute> getDao() {
		return new AttributeDAO( catalogue );
	}

}
