package edu.cmu.cs.able.eseb.ui.bus;

import javax.swing.JMenu;

import incubator.il.IMutexManager;
import incubator.ui.FrameUtils;
import incubator.ui.MainApplicationFrame;

/**
 * Frame with the main eseb server user interface.
 */
@SuppressWarnings("serial")
public class ServerUiMainFrame extends MainApplicationFrame {
	/**
	 * Creates a new frame.
	 */
	private ServerUiMainFrame() {
		super("eseb server", null);
	}
	
	/**
	 * Creates a new frame.
	 * @return the frame
	 */
	public static ServerUiMainFrame make() {
		ServerUiMainFrame maf = new ServerUiMainFrame();
		
		IMutexManager mm = new IMutexManager("eseb monitor mutex manager");
		
		ScanMutexManagerAction smma = new ScanMutexManagerAction(
				mm, maf);
		smma.bind(maf.get_action_context());
		
		JMenu mutex_menu = new JMenu("Mutex");
		mutex_menu.add(smma.createJMenuItem(false));
		maf.add_menu_after(mutex_menu, "File");
		
		ScanEsebServerAction sesa = new ScanEsebServerAction(mm, maf);
		sesa.bind(maf.get_action_context());
		
		JMenu ebus_menu = new JMenu("Event Bus");
		ebus_menu.add(sesa.createJMenuItem(false));
		maf.add_menu_after(ebus_menu, mutex_menu.getText());
		
		ShowExceptionBrowserAction seba = new ShowExceptionBrowserAction(maf);
		seba.bind(maf.get_action_context());
		
		JMenu tools_menu = new JMenu("Tools");
		tools_menu.add(seba.createJMenuItem(false));
		maf.add_menu_after(tools_menu, ebus_menu.getText());
		
		maf.pack();
		FrameUtils.trackWindowPositioning(maf,
				ServerUiMainFrame.class.getName());
		maf.setVisible(true);
		
		return maf;
	}
}