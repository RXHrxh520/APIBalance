package asia.rxh.apibalance;

import android.app.Application;

/** 进程启动第一步：把 1.0 时代 ds_ 前缀的旧配置搬到新文件名下，再让各处读取。 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PrefsMigrator.migrate(this);
    }
}
