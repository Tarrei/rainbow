package incubator.rmi.ui;

import incubator.ctxaction.ActionContext;
import incubator.ctxaction.ContextualAction;
import incubator.rmi.RmiScanner;

/**
 * Action that stops a scan.
 */
public class StopScanAction extends ContextualAction {
	/**
	 * Version for serialization.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new action.
	 */
	public StopScanAction() {
		/*
		 * Nothing to do.
		 */
	}

	@Override
	protected boolean isValid(ActionContext context) {
		RmiScanner scanner;
		scanner = (RmiScanner) context.get(RmiScannerIFrame.SCANNER_KEY);
		if (scanner == null) {
			return false;
		}
		
		Integer state;
		state = (Integer) context.get(RmiScannerIFrame.SCANNER_STATUS_KEY);
		if (state == null) {
			return false;
		}
		
		if (state.intValue() == RmiScanner.SCANNING ||
				state.intValue() == RmiScanner.PAUSED) {
			return true;
		}
		
		return false;
	}

	@Override
	public void perform(ActionContext context) {
		RmiScanner scanner;
		scanner = (RmiScanner) context.get(RmiScannerIFrame.SCANNER_KEY);
		scanner.stop();
	}
}
