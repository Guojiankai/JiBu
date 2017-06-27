package android.jibu.com.jibu;

import android.util.Log;

/**
 * Created by Administrator on 2017/6/22 0022.
 */

public class StepCount implements StepCountListener {
    private int mCount; //当前步数
    private int count;  //缓存步数，步数3秒内小于10步则不计数
    private long timeOfLastPeak = 0;//计时  开始时间 步数3秒内小于10步则不计数
    private long timeOfThisPeak = 0;//计时  现在时间 步数3秒内小于10步则不计数
    private StepValuePassListener stepValuePassListener;//接口用来传递步数变化
    private StepDetector stepDetector;//传感器SensorEventListener子类实例

    public StepCount() {
        stepDetector = new StepDetector();
        stepDetector.initListener(this);
    }

    @Override
    public void countStep() {
        this.timeOfLastPeak = this.timeOfThisPeak;
        this.timeOfThisPeak = System.currentTimeMillis();
        Log.i("countStep","传感器数据刷新回调");
//        notifyListener();
        if (this.timeOfThisPeak - this.timeOfLastPeak <= 3000L) {
            if (this.count < 9) {
                this.count++;
            } else if (this.count == 9) {
                this.count++;
                this.mCount += this.count;
                notifyListener();
            } else {
                this.mCount++;
                notifyListener();
            }
        } else {//超时
            this.count = 1;//为1,不是0
        }

    }
    public void setSteps(int initNowBusu){
        this.mCount = initNowBusu;//接收上层调用传递过来的当前步数
        this.count = 0;
        timeOfLastPeak = 0;
        timeOfThisPeak = 0;
        notifyListener();
    }

    /**
     * 用来给调用者获取SensorEventListener实例
     * @return 返回SensorEventListener实例
     */
    public StepDetector getStepDetector(){
        return stepDetector;
    }
    /**
     * 更新步数，通过接口函数通过上层调用者
     */
    public void notifyListener(){
        if(this.stepValuePassListener != null){
            Log.i("countStep","数据更新");
            this.stepValuePassListener.stepChanged(this.mCount);  //当前步数通过接口传递给调用者
        }
    }
    public  void initListener(StepValuePassListener listener){
        this.stepValuePassListener = listener;
    }
}
