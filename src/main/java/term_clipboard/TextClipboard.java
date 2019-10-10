package term_clipboard;

import java.util.Arrays;
import java.util.Iterator;

import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Display;

/**
 * Simple clipboard to copy text in the OS clipboard
 * @author avonva
 *
 */
public class TextClipboard implements IClipboard<String> {
	
	private Display display;
	
	public TextClipboard(Display display) {
		this.display = display;
	}
	
	@Override
	public void copy(String source) {
		this.copy(Arrays.asList(source));
	}

	@Override
	public void copy(Iterable<String> sources) {
		
		StringBuilder strings = new StringBuilder();
		
		Iterator<String> iterator = sources.iterator();
		
		// copy each source in a new line
		while(iterator.hasNext()) {
			
			strings.append(iterator.next());
			
			if (iterator.hasNext())
				strings.append( "\n" );
		}
		
		Clipboard clipboard = new Clipboard(display);
		clipboard.setContents(new Object[] {strings.toString()}, 
				new Transfer[] {TextTransfer.getInstance()});
		clipboard.dispose();
	}
}
