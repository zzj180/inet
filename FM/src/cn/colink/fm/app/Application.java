package cn.colink.fm.app;

public class Application extends android.app.Application {
    
	public static Application instance;

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;
		CrashHandler crashHandler = CrashHandler.getInstance();
		crashHandler.init(getApplicationContext());

	}

	public static Application getInstance() {
		if (null == instance) {
			instance = new Application();
		}
		return instance;
	}
	
}
