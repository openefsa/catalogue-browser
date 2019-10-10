package ui_console;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;

public class Console extends Composite {
	
	private TableViewer table;
	private TableViewerColumn consoleCol;
	
	public Console(Composite parent, int style) {
		super(parent, style);
		createContents();
	}
	
	private void createContents() {
		
		this.setLayout(new FillLayout());
		
		this.table = new TableViewer(this, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL | SWT.HIDE_SELECTION);
		
		table.setContentProvider(new ConsoleContentProvider());
		
		consoleCol = new TableViewerColumn(table, SWT.NONE);
		consoleCol.getColumn().setWidth(500);
		consoleCol.setLabelProvider(new ConsoleLabelProvider());
		consoleCol.getColumn().pack();
	}
	
	public void refresh() {
		this.table.refresh();
	}
	
	/**
	 * Add a new message into the console
	 * @param message
	 */
	public void add(ConsoleMessage... messages) {
		table.add(messages);
		table.reveal(messages[messages.length - 1]);
		
		if (consoleCol != null)
			consoleCol.getColumn().pack();
	}
	
	/**
	 * Add a new message into the console
	 * with a chosen colour
	 * @param message
	 * @param colour
	 */
	public void add(String message, int colour) {
		ConsoleMessage consoleMessage = new ConsoleMessage(message, colour);
		this.add(consoleMessage);
	}
	
	/**
	 * Add a new message into the console
	 * with default colour
	 * @param message
	 */
	public void add(String... messages) {
		
		Stream<ConsoleMessage> consoleMessages = Arrays.stream(messages).map(new Function<String, ConsoleMessage>() {

			@Override
			public ConsoleMessage apply(String arg0) {
				return new ConsoleMessage(arg0);
			}
		});

		this.add(consoleMessages.toArray(ConsoleMessage[]::new));
	}
}
