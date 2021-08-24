package de.fmp.liulab.internal.view;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;

import de.fmp.liulab.task.LoadPTMsTask;
import de.fmp.liulab.task.MainSingleNodeTask;
import de.fmp.liulab.task.ProcessProteinLocationTask;

/**
 * Class responsible for creating JFrames without max nor min buttons
 * 
 * @author borges.diogo
 *
 */
public class JFrameWithoutMaxAndMinButton extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 * @param frame         main frame
	 * @param title         title of frame
	 * @param originalFrame -1:Nothing, 0: MainSingleNode, 1: LoadProteinDomain
	 */
	public JFrameWithoutMaxAndMinButton(final JFrame frame, String title, final int originalFrame) {
		super(frame, title);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent we) {
				super.windowClosing(we);
				if (originalFrame == 0) {
					if (MainSingleNodeTask.cancelProcess())
						frame.dispose();
				} else if (originalFrame == 1) {
					if (ProcessProteinLocationTask.cancelProcess())
						frame.dispose();
				} else if (originalFrame == 2) {
					if (LoadPTMsTask.cancelProcess())
						frame.dispose();
				} else if (originalFrame == 3) {
					if (ProcessProteinLocationTask.cancelProcess())
						frame.dispose();
				} else if (originalFrame == -1) {
					frame.dispose();
				}
			}
		});
	}
}
