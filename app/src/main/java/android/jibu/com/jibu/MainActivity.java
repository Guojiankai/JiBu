package android.jibu.com.jibu;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private BindService bindService;
    private TextView textView;
    private boolean isBind;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                textView.setText(msg.arg1 + "");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.busu);
        Intent intent = new Intent(MainActivity.this, BindService.class);
        isBind =  bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    //和绷定服务数据交换的桥梁，可以通过IBinder service获取服务的实例来调用服务的方法或者数据
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BindService.LcBinder lcBinder = (BindService.LcBinder) service;
            bindService = lcBinder.getService();
            bindService.registerCallback(new UpdateUiCallBack() {
                @Override
                public void updateUi(int stepCount) {
                    //当前接收到stepCount数据，就是最新的步数
                    Message message = Message.obtain();
                    message.what = 1;
                    message.arg1 = stepCount;
                    handler.sendMessage(message);
                    Log.i("MainActivity—updateUi","当前步数"+stepCount);
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    public void onDestroy() {  //app被关闭之前，service先解除绑定
        super.onDestroy();
        if (isBind) {
            this.unbindService(serviceConnection);
        }
    }
}
