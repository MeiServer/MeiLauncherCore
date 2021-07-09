package r3qu13m.mei.launcher.core;

public class MeiLauncherCore {
	private static MeiLauncherCore _instance;

	public static MeiLauncherCore instance() {
		if (MeiLauncherCore._instance == null) {
			MeiLauncherCore._instance = new MeiLauncherCore();
		}
		return MeiLauncherCore._instance;
	}

	private MeiLauncherCore() {

	}

	public void initialize() {
		
	}
}
