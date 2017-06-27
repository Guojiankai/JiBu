package android.jibu.com.jibu;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

public class BindService extends Service implements SensorEventListener {
    /**
     * binder服务与activity交互桥梁
     */
    private LcBinder lcBinder = new LcBinder();
    /**
     * 当前步数
     */
    private int nowBuSu = 0;
    /**
     * 传感器管理对象
     */
    private SensorManager sensorManager;
    /**
     * 加速度传感器中获取的步数
     */
    private StepCount mStepCount;

    /**
     * 数据回调接口，通知上层调用者数据刷新
     */
    private UpdateUiCallBack mCallback;

    /**
     * 计步传感器类型  Sensor.TYPE_STEP_COUNTER或者Sensor.TYPE_STEP_DETECTOR
     */
    private static int stepSensorType = -1;
    /**
     * 每次第一次启动记步服务时是否从系统中获取了已有的步数记录
     */
    private boolean hasRecord = false;
    /**
     * 系统中获取到的已有的步数
     */
    private int hasStepCount = 0;
    /**
     * 上一次的步数
     */
    private int previousStepCount = 0;
    /**
     * 构造函数
     */
    public BindService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("BindService—onCreate", "开启计步");
        new Thread(new Runnable() {
            @Override
            public void run() {
                startStepDetector();
                Log.i("BindService—子线程", "startStepDetector()");
            }
        }).start();
    }

    /**
     * 选择计步数据采集的传感器
     * SDK大于等于19，开启计步传感器，小于开启加速度传感器
     */
    private void startStepDetector() {
        if (sensorManager != null) {
            sensorManager = null;
        }
        //获取传感器管理类
        sensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        int versionCodes = Build.VERSION.SDK_INT;//取得SDK版本
        if (versionCodes >= 19) {
            //SDK版本大于等于19开启计步传感器
            addCountStepListener();
        } else {        //小于就使用加速度传感器
            addBasePedometerListener();
        }
    }

    /**
     * 启动计步传感器计步
     */
    private void addCountStepListener() {
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor detectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (countSensor != null) {
            stepSensorType = Sensor.TYPE_STEP_COUNTER;
            sensorManager.registerListener(BindService.this, countSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.i("计步传感器类型", "Sensor.TYPE_STEP_COUNTER");
        } else if (detectorSensor != null) {
            stepSensorType = Sensor.TYPE_STEP_DETECTOR;
            sensorManager.registerListener(BindService.this, detectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            addBasePedometerListener();
        }
    }

    /**
     * 启动加速度传感器计步
     */
    private void addBasePedometerListener() {
        Log.i("BindService", "加速度传感器");
        mStepCount = new StepCount();
        mStepCount.setSteps(nowBuSu);
        //获取传感器类型 获得加速度传感器
        Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //此方法用来注册，只有注册过才会生效，参数：SensorEventListener的实例，Sensor的实例，更新速率
        boolean isAvailable = sensorManager.registerListener(mStepCount.getStepDetector(), sensor, SensorManager.SENSOR_DELAY_UI);
        mStepCount.initListener(new StepValuePassListener() {
            @Override
            public void stepChanged(int steps) {
                nowBuSu = steps;//通过接口回调获得当前步数
                updateNotification();    //更新步数通知
            }
        });
    }

    /**
     * 通知调用者步数更新 数据交互
     */
    private void updateNotification() {
        if (mCallback != null) {
            Log.i("BindService", "数据更新");
            mCallback.updateUi(nowBuSu);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {

        return lcBinder;
    }

    /**
     * 计步传感器数据变化回调接口
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        //这种类型的传感器返回步骤的数量由用户自上次重新启动时激活。返回的值是作为浮动(小数部分设置为0),
        // 只在系统重启复位为0。事件的时间戳将该事件的第一步的时候。这个传感器是在硬件中实现,预计低功率。
        if (stepSensorType == Sensor.TYPE_STEP_COUNTER) {
            //获取当前传感器返回的临时步数
            int tempStep = (int) event.values[0];
            //首次如果没有获取手机系统中已有的步数则获取一次系统中APP还未开始记步的步数
            if (!hasRecord) {
                hasRecord = true;
                hasStepCount = tempStep;
            } else {
                //获取APP打开到现在的总步数=本次系统回调的总步数-APP打开之前已有的步数
                int thisStepCount = tempStep - hasStepCount;
                //本次有效步数=（APP打开后所记录的总步数-上一次APP打开后所记录的总步数）
                int thisStep = thisStepCount - previousStepCount;
                //总步数=现有的步数+本次有效步数
                nowBuSu += (thisStep);
                //记录最后一次APP打开到现在的总步数
                previousStepCount = thisStepCount;
            }
        }
        //这种类型的传感器触发一个事件每次采取的步骤是用户。只允许返回值是1.0,为每个步骤生成一个事件。
        // 像任何其他事件,时间戳表明当事件发生(这一步),这对应于脚撞到地面时,生成一个高加速度的变化。
        else if (stepSensorType == Sensor.TYPE_STEP_DETECTOR) {
            if (event.values[0] == 1.0) {
                nowBuSu++;
            }
        }
        updateNotification();

    }

    /**
     * 计步传感器精度变化回调接口
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * 绑定回调接口
     */
    public class LcBinder extends Binder {
        BindService getService() {
            return BindService.this;
        }
    }

    /**
     * 数据传递接口
     *
     * @param paramICallback
     */
    public void registerCallback(UpdateUiCallBack paramICallback) {
        this.mCallback = paramICallback;
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //返回START_STICKY ：在运行onStartCommand后service进程被kill后，那将保留在开始状态，但是不保留那些传入的intent。
        // 不久后service就会再次尝试重新创建，因为保留在开始状态，在创建     service后将保证调用onstartCommand。
        // 如果没有传递任何开始命令给service，那将获取到null的intent。
        return START_STICKY;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        //取消前台进程
        stopForeground(true);

    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
}
