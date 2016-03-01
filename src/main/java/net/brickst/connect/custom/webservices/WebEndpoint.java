/*
 * Storage Class for Web Endpoint Configuration
 * 
 * Copyright (c) 2016 Brick Street Software, Inc.
 * 
 * This code is provided under the Apache License.
 * http://www.apache.org/licenses/
 */

package net.brickst.connect.custom.webservices;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Contains configuration information and helper code for "web endpoints"
 */
public abstract class WebEndpoint {
	public enum EndpointType {
		JMS, REST, LOG, CUSTOM
	}

	//
	// TOP LEVEL RETRY DIR (STATIC)
	//
	private static File topLevelRetryDir;
	private static Timer retryTimer;

	// start retry timer thread when loaded
	static {
		retryTimer = new Timer("WebEndpoint Delivery Retry", true);
	}
	
	//
	// INSTANCE VARS
	//

	protected EndpointType endpointType;
	protected File retryDir;
	protected long retryIntervalMS;

	// retry timer task
	protected Object retryTimerLock = new Object();
	protected RetryTimerTask retryTimerTask;
	
	//
	// GETTERS and SETTERS
	//

	// static retry dir
	public static File getTopLevelRetryDir() {
		return topLevelRetryDir;
	}

	public static void setTopLevelRetryDir(File value) {
		if (!value.isDirectory()) {
			// mkdir ?
			if (!value.exists()) {
				// try mkdir
				boolean didCreate = false;
				try {
					didCreate = value.mkdirs();
				} catch (Exception x) {
					// TODO LOG EXCEPTION
				}
				if (!value.isDirectory()) {
					throw new IllegalArgumentException(
							"Unable to create retry directory: "
									+ value.getAbsolutePath());
				}
			} else {
				// exists and is not directory == invalid
				throw new IllegalArgumentException("Invalid retry directory: "
						+ value.getAbsolutePath());
			}
		}
		topLevelRetryDir = value;
	}

	// retry timer object
	public static Timer getRetryTimer() { return retryTimer; }

	// EndpointType
	public EndpointType getEndpointType() {
		return endpointType;
	}

	public void setEndpointType(EndpointType value) {
		endpointType = value;
	}

	// RetryIntervalMS
	public long getRetryIntervalMS() {
		return retryIntervalMS;
	}

	public void setRetryIntervalMS(long value) {
		retryIntervalMS = value;
	}

	// RetryDir
	public File getRetryDir() {
		return retryDir;
	}

	public void setRetryDir(File value) {
		if (!value.isDirectory()) {
			// mkdir ?
			if (!value.exists()) {
				// try mkdir
				boolean didCreate = false;
				try {
					didCreate = value.mkdirs();
				} catch (Exception x) {
					// TODO LOG EXCEPTION
				}
				if (!value.isDirectory()) {
					throw new IllegalArgumentException(
							"Unable to create retry directory: "
									+ value.getAbsolutePath());
				}
			} else {
				// exists and is not directory == invalid
				throw new IllegalArgumentException("Invalid retry directory: "
						+ value.getAbsolutePath());
			}
		}
		retryDir = value;
	}

	// 
	//
	// CONFIG FROM PROPERTIES METHODS
	//
	public abstract void initFromProperties(Properties props, String prefix);
	
	//
	// init network resources
	//
    public abstract void initNetworkResources();

	//
	// DELIVER MESSAGE
	//
	public abstract void deliverMessage(String content);
	
	//
	// RETRY LOGIC
	//

	// TimerTask Inner Class
	public class RetryTimerTask extends TimerTask {
		WebEndpoint wep;

		public RetryTimerTask(WebEndpoint wep) {
			this.wep = wep;
		}

		public void run() {
			wep.doRetry();
		}
	}

	public void startRetryTask()
	{
        // schedule retry
        synchronized (retryTimerLock) {
            if (retryTimerTask == null) {
                retryTimerTask = new RetryTimerTask(this);
                retryTimer.schedule(retryTimerTask, retryIntervalMS,
                        retryIntervalMS);
            }
        }	    
	}
	
	public void stopRetryTask()
	{
	    synchronized (retryTimerLock) {
	        if (retryTimerTask != null) {
	            retryTimerTask.cancel();
	            retryTimerTask = null;
	        }
	    }
	}
	
	/**
	 * Saves a message for future retry
	 */
	public void scheduleRetry(String content) throws IOException {
		// assume this will generate unique filenames based on current timestamp
		File tempfile = null;
		File realfile = null;

		while (true) {
			long now = System.currentTimeMillis();
			String tempFilename = ".msg" + Long.toString(now) + ".txt";
			String filename = "msg" + Long.toString(now) + ".txt";
			tempfile = new File(getRetryDir(), tempFilename);
			realfile = new File(getRetryDir(), filename);
			if (!tempfile.exists() && !realfile.exists()) {
				break;
			}

			// filename collision; wait and retry
			double randomWait = Math.random() * 1000; // 0 - 1000 milliseconds
			try {
				Thread.sleep((long) randomWait);
			} catch (InterruptedException ix) {
				// don't care
			}
		}

		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		try {
			fos = new FileOutputStream(tempfile);
			osw = new OutputStreamWriter(fos, "UTF-8");
			osw.write(content);
		} finally {
			if (osw != null) {
				osw.close();
			}
			if (fos != null) {
				fos.close();
			}
		}

		// rename file so that retry can see it
		tempfile.renameTo(realfile);

		// ensure retry task is started
		startRetryTask();
	}

	private String readFileContents(File file) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		FileInputStream fis = null;

		try {
			fis = new FileInputStream(file);
			byte[] buf = new byte[512];

			while (true) {
				int cc = fis.read(buf);
				if (cc == -1) {
					break;
				}
				baos.write(buf, 0, cc);
			}
		} finally {
			fis.close();
		}
		return baos.toString();
	}

	public void doRetry() {
		// get directory list
		File[] files = retryDir.listFiles();
		Arrays.sort(files);

		int filecount = files.length;
		if (filecount == 0) {
		    return;
		}
		
		for (int i = 0; i < filecount; i++) {
			File retryFile = files[i];

			// check name
			String fname = retryFile.getName();
			if (!fname.startsWith("msg")) {
				continue;
			}

			// read file contents
			String content = null;

			if (!retryFile.exists()) {
				// skip non-existent file
				continue;
			}

			try {
				content = readFileContents(retryFile);
			} catch (Exception x) {
				// TODO LOG EXCEPTION
				// skip this file
				continue;
			}
			if (content == null || content.trim().length() == 0) {
				// TODO LOG ??
				continue;
			}

			// got content
			try {
				deliverMessage(content);
			} catch (Exception x) {
			    // Note: the delivery attempt can fail here.
			    // If we get a failure, we do nothing since the 
			    // message is already persisted in the retry directory.
			    
				// TODO LOG EXCEPTION
				continue;
			}

			// message delivered
			boolean didDelete = retryFile.delete();
			if (!didDelete) {
				// TODO LOG WEIRDNESS
			}
		}
	}
}
