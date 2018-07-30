package io.skypvp.mlg.sql;

import java.security.InvalidParameterException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.EmptyStackException;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.PooledConnection;

public class MiniConnectionPoolManager {
	private ConnectionPoolDataSource dataSource;
	private int maxConnections;
	private int maxIdleConnectionLife;
	private int timeout;
	private Semaphore semaphore;
	private Queue<PCTS> recycledConnections;
	private int activeConnections;
	private PoolConnectionEventListener poolConnectionEventListener;
	private boolean isDisposed;
	private Timer timer;

	private class PCTS implements Comparable<PCTS> {
		private PooledConnection pconn;
		private Calendar timeStamp;

		private PooledConnection getPConn() {
			return this.pconn;
		}

		private Calendar getTimeStamp() {
			return this.timeStamp;
		}

		private PCTS(PooledConnection pconn) {
			this.timeStamp = Calendar.getInstance();
			this.pconn = pconn;
		}

		public int compareTo(PCTS other) {
			return (int) (other.getTimeStamp().getTimeInMillis() - getTimeStamp().getTimeInMillis());
		}
	}

	private class PoolConnectionEventListener implements ConnectionEventListener {
		private PoolConnectionEventListener() {
		}

		public void connectionClosed(ConnectionEvent event) {
			PooledConnection pconn = (PooledConnection) event.getSource();
			pconn.removeConnectionEventListener(this);
			MiniConnectionPoolManager.this.recycleConnection(pconn);
		}

		public void connectionErrorOccurred(ConnectionEvent event) {
			PooledConnection pconn = (PooledConnection) event.getSource();
			pconn.removeConnectionEventListener(this);
			MiniConnectionPoolManager.this.disposeConnection(pconn);
		}
	}

	private class ConnectionMonitor extends TimerTask {
		private MiniConnectionPoolManager owner;

		private ConnectionMonitor(MiniConnectionPoolManager owner) {
			this.owner = owner;
		}

		@Override
		public void run() {
			Calendar now = Calendar.getInstance();
			synchronized (owner) {
				Iterator<PCTS> iterator = recycledConnections.iterator();
				while (iterator.hasNext()) {
					PCTS pcts = iterator.next();
					int delta = (int) ((now.getTimeInMillis() - pcts.getTimeStamp().getTimeInMillis()) / 1000);
					if (delta >= maxIdleConnectionLife) {
						closeConnectionNoEx(pcts.getPConn());
						iterator.remove();
					}
				}
			}
		}
	}

	public static class TimeoutException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public TimeoutException() {
			super();
		}
	}

	public MiniConnectionPoolManager(ConnectionPoolDataSource dataSource, int maxConnections) {
		this(dataSource, maxConnections, 60, 60);
	}

	public MiniConnectionPoolManager(ConnectionPoolDataSource dataSource, int maxConnections, int timeout,
			int maxIdleConnectionLife) {
		if (dataSource == null) {
			throw new InvalidParameterException("dataSource cant be null");
		}
		if (maxConnections < 1) {
			throw new InvalidParameterException("maxConnections must be > 1");
		}
		if (timeout < 1) {
			throw new InvalidParameterException("timeout must be > 1");
		}
		if (maxIdleConnectionLife < 1) {
			throw new InvalidParameterException("maxIdleConnectionLife must be > 1");
		}
		this.dataSource = dataSource;
		this.maxConnections = maxConnections;
		this.maxIdleConnectionLife = maxIdleConnectionLife;
		this.timeout = timeout;
		this.semaphore = new Semaphore(maxConnections, true);
		this.recycledConnections = new PriorityQueue<PCTS>();
		this.poolConnectionEventListener = new PoolConnectionEventListener();

		this.timer = new Timer(getClass().getSimpleName(), true);
		this.timer.schedule(new ConnectionMonitor(this), this.maxIdleConnectionLife, this.maxIdleConnectionLife);
	}

	private void assertInnerState() {
		if (this.activeConnections < 0) {
			throw new AssertionError();
		}
		if (this.activeConnections + this.recycledConnections.size() > this.maxConnections) {
			throw new AssertionError();
		}
		if (this.activeConnections + this.semaphore.availablePermits() > this.maxConnections) {
			throw new AssertionError();
		}
	}

	private void closeConnectionNoEx(PooledConnection pconn) {
		try {
			pconn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public synchronized void dispose() throws SQLException {
		if (this.isDisposed) {
			return;
		}
		this.isDisposed = true;
		SQLException e = null;
		while (!this.recycledConnections.isEmpty()) {
			PCTS pcts = (PCTS) this.recycledConnections.poll();
			if (pcts == null) {
				throw new EmptyStackException();
			}
			PooledConnection pconn = pcts.getPConn();
			try {
				pconn.close();
			} catch (SQLException e2) {
				if (e == null) {
					e = e2;
				}
			}
		}
		if (timer != null)
			this.timer.cancel();
		if (e != null) {
			throw e;
		}
	}

	private synchronized void disposeConnection(PooledConnection pconn) {
		if (this.activeConnections < 0) {
			throw new AssertionError();
		}
		this.activeConnections -= 1;
		this.semaphore.release();
		closeConnectionNoEx(pconn);
		assertInnerState();
	}

	public synchronized int getActiveConnections() {
		return this.activeConnections;
	}

	public Connection getConnection() throws SQLException {
		synchronized (this) {
			if (this.isDisposed) {
				throw new IllegalStateException("Connection pool has been disposed.");
			}
		}
		try {
			if (!this.semaphore.tryAcquire(this.timeout, TimeUnit.SECONDS)) {
				throw new TimeoutException();
			}
		} catch (InterruptedException e) {
			throw new RuntimeException("Interrupted while waiting for a database connection.", e);
		}
		boolean ok = false;
		try {
			Connection conn = getConnection2();
			ok = true;
			return conn;
		} finally {
			if (!ok) {
				this.semaphore.release();
			}
		}
	}

	private synchronized Connection getConnection2() throws SQLException {
		if (this.isDisposed) {
			throw new IllegalStateException("Connection pool has been disposed.");
		}
		PooledConnection pconn;
		if (this.recycledConnections.size() > 0) {
			PCTS pcts = (PCTS) this.recycledConnections.poll();
			if (pcts == null) {
				throw new EmptyStackException();
			}
			pconn = pcts.getPConn();
		} else {
			pconn = this.dataSource.getPooledConnection();
		}
		Connection conn = pconn.getConnection();
		this.activeConnections += 1;
		pconn.addConnectionEventListener(this.poolConnectionEventListener);
		assertInnerState();
		return conn;
	}

	private synchronized void recycleConnection(PooledConnection pconn) {
		if (this.isDisposed) {
			disposeConnection(pconn);
			return;
		}
		if (this.activeConnections <= 0) {
			throw new AssertionError();
		}
		this.activeConnections -= 1;
		this.semaphore.release();
		this.recycledConnections.add(new PCTS(pconn));
		assertInnerState();
	}
}
