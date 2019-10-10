package ui_main_panel;

import java.util.ArrayList;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_browser_dao.HierarchyDAO;
import catalogue_object.Hierarchy;
import i18n_messages.CBMessages;
import property.EditingSupportSimpleProperty;
import property.LabelProviderDCFProperty;
import utilities.GlobalUtil;

/**
 * Editor for hierarchies
 * @author avonva
 *
 */
public class HierarchyEditor extends CatalogueObjectEditor<Hierarchy> {

	private static final String WINDOW_CODE = "HierarchyEditor";
	private Catalogue catalogue;

	public HierarchyEditor(Shell shell, Catalogue catalogue, String title) {
		super(shell, WINDOW_CODE, catalogue.getHierarchies(), title);
		this.catalogue = catalogue;
	}

	@Override
	public void createColumns(TableViewer table) {
		
		// add code column
		TableViewerColumn codeCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Code"), 
				CBMessages.getString("HierarchyEditor.CodeColumn"), 100 );
		codeCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Code" ) ); //$NON-NLS-1$

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

		// add type column
		TableViewerColumn typeCol = GlobalUtil.addStandardColumn(table, new LabelProviderDCFProperty("Applicability"), 
				CBMessages.getString("HierarchyEditor.ApplicabilityColumn"), 100, SWT.CENTER );
		typeCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Applicability" ) ); //$NON-NLS-1$

		// add order column
		TableViewerColumn orderCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Order"), 
				"Order", 50, SWT.CENTER );
		orderCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Order" ) );

		// add status column
		TableViewerColumn statusCol = GlobalUtil.addStandardColumn( table, new LabelProviderDCFProperty("Status"), 
				"Status", 50 );
		statusCol.setEditingSupport( new EditingSupportSimpleProperty( table , "Status" ) );
	}

	@Override
	public Hierarchy createNewObject() {
		// create a default hierarchy and add to the list of hierarchies
		Hierarchy h = Hierarchy.getDefaultHierarchy();
		return h;
	}

	@Override
	public boolean canRemove(Hierarchy obj) {
		
		// check if the selected hierarchy is the master hierarchy
		// if so, return false we cannot remove master
		if ( obj.isMaster() ) {
			GlobalUtil.showErrorDialog( getShell(),
					CBMessages.getString("HierarchyEditor.DeleteHierarchyErrorTitle"), 
					CBMessages.getString("HierarchyEditor.DeleteHierarchyErrorMessage"));
			return false;
		}
		
		return true;
	}

	@Override
	public boolean validateObject(Hierarchy obj) {
		
		// if master but facet hierarchy => error
		if ( obj.isMaster() ) {
			if ( !obj.isBoth() ) {
				GlobalUtil.showErrorDialog( getShell(), 
						CBMessages.getString("HierarchyEditor.FacetErrorTitle"), 
						CBMessages.getString("HierarchyEditor.FacetErrorMessage"));
				return false;
			}
		}

		return true;
	}

	@Override
	public void refresh() {
		catalogue.refreshHierarchies();
		catalogue.refreshApplicabities();
	}
	
	@Override
	public ArrayList<Hierarchy> reset() {
		return catalogue.getHierarchies();
	}

	@Override
	public CatalogueEntityDAO<Hierarchy> getDao() {
		return new HierarchyDAO( catalogue );
	}
}
