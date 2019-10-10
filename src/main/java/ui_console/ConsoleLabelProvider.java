package ui_console;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class ConsoleLabelProvider extends ColumnLabelProvider implements ILabelProvider {
	
	@Override
	public Color getForeground(Object element) {
		
		int colour = SWT.COLOR_WHITE;
		
		if (element instanceof ConsoleMessage)
			colour = ((ConsoleMessage) element).getColor();
		
		return Display.getDefault().getSystemColor(colour);
	}

	@Override
	public void addListener(ILabelProviderListener arg0) {}

	@Override
	public void dispose() {}

	@Override
	public boolean isLabelProperty(Object arg0, String arg1) {
		return false;
	}

	@Override
	public void removeListener(ILabelProviderListener arg0) {}

	@Override
	public Image getImage(Object arg0) {
		return null;
	}
	
	@Override
	public String getText(Object arg0) {
		
		if (arg0 instanceof ConsoleMessage)
			return ((ConsoleMessage) arg0).getMessage();
		
		return null;
	}

}
