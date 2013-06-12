package incubator.exh;

import incubator.dispatch.DispatcherOp;
import incubator.dispatch.LocalDispatcher;
import incubator.pval.Ensure;
import incubator.scb.ScbContainer;
import incubator.scb.ScbContainerListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Class that collects throwables. This class maintains up to a fixed number
 * of throwables.
 */
public class ThrowableCollector implements Serializable,
		ScbContainer<ThrowableContext> {
	/**
	 * Version for serialization.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Default value for the maximum size.
	 */
	private static final int DEFAULT_MAX_SIZE = 20;
	
	/**
	 * The name of the collector.
	 */
	private String m_name;
	
	/**
	 * Maximum number of throwables that can be collected. 
	 */
	private int m_max_size;
	
	/**
	 * The throwables.
	 */
	private LinkedList<ThrowableContext> m_throwables;
	
	/**
	 * The SCB event dispatcher.
	 */
	private transient LocalDispatcher<ScbContainerListener<ThrowableContext>>
			m_dispatcher;
	
	/**
	 * Constructor.
	 * @param name the collector name
	 */
	public ThrowableCollector(String name) {
		Ensure.notNull(name);
		
		m_max_size = DEFAULT_MAX_SIZE;
		m_throwables = new LinkedList<>();
		m_dispatcher = new LocalDispatcher<>();
		m_name = name;
	}
	
	@Override
	public void add_listener(ScbContainerListener<ThrowableContext> l) {
		m_dispatcher.add(l);
	}
	
	@Override
	public void remove_listener(ScbContainerListener<ThrowableContext> l) {
		m_dispatcher.remove(l);
	}
	
	/**
	 * Sets the maximum number of throwables.
	 * @param m the maximum number of throwables
	 */
	public synchronized void max_size(int m) {
		m_max_size = m;
		clear_old();
	}
	
	/**
	 * Obtains all throwables.
	 * @return all throwables
	 */
	public synchronized List<ThrowableContext> throwables() {
		return new ArrayList<>(m_throwables);
	}
	
	/**
	 * Adds a new throwable to the list, discarding older ones if necessary.
	 * @param t the throwable
	 * @param l an optional location for the throwable
	 */
	public synchronized void collect(Throwable t, String l) {
		Ensure.notNull(t);
		ThrowableContext ctx = new ThrowableContext(t, l);
		collect(ctx);
	}
	
	/**
	 * Collects a context. This method is what really performs collection.
	 * @param ctx the context
	 */
	synchronized void collect(final ThrowableContext ctx) {
		m_throwables.addLast(ctx);
		
		m_dispatcher.dispatch(
				new DispatcherOp<ScbContainerListener<ThrowableContext>>() {
			@Override
			public void dispatch(ScbContainerListener<ThrowableContext> l) {
				l.scb_added(ctx);
			}
		});
		
		clear_old();
	}
	
	/**
	 * Removes all old throwables.
	 */
	private synchronized void clear_old() {
		while (m_throwables.size() > m_max_size) {
			final ThrowableContext ctx = m_throwables.getFirst();
			uncollect(ctx);
		}
	}
	
	/**
	 * Removes the throwable context from the list.
	 * @param ctx the context
	 */
	synchronized void uncollect(final ThrowableContext ctx) {
		m_throwables.remove(ctx);
		
		m_dispatcher.dispatch(
				new DispatcherOp<ScbContainerListener<ThrowableContext>>() {
			@Override
			public void dispatch(ScbContainerListener<ThrowableContext> l) {
				l.scb_removed(ctx);
			}
		});
	}
	
	/**
	 * Obtains the name of the collector.
	 * @return the name
	 */
	public String name() {
		return m_name;
	}
	
	@Override
	public synchronized Collection<ThrowableContext> all_scbs() {
		return new ArrayList<>(m_throwables);
	}
}
